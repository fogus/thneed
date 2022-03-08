(ns fogus.rdr
  (:require [clojure.reflect :as ref]
            [clojure.string :as string])
  (:import java.io.PushbackReader
           clojure.lang.LispReader))

;; TODO: varargs as?
;; TODO: primitive arrays
;; TODO: hint return of constructor functions?
;; TODO: error when class || method not resolved
;; TODO: cache

(set! *warn-on-reflection* true)

;; READER STUFF

(declare make-fn)

(defn- ctor?
  "Does a symbol look like a constructor form?"
  ([sym]
   (let [^String nom (and sym (name sym))]
     (= (.indexOf nom (int \.)) (dec (.length nom)))))
  ([l r] (= l r)))

(defn- split-symbol
  "Splits a symbol of the form Class/method into two symbols, one for each part in a pair vector.
  For a constructor form the method symbol remains as 'Class.'"
  [sym]
  (let [[klass method] ((juxt namespace name) sym)
        klass (if (ctor? method) (->> method name seq butlast (string/join "") symbol) (symbol klass))
        ^java.lang.Class resolved-class  (-> klass resolve)
        klass (-> resolved-class .getName symbol)
        method (if (ctor? method) klass method)]
    [(when klass  (symbol klass))
     (when method (symbol method))]))

(defn qmethod-rdr
  "Pass-through to the read handler for the qualified method handler. Calls through
  the Var for development purposes."
  [rdr dot opts pending]
    (let [form (LispReader/read rdr true nil false opts)]
    (when (not (symbol? form))
      (throw (RuntimeException. "expecting a symbol for reader form #.")))
    (let [[klass-sym method-sym] (split-symbol form)]
      (when (not (and klass-sym method-sym))
        (throw (RuntimeException. (str "expecting a qualified symbol for reader form #. got #." form " instead"))))
      `(make-fn ~klass-sym ~method-sym))))

(defn get-field
  "Access to private or protected field.  field-name is a symbol or
  keyword."
  [klass field-name obj]
  (-> ^Class klass
      (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))

(import 'clojure.lang.IFn)

(defn attach-qmethod-reader!
  "Jack into the LispReader dispatchMacros for the . character."
  [^clojure.lang.IFn  rdr]
  (let [^"[Lclojure.lang.IFn;" slot (get-field clojure.lang.LispReader :dispatchMacros nil)]
    (aset slot
          (int \.)
          rdr)))

;; TREE BUILDING

(def ranks '{long 1
             double 2
             int 3
             float 4})

(defn- tcompare
  "Comaparator for the type/arg tuple comprising the dispatch tree. Compares the
  priority ranking as found in ranks or defaults to < if not found to avoid clashes
  on types not found in ranks."
  [[t1 _] [t2 _]]
  (compare (get ranks t1 0)
           (get ranks t2 1)))

(defn- build-dispatch-tree
  "Given a set of type signatures and an arglist, builds a tree representing the
  type+arg matching for a set of signatures for a single arity. The tree is built
  from nested maps sorted by the type priority rankings. Each branch represents a
  single siganture and layers correspond to the same positional argument in all
  of the signatures."
  [sigs args]
  (let [tuples (map #(partition 2 (interleave %1 %2)) sigs (cycle [args]))]
    (apply (fn m [& all] (apply merge-with m all))
           (map (fn ps [sig]
                  (if (seq sig)
                    (sorted-map-by tcompare (first sig) (ps (rest sig)))
                    (sorted-map-by tcompare)))
                tuples))))

;; CALLSITE BUILDING

(def coercions
  '{int int
    float float
    long long
    double double})

(defn- build-resolutions
  "Given a seq of types and their corresponding arguments, builds a seq of coercion
  forms for each argument. Attempts to look up a coercion function name in coercions
  and builds a form for a call to that function with the corresponding arg, else
  attached :tag metadata to that argument for the type."
  [types args]
  (map (fn [t a]
         (let [coercer (get coercions t)]
           (if coercer
             (list coercer a)
             (with-meta a {:tag t}))))
       types
       args))

(defn build-callsite
  "Given the information required to build a call form, returns one of: a static
  method call, an instance method call with target object name and hinting, or
  a call to new if a constructor is signified. All arguments to the call form will
  be coerced to the expected types."
  [types args target class-sym method-sym]
  (cond (ctor? class-sym method-sym) `(new ~class-sym ~@(build-resolutions types args))
        target `(. ~(with-meta target {:tag class-sym}) ~method-sym ~@(build-resolutions types args))
        :default `(. ~class-sym ~method-sym ~@(build-resolutions types args))))

(def ttable '{long Long
              int  Integer
              float Float
              double Double})

