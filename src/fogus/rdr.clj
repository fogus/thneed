(ns fogus.rdr
  (:require [clojure.reflect :as ref]
            [clojure.string :as string])
  (:import java.io.PushbackReader
           clojure.lang.LispReader))

;; TODO: varargs as?
;; TODO: primitive arrays
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
  "Passthrough to the read handler for the qualified method handler. Calls through
  the Var for development purposes."
  [rdr dot opts pending]
    (let [form (LispReader/read rdr true nil false opts)]
    (when (not (symbol? form))
      (throw (RuntimeException. "expecting a symbol for reader form #.")))
    (let [[klass-sym method-sym] (split-symbol form)]
      (when (not (and klass-sym method-sym))
        (throw (RuntimeException. (str "expecting a qualified symbol for reader form #. got #." form " instead"))))
      `(make-fn ~klass-sym ~method-sym))))

;; REFLECTION STUFF

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

(defn- build-dispatch-tree
  [sigs args]
  (let [tuples (map #(partition 2 (interleave %1 %2)) sigs (cycle [args]))]
    (apply (fn m [& all] (apply merge-with m all))
           (map (fn ps [sig]
                  (if (seq sig)
                    (array-map (first sig) (ps (rest sig)))
                    (array-map)))
                tuples))))

(def coercions
  '{int int
    float float
    long long
    double double})

(defn- build-resolutions [types args]
  (map (fn [t a]
         (let [coercer (get coercions t)]
           (if coercer
             (list coercer a)
             (with-meta a {:tag t}))))
       types
       args))

(defn build-callsite [types args target class-sym method-sym]
  (cond (ctor? class-sym method-sym) `(new ~class-sym ~@(build-resolutions types args))
        target `(. ~(with-meta target {:tag class-sym}) ~method-sym ~@(build-resolutions types args))
        :default `(. ~class-sym ~method-sym ~@(build-resolutions types args))))

(def ttable '{long Long
              int  Integer
              float Float
              double Double})

(defn- build-conditional-dispatch
  [tree type-stack args target class-sym method-sym]
  (when tree
    `(cond
       ~@(mapcat (fn [[param subtree]]
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
       :default ~(list `throw (list 'IllegalArgumentException.
                                    (list 'str (str "invalid argument type to " class-sym "/" method-sym ": ") (second (ffirst tree))))))))

(defn- build-simple-dispatch [args target class-sym method-sym]
  (cond (ctor? class-sym method-sym) `(new ~class-sym ~@args)
        target `(. ~(with-meta target {:tag class-sym}) ~method-sym ~@args)
        :default `(. ~class-sym ~method-sym ~@args)))

(defn- build-body [arity sigs static? class-sym method-sym]
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

(defn- overloads
  "Returns a seq of the overides for a given method in clojure.reflect/reflect structs."
  [details method-sym]
  (->> details :members (filter (comp #{method-sym} :name))))

(defn- build-method-descriptor
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

(defmacro make-fn [class-sym method-sym]
  (let [descr (build-method-descriptor class-sym method-sym)]
    `(let []
       ~(build-method-fn descr))))

(comment
  (attach-qmethod-reader! qmethod-rdr)

  ;;#.String/toUpperCase
  
  (def _abs (make-fn java.lang.Math abs))
  (_abs -1)
  
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
  (build-method-fn (build-method-descriptor 'java.util.Date 'java.util.Date)) ;; TODO

  (new java.util.Date 75 12 2)
  
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
