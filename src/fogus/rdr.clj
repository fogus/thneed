(ns fogus.rdr
  (:require [clojure.reflect :as ref]
            [clojure.string :as string])
  (:import java.io.PushbackReader
           clojure.lang.LispReader))

;; TODO: hint return of constructor functions?
;; TODO: lift out singleton cond branches
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

;; TYPES and CHECKS

(defn tpred [t]
  `(fn [x#]
     (if (nil? x#)
       false
       (-> x# class .getComponentType (= ~t)))))

(def types-table
  {'long      {:rank 1  :coercer `long     :checker Long}
   'double    {:rank 2  :coercer `double   :checker Double}
   'int       {:rank 3  :coercer `int      :checker Integer}
   'float     {:rank 4  :coercer `float    :checker Float}
   'char      {:rank 5  :coercer `char     :checker Character}
   'boolean   {:rank 6  :coercer `boolean  :checker Boolean}
   'short     {:rank 7  :coercer `short    :checker Short}
   'byte      {:rank 8  :coercer `byte     :checker Byte}
   'long<>    {:rank 9  :coercer `longs    :checker (tpred 'Long/TYPE)}
   'double<>  {:rank 10 :coercer `doubles  :checker (tpred 'Double/TYPE)}
   'int<>     {:rank 11 :coercer `ints     :checker (tpred 'Integer/TYPE)}
   'float<>   {:rank 12 :coercer `floats   :checker (tpred 'Float/TYPE)}
   'char<>    {:rank 13 :coercer `chars    :checker (tpred 'Character/TYPE)}
   'boolean<> {:rank 14 :coercer `booleans :checker (tpred 'Boolean/TYPE)}
   'short<>   {:rank 15 :coercer `shorts   :checker (tpred 'Short/TYPE)}
   'byte<>    {:rank 16 :coercer `bytes    :checker bytes?}
   'java.lang.Object<> {:coercer `to-array :checker (tpred 'java.lang.Object)}})

;; TREE BUILDING

(defn- hierarchy-comparator [l r]
  (cond (= l r) 0
        (nil? l) (if (nil? r) 0 -1)
        (nil? r) 1
        :default (let [^java.lang.Class lc (resolve l)
                       ^java.lang.Class rc (resolve r)]
                   (cond (.isAssignableFrom lc rc)  1
                         (.isAssignableFrom rc lc) -1
                         :default (compare l r)))))

(defn- tcompare
  "Comaparator for the type/arg tuple comprising the dispatch tree. Compares the
  priority ranking as found in ranks or defaults to < if not found to avoid clashes
  on types not found in ranks."
  [[t1 _] [t2 _]]
  (let [r1 (get-in types-table [t1 :rank])
        r2 (get-in types-table [t2 :rank])]
    (if (and r1 r2)
      (compare r1 r2)
      (if (nil? r1)
        (if (nil? r2)
          (hierarchy-comparator t1 t2)
          1)
        (if (nil? r2)
          -1
          (compare (hash t1) (hash t2)))))))