;; DISPATCH TABLE BUILDING

(defn- build-conditional-dispatch
  "Given a dispatch tree for a single arity, a stack of types encountered so far, the
  arg list, and the data needed to build a call form, returns a dispatch table that checks
  each possible argument type and the overloads of each that bottoms out into a call
  that coerces its arguments to the expected types. The dispatch table is implemented as
  nested conds that branch on the disparate types for each argument."
  [tree type-stack args target class-sym method-sym]
  (when tree
    `(cond
       ~@(let [branches (map (fn [[param subtree]]
                               (let [[t p] param
                                     ts (conj type-stack t)]
                                 [(list `instance? (get ttable t t) p)
                                  (if (seq subtree)
                                    (build-conditional-dispatch subtree
                                                                ts
                                                                args
                                                                target
                                                                class-sym
                                                                method-sym)
                                    (build-callsite ts args target class-sym method-sym))]))
                             (seq tree))
               ;; patch up the final branch to be the default case (i.e. last coercion)
               default-branch (->> branches last second (vector :default))]
           (apply concat (conj (vec (butlast branches)) default-branch))))))

;; FUNCTION BUILDING

(defn- build-simple-dispatch
  "Given the information required to build a call form, returns one of: a static
  method call, an instance method call with target object name and hinting, or
  a call to new if a constructor is signified. None of the arguments are coerced."
  [args target class-sym method-sym]
  (cond (ctor? class-sym method-sym) `(new ~class-sym ~@args)
        target `(. ~(with-meta target {:tag class-sym}) ~method-sym ~@args)
        :default `(. ~class-sym ~method-sym ~@args)))

(defn- build-body
  "Takes an arity count, a seq of signatures for it, a flag if the underlying method
  is static, and the class and function names and uilds a function body for that arity.
  If there are no overloads for this arity then the body has no conditional branching on
  types and none of the arguments to the call are coerced. In the case of type overloading
  a conditional dispatch table is built and arguments to calls are coerced as needed."
  [arity sigs static? class-sym method-sym]
  (let [arglist  (repeatedly arity gensym)
        target   (when (not static?) (with-meta (gensym "self") {:tag class-sym}))
        dispatch-tree (build-dispatch-tree sigs arglist)]
    `(~(vec (if static? arglist (cons target arglist)))
      ~(if (< 1 (count sigs))
         (build-conditional-dispatch dispatch-tree [] arglist target class-sym method-sym)
         (build-simple-dispatch arglist target class-sym method-sym)))))

(defn- build-method-fn
  [{:keys [static? class-sym method-sym arities] :as descr}]
  `(fn ~(gensym (name method-sym))
     ~@(map (fn [[arity sigs]]
              (build-body arity sigs static? class-sym method-sym))
            arities)))

;; MACRO SUPPORT

(defn- overloads
  "Returns a seq of the overides for a given method in clojure.reflect/reflect structs."
  [details method-sym]
  (->> details :members (filter (comp #{method-sym} :name))))

(defn- build-method-descriptor
  "Takes a class and method symbol and builds a descriptor for the method containing:

  :static? - a flag indicating if the method is static
  :klass - the resolved class
  :class-sym - the symbolic class name
  :method-sym - the symbolic method name
  :arities - a map of arity-count => signatures (parameter types and return type)"
  [klass-sym method-sym]
  (let [form    (symbol (name klass-sym) (name method-sym))
        klass   (resolve klass-sym)
        details (ref/reflect klass)
        ovr     (overloads details method-sym) ;; what to do if there are more than one?
        static? (or (contains? (-> ovr first :flags) :static) (ctor? klass-sym method-sym))
        overloads (map (fn [m]
                         (let [params (:parameter-types m)]
                           (with-meta
                             params
                             {:ret  (:return-type m)
                              :sig  params})))
                       ovr)
        arities (into (sorted-map) (group-by count overloads))]
    {:static?    static?
     :class-sym  klass-sym
     :method-sym method-sym
     :klass      klass
     :arities    arities}))

(defmacro make-fn
  "Given a class and method name, returns a function that dispatches to a call of 
  the method with its arguments coerced to the expected types. If the method-sym argument
  is the same as the class-sym argument then the function returned dispatches to that
  class's constructor with its arguments coerced. In the case where an unsupported type is
  passed to the generated function, a coersion exception will occur."
  [class-sym method-sym]
  (let [descr (build-method-descriptor class-sym method-sym)]
    `(let []
       ~(build-method-fn descr))))



(comment
  (attach-qmethod-reader! qmethod-rdr)

  ;;#.String/toUpperCase
  
  (def _abs (make-fn java.lang.Math abs))
  (_abs "a")
  
  (map (make-fn java.lang.Math abs) [-1 -1.2 (int -3) (float -4.25)])
  (map _abs [-1 -1.2 (int -3) (float -4.25)])

  (map (make-fn java.lang.String toUpperCase) ["a" "bc"])

  (map (make-fn java.util.Collections max) [[1 2 3] [4 5 6] [7 8 9]])

  ((make-fn java.lang.Math nextAfter) 0.1 1.1)
  ((make-fn java.util.Date java.util.Date) (int 1))

  (import 'java.util.Date)
  
  (build-method-fn (build-method-descriptor 'java.lang.Math 'abs))
  (build-method-fn (build-method-descriptor 'java.lang.Math 'nextAfter))
  (build-method-fn (build-method-descriptor 'java.lang.String 'toUpperCase))
  (build-method-fn (build-method-descriptor 'java.util.Collections 'max))
  (build-method-fn (build-method-descriptor 'java.sql.Timestamp 'compareTo))
  (build-method-fn (build-method-descriptor 'java.lang.String 'format))
  (build-method-fn (build-method-descriptor 'java.util.Date 'java.util.Date))
  ;;(build-method-fn (build-method-descriptor 'java.lang.String 'java.lang.String))

  (build-body 1 '[[int] [float] [double] [long]] true 'Math 'abs)
  
  (build-simple-dispatch '[] 'self 'String 'toUpperCase)
  (build-simple-dispatch '[loc] 'self 'String 'toUpperCase)

  (build-simple-dispatch '[coll] nil 'Collections 'max)
  
  (build-conditional-dispatch
   (build-dispatch-tree '[[int] [float] [double] [long]] '[n])
   []
   '[n]
   nil
   'Math
   'abs)

  (build-conditional-dispatch
   (build-dispatch-tree '[[float double] [double double] [float int]] '[x y])
   []
   '[x y]
   nil
   'Math
   'foo)
  
  (build-conditional-dispatch
   (build-dispatch-tree '[[float double double] [double double double] [float int double]]  '[x y z]))

  (build-conditional-dispatch
   (build-dispatch-tree '[[java.util.Collection java.util.Comparator]]  '[coll com])
   []
   '[coll com]
   nil
   'Collections
   'max)

  (build-conditional-dispatch
   (build-dispatch-tree '[]  '[]))

)