(defn- build-dispatch-tree
  "Given a set of type signatures and an arglist, builds a tree representing the
  type+arg matching for a set of signatures for a single arity. The tree is built
  from nested maps sorted by the type priority rankings. Each branch represents a
  single siganture and layers correspond to the same positional argument in all
  of the signatures. For the signatures [[double double] [double String] [float double]]

  {(double x) {(double y) {(java.lang.String y) {}}}
   (float x)  {(double y) {}}"
  [sigs args]
  (let [tuples (map #(partition 2 (interleave %1 %2)) sigs (cycle [args]))]
    (apply (fn m [& all] (apply merge-with m all))
           (map (fn ps [sig]
                  (if (seq sig)
                    (sorted-map-by tcompare (first sig) (ps (rest sig)))
                    (sorted-map-by tcompare)))
                tuples))))

;; CALLSITE BUILDING

(defn- build-resolutions
  "Given a seq of types and their corresponding arguments, builds a seq of coercion
  forms for each argument. Attempts to look up a coercion function name in coercions
  and builds a form for a call to that function with the corresponding arg, else
  attached :tag metadata to that argument for the type."
  [types args]
  (map (fn [t a]
         (let [coercer (get-in types-table [t :coercer])]
           (if coercer
             (list coercer a)
             (with-meta a {:tag t}))))
       types
       args))

(defn build-callsite
  "Given the information required to build a call form, returns one of: a static
  method call, an instance method call with target object name and hinting, or
  a call to new if a constructor is signified. All arguments to the call form will
  be coerced to the expected types.

  static call: (. Klass method (coerce arg1) ^T arg2 ...)

  instance call: (. ^Klass self method (coerce arg1) ^T arg2 ...)

  c-tor: (new Klass (coerce arg1) ^T arg2 ...)"
  [types args target class-sym method-sym]
  (cond (ctor? class-sym method-sym) `(new ~class-sym ~@(build-resolutions types args))
        target `(. ~(with-meta target {:tag class-sym}) ~method-sym ~@(build-resolutions types args))
        :default `(. ~class-sym ~method-sym ~@(build-resolutions types args))))

;; DISPATCH TABLE BUILDING

(defn- build-conditional-dispatch
  "Given a dispatch tree for a single arity, a stack of types encountered so far, the
  arg list, and the data needed to build a call form, returns a dispatch table that checks
  each possible argument type and the overloads of each that bottoms out into a call
  that coerces its arguments to the expected types. The dispatch table is implemented as
  nested conds that branch on the disparate types for each argument.

  for a set of signatures [[T U] [T V] [U V]] and args [x y] on a static method Klass/method:

  (cond
    (instance? T x) (cond
                      (instance? U y) (. Klass method ^T x ^U y)
                      :default        (. Klass method ^T x ^V y))
    :default (. Klass method ^U x ^V y))
  "
  [tree type-stack args target class-sym method-sym]
  (when tree
    `(cond
       ~@(let [branches (map (fn [[param subtree]]
                               (let [[t p] param
                                     ts (conj type-stack t)
                                     checker (get-in types-table [t :checker] t)]
                                 [(if (or  (seq? checker) (fn? checker))
                                    (list checker p)
                                    (list `instance? checker p))
                                  (if (seq subtree)
                                    (build-conditional-dispatch subtree
                                                                ts
                                                                args
                                                                target
                                                                class-sym
                                                                method-sym)
                                    (build-callsite ts args target class-sym method-sym))]))
                             (seq tree))
               ;; patch up the final branch to be the last coercion
               default-branch (->> branches last second (vector :default))]
           (apply concat (conj (vec (butlast branches)) default-branch))))))

;; FUNCTION BUILDING

(defn- build-simple-dispatch
  "Given the information required to build a call form, returns one of: a static
  method call, an instance method call with target object name and hinting, or
  a call to new if a constructor is signified. None of the arguments are coerced.

  static call: (. Klass method arg1 arg2 ...)

  instance call: (. ^Klass self method arg1 arg2 ...)

  c-tor: (new Klass arg1 arg2 ...)"
  [args target class-sym method-sym]
  (cond (ctor? class-sym method-sym) `(new ~class-sym ~@args)
        target `(. ~(with-meta target {:tag class-sym}) ~method-sym ~@args)
        :default `(. ~class-sym ~method-sym ~@args)))

(defn- build-body
  "Takes an arity count, a seq of signatures for it, a flag if the underlying method
  is static, and the class and function names and uilds a function body for that arity.
  If there are no overloads for this arity then the body has no conditional branching on
  types and none of the arguments to the call are coerced. In the case of type overloading
  a conditional dispatch table is built and arguments to calls are coerced as needed.

  Given Klass/method of arities 2 and some types overloads on that arity:

  ([x y] (cond ...type dispatch branching for x+y...))"
  [arity sigs static? class-sym method-sym]
  (let [arglist  (repeatedly arity gensym)
        target   (when (not static?) (with-meta (gensym "self") {:tag class-sym}))
        dispatch-tree (build-dispatch-tree sigs arglist)]
    `(~(vec (if static? arglist (cons target arglist)))
      ~(if (< 1 (count sigs))
         (build-conditional-dispatch dispatch-tree [] arglist target class-sym method-sym)
         (build-simple-dispatch arglist target class-sym method-sym)))))

(defn- build-fn-name [{:keys [static? class-sym method-sym]}]
  (let [sep (if static? \/ \#)]
    (munge (gensym (str class-sym sep method-sym "-deligate")))))

(defn- build-method-fn
  "Given Klass/method of arities #{1 2} and some types overloads on those arities:
  
  (fn <name from Klass/method>
    ([x]   (cond ...type dispatch branching for x...))
    ([x y] (cond ...type dispatch branching for x+y...)))"
  [{:keys [static? class-sym method-sym arities] :as descr}]
  `(fn ~(build-fn-name descr)
     ~@(map (fn [[arity sigs]]
              (build-body arity sigs static? class-sym method-sym))
            arities)))

;; MACRO SUPPORT

(defn- overloads
  "Returns a seq of the overides for a given method in clojure.reflect/reflect structs."
  [details method-sym]
  (->> details
       :members
       (filter (comp #{method-sym} :name))
       (filter #(-> % :flags (contains? :public)))
       (remove #(-> % :flags (contains? :bridge)))))

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

(def qmethod-cache (atom {}))

(defmacro make-fn
  "Given a class and method name, returns a function that dispatches to a call of 
  the method with its arguments coerced to the expected types. If the method-sym argument
  is the same as the class-sym argument then the function returned dispatches to that
  class's constructor with its arguments coerced. In the case where an unsupported type is
  passed to the generated function, a coersion exception will occur."
  [class-sym method-sym]
  (let [descr (build-method-descriptor class-sym method-sym)]
    `(let [gfn# ~(or (get qmethod-cache [class-sym method-sym])
                     (build-method-fn descr))
           ]
       gfn#)))



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

  ((make-fn java.lang.String java.lang.String) "foo")
  
  (build-method-fn (build-method-descriptor 'java.lang.Math 'abs))
  (build-method-fn (build-method-descriptor 'java.lang.String 'toUpperCase))
  (build-method-fn (build-method-descriptor 'java.util.Collections 'max))
  (build-method-fn (build-method-descriptor 'java.lang.Math 'nextAfter))
  (build-method-fn (build-method-descriptor 'java.sql.Timestamp 'compareTo))
  (build-method-fn (build-method-descriptor 'java.lang.String 'format))
  (build-method-fn (build-method-descriptor 'java.util.Date 'java.util.Date))
  (build-method-fn (build-method-descriptor 'java.util.ArrayList 'forEach))
  (build-method-fn (build-method-descriptor 'java.lang.String 'java.lang.String))
  (build-method-fn (build-method-descriptor 'java.util.Arrays 'copyOf))
  (build-method-fn (build-method-descriptor 'java.util.Arrays 'fill))
  (spit "foo.clj" (with-out-str (clojure.pprint/pprint   (build-method-fn (build-method-descriptor 'java.util.Arrays 'binarySearch)))))

  (build-method-fn (build-method-descriptor 'java.util.Collections 'sort))
  
  (make-fn java.lang.String java.lang.String)
  ((make-fn java.util.Arrays asList) (long-array [1 2 3]))
  (java.util.Arrays/asList (long-array [1 2 3]))

  ((make-fn java.util.Arrays binarySearch) (long-array [1 2 3]) 1)
  (make-fn java.util.Arrays fill)

  (= java.lang.Object
     (-> (object-array [1 2 3]) class .getComponentType))
  
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

;; Object hint

(comment
  (set! *warn-on-reflection* true)

  (fn compareTo10601 
    ([self arg]
     (clojure.core/cond 
       (clojure.core/instance? java.sql.Timestamp arg) (. ^java.sql.Timestamp self compareTo ^java.sql.Timestamp arg) 
       (clojure.core/instance? java.util.Date arg) (. ^java.sql.Timestamp self compareTo ^java.util.Date arg) 
       :default (. ^java.sql.Timestamp self compareTo ^java.lang.Object arg))))

)

