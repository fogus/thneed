(ns fogus.rdr
  (:require [clojure.reflect :as ref]
            [clojure.string :as string])
  (:import java.io.PushbackReader
           clojure.lang.LispReader))

(defn get-field
  "Access to private or protected field.  field-name is a symbol or
  keyword."
  [klass field-name obj]
  (-> klass
      (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))

(defn attach-qmethod-reader!
  "Jack into the LispReader dispatchMacros for the . character."
  [rdr]
  (aset (get-field clojure.lang.LispReader :dispatchMacros nil)
        (int \.)
        rdr))

(set! *warn-on-reflection* true)

(declare qmeth)

(defn- ctor?
  "Does a symbol look like a constructor form?"
  [sym]
  (let [^String nom (and sym (name sym))]
    (= (.indexOf nom (int \.)) (dec (.length nom)))))

(defn- split-symbol
  "Splits a symbol of the form Class/method into two symbols, one for each part in a pair vector.
  For a constructor form the method symbol remains as 'Class.'"
  [sym]
  (let [[klass method] ((juxt namespace name) sym)
        klass (if (ctor? method) (->> method name seq butlast (string/join "") symbol) klass)]
    (let [nom (-> 'String. name )])
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

      `(qmeth ~klass-sym ~method-sym))))

(defn- overloads
  "Returns a seq of the overides for a given method in clojure.reflect/reflect structs."
  [details method-sym]
  (->> details :members (filter (comp #{method-sym} :name))))

(defmacro qmeth
  "Reads a reader form for a qualified method or constructor and emits a structure
  for a function that calls down to the given class member. The following forms
  result in the corresponding functions:

   #.Class/staticMethod => (fn [arg1 arg2] (Class/staticMethod arg1 arg2))

   #.Class/boundMethod  => (fn [this arg1 arg2] (. this boundMethod arg1 arg2))

   #.Class.             => (fn [arg1] (new Class arg1))

  Malformed qualified forms result in an exception at read time.
  
  Some error and warning modes are not yet handled:
  - private methods
  - multiple overloads
  - class not resolved"
  [klass-sym method-sym]
  (let [form    (symbol (name klass-sym) (name method-sym))
        klass   (resolve klass-sym)
        details (ref/reflect klass)
        ovr     (overloads details method-sym) ;; what to do if there are more than one?
        static? (contains? (-> ovr first :flags) :static)
        target  (first ovr)
        params  (map #(do % (gensym)) (:parameter-types target))
        params  (vec (if static? params (cons (gensym) params)))]
    ;; TODO: check for private
    ;; TODO: ctor of more than 1-arg?
    ;; not seeing reflection warnings?
    (cond static?            `(fn ~params (~form ~@params))
          (ctor? method-sym) (let [p [(gensym)]] `(fn ~p (new ~klass ~@p)))
          :default           `(fn ~(vec (cons (with-meta (first params) {:tag klass-sym}) (rest params)))
                                (. ~(first params) ~method-sym ~@(rest params))))))

(comment
  (attach-qmethod-reader! qmethod-rdr)

  (import 'java.util.Collections)
  (import 'java.util.Date)
  (import 'java.sql.Time)
  (import 'java.sql.Timestamp)
  
  (read-string "#.Math/abs")
  (read-string "#.String/toUpperCase")
  (read-string "#.String.")

  (macroexpand-1 '(qmeth Math abs))

  (map #.Math/abs [-1 2])  ;; reflect, type overloads
  (map #.Math/acos [0.2 0.1]) ;; no reflect, no overloads, single arity
  (#.Collections/max [1 2 3 2 5 4 1]) ;; error, chooses 2-arity method
  (map #.String/toUpperCase ["a" "foobar" "HeLLO"]) ;; no reflect, no overloads, single arity
  (map #.String. [(StringBuffer. "ab") "foo"]) ;; reflection, chooses 1-arity but has overloads
  (#.Math/abs -1) ;; reflect, type overloads
  (#.Time. 1) ;; no reflect, 1-arity ctor is default, no type overload
  (#.Date. 1) ;; reflect, 1-arity ctor is default, type overloads

  (defn f [n]
    (#.Date. ^long n))

  (defn f [^long n]
    ((fn [^long x] (Date. x)) n))
  
  (map (fn [n] (Math/abs n)) [-1 2])

  (let [n -1]
    (#.Math/abs n))

  (import '(java.lang.invoke MethodHandles
                             MethodHandles$Lookup
                             MethodType
                             MethodHandle))

  (def ^MethodType meth-type (MethodType/methodType Long/TYPE Long/TYPE))
  
  (def ^MethodHandle abs-handle (.findStatic (MethodHandles/lookup) 
                                             Math 
                                             "abs" 
                                             meth-type))

  (class -42)
  
  (.invokeWithArguments abs-handle (object-array [-42]))
  (.invokeWithArguments abs-handle (object-array [-42.12]))

  (filter (fn [x] (.. x getName (equals "abs")))
          (.getDeclaredMethods Math))

  (.getDeclaredMethods Math)

  (defn f ^long [^long n] (long (Math/abs n)))

  (f 42)

  clojure.lang.IFn$LO
  
  (.invokePrim ^clojure.lang.IFn$LL f 42)
)

(defn- build-method-descriptor
  [klass-sym method-sym]
  (let [form    (symbol (name klass-sym) (name method-sym))
        klass   (resolve klass-sym)
        details (ref/reflect klass)
        ovr     (overloads details method-sym) ;; what to do if there are more than one?
        static? (contains? (-> ovr first :flags) :static)
        overloads (map (fn [m]
                         (let [params (:parameter-types m)]
                           {:ret  (:return-type m)
                            :sig  params}))
                       ovr)
        arities (into (sorted-map) (group-by (comp count :sig) overloads))]
    {:static?    static?
     :class-sym  klass-sym
     :method-sym method-sym
     :klass      klass
     :arities    arities}))

(def ttable '{long Long
              int  Integer
              float Float
              double Double})

(defn- prim? [sym]
  (contains? ttable sym))

(declare build-dispatch)

(defn- methodize [c m]
  (symbol (name c) (name m)))

(defn- build-branch [static? target {:keys [ret sig]} args {:keys [static? class-sym method-sym] :as descr}]
  [(list instance? (get ttable (first sig)) (first args))
   (if (next sig)
     (build-conditional-dispatch target
                                 [{:ret ret :sig (rest sig)}]
                                 (rest args)
                                 descr)
     (if static?
       `(. ~class-sym ~method-sym ~@(map #(list %1 %2) sig args))   ;; TODO: this is not general
       `(. ~(with-meta target {:tag class-sym}) ~method-sym ~@args) ;; TODO: this is not general)
       ))])

(defn- build-conditional-dispatch [target sigs arglist {:keys [static?] :as descr}]
  `(cond ~@(mapcat (fn [sig]
                     (build-branch static?
                                   target
                                   sig
                                   arglist
                                   descr))
                   sigs)))

(defn- build-simple-dispatch [target sigs arglist {:keys [static? class-sym method-sym] :as descr}]
  (if static?
    `(. ~class-sym ~method-sym ~@arglist)
    `(. ~(with-meta target {:tag class-sym}) ~method-sym ~@arglist)))

(defn- build-body [arity sigs {:keys [static? class-sym] :as descr}]
  (let [arglist  (repeatedly arity gensym)
        target   (when (not static?) (with-meta (gensym "self") {:tag class-sym}))
        callsite nil]
    `(~(vec (if static? arglist (cons target arglist)))
      ~(if (< 1 (count sigs))
         (build-conditional-dispatch target sigs arglist descr)
         (build-simple-dispatch target sigs arglist descr)))))

(defn- build-method-fn
  [{:keys [static? class-sym method-sym klass arities] :as descr}]
  `(fn ~(gensym (name method-sym))
     ~@(map (fn [[arity sigs]]
              (build-body arity sigs descr))
            arities)))

(defmacro make-fn [class-sym method-sym]
  (let [descr (build-method-descriptor class-sym method-sym)]
    `(let []
       ~(build-method-fn descr))))

(comment
  (build-method-fn (build-method-descriptor 'Math 'nextAfter))

  (map (make-fn String toUpperCase) ["a" "bc"])
  
  (map (make-fn Math abs) [-1 -1.2 (int -3) (float -4.1)])

  (map (make-fn Collections max) [[1 2 3] [4 5 6] [7 8 9]])
  ((make-fn Collections max) [1 2 3] >)

  ((make-fn Math nextAfter) 0.1 0.1)
  
  (build-method-descriptor 'Math 'abs)
  (build-method-descriptor 'Collections 'max)
  (build-method-descriptor 'String 'toUpperCase)
  (build-method-descriptor 'Math 'nextAfter)
  (build-method-descriptor 'String 'format)
  (build-method-descriptor 'Timestamp 'compareTo)


  (build-arglist true  '[float])
  (build-arglist false '[java.util.Locale])

  Math/abs ()
  
  (build-bodies   (build-method-descriptor 'Math 'abs))
  (build-bodies   (build-method-descriptor 'Collections 'max))
  (build-bodies   (build-method-descriptor 'String 'toUpperCase))

  ((fn [ts o]
     (cond 
       (instance? Date o) (.compareTo ^Timestamp ts ^Date o)
       (instance? Timestamp o) (.compareTo ^Timestamp ts ^Timestamp o)
       :default (throw (IllegalArgumentException. (str "invalid argument type to Timestamp/compareTo: " (type o))))))
   (Timestamp. 42227)
   (Timestamp. 42226))

  (map (fn [n]
         (cond
           (instance? Long n)    (Math/abs (long n))
           (instance? Double n)  (Math/abs (double n))    
           (instance? Integer n) (Math/abs (int n))
           (instance? Float n)   (Math/abs (float n))
           :default (throw (IllegalArgumentException. (str "invalid argument type to Math/abs: " (type n))))))
       [-1 -2.1 (int -4)])

    (map (fn [n]
         (cond
           (instance? Long n)    (Math/abs (long n))
           (instance? Double n)  (Math/abs (double n))    
           (instance? Integer n) (Math/abs (int n))
           (instance? Float n)   (Math/abs (float n))
           :default (throw (IllegalArgumentException. (str "invalid argument type to Math/abs: " (type n))))))
       [-1 -2.1 (int -4) "a"])

  ((fn
     ([coll]
      (Collections/max coll))
     ([coll cmp]
      (Collections/max coll cmp)))
   [1 2 3 2 5 4 7 3 1 2 -3])

  ((fn
     ([coll]
      (Collections/max coll))
     ([coll cmp]
      (Collections/max coll cmp)))
     [1 2 3 2 5 4 7 3 1 2 -3]
   >)

  ((fn
     ([self] (. ^String self toUpperCase))
     ([self loc] (. ^String self toUpperCase loc)))
   "abc")

  ((fn
     ([self] (. ^String self toUpperCase))
     ([self loc] (. ^String self toUpperCase loc)))
   "abc"
   java.util.Locale/US)

  ((fn [x y]
     (cond 
       (instance? Double x) (cond
                              (instance? Double y) (Math/nextAfter (double x) (double y))
                              :default (throw (IllegalArgumentException. (str "invalid argument type to Math/nextAfter: " (type y)))))
       (instance? Float x) (cond
                             (instance? Double y) (Math/nextAfter (float x) (double y))
                             :default (throw (IllegalArgumentException. (str "invalid argument type to Math/nextAfter: " (type y)))))
       :default (throw (IllegalArgumentException. (str "invalid argument type to Math/nextAfter: " (type x))))))
   (float 1.2) 0.1)

    ((fn [x y]
     (cond 
       (instance? Double x) (cond
                              (instance? Double y) (Math/nextAfter (double x) (double y))
                              :default (throw (IllegalArgumentException. (str "invalid argument type to Math/nextAfter: " (type y)))))
       (instance? Float x) (cond
                             (instance? Double y) (Math/nextAfter (float x) (double y))
                             :default (throw (IllegalArgumentException. (str "invalid argument type to Math/nextAfter: " (type y)))))
       :default (throw (IllegalArgumentException. (str "invalid argument type to Math/nextAfter: " (type x))))))
     1 0.1)

    ;;(Math/nextAfter 1.2 1)

    ((fn 
       ([s more] (String/format s (to-array more)))
       ([l s more] (String/format l s (to-array more))))
     "%d -- %d" [1 2])

    ((fn 
       ([s more] (String/format s (to-array more)))
       ([l s more] (String/format l s (to-array more))))
     "%d -- %d" (to-array [1 2]))
)

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

(defn build-callsite [target class-sym method-sym types args]
  (if target
    `(. ~(with-meta target {:tag class-sym}) ~method-sym ~@(build-resolutions types args))
    `(. ~class-sym ~method-sym ~@(build-resolutions types args))))

#_(defn- build-conditional-dispatch [target class-sym method-sym sigs args]
  (let [calls (map #(build-callsite target class-sym method-sym % args) sigs)]
    (loop [disp               '[cond]
           [[params] & sigs]  sigs
           [[arg & args]]     args]
      (if (seq params) ;; build branches
        (let [branch (conj disp [:test (first params) (first args)])]
          (recur branch (rest params) (rest arglist)))
        (conj disp :TAIL)))))

(comment
  (build-conditional-dispatch nil 'Math 'abs '[[long] [double] [int] [float]] '[n])

  
  (build-callsite 'self 'String 'toUpperCase '[java.util.Locale] '[loc])
  (build-callsite nil 'Math 'abs '[int] '[n])


  (loop [disp '[cond]
                        sigs sigs]
                   (if (next sigs)
                     (loop [types (first sigs)
                            args  args
                            disp  disp]
                       (if (next types)
                         (conj (conj [:test (first arg)] disp)
                               (recur (rest types)
                                      (rest args)
                                      '[cond]))
                         (conj [:call])))
                     
                     (recur ...)))
)


(comment

  (def ttree {:T1 {:T2 {}
                   :T3 {}}
              :T2 {:T3 {}}})

  (def sigs [[:T1 :T2] [:T1 :T3] [:T2 :T3]])
  (def sigs2 [[:T1 :T2] [:T1 :T3] [:T32 :T33]])
  (def sigs3 [[:T11 :T12 :T13] [:T21 :T23 :T24] [:T32 :T33 :T31]])
  (def sigs4 '[[a1 a2 a3] [b1 b2 b3] [c1 c2 b3]])

  (defn- build-sigs-tree [sigs]
    )

  (build-sigs-tree sigs)

  (build-dispatch-tree '[[int] [float] [double] [long]] '[n])
  (build-dispatch-tree '[[float double] [double double] [float int]] '[x y])
  (build-dispatch-tree '[[float double double] [double double double] [float int double]]  '[x y z])

  (def dt1 '{(int n) {}, (float n) {}, (double n) {}, (long n) {}})
  (def dt2 '{(float x)  {(double y) {}, (int y) {}},
             (double x) {(double y) {}}})

  (defn build-dispatch-table [tree]
    (when tree
      `(cond
         ~@(mapcat (fn [[param subtree]]
                     (let [[t p] param]
                       [(list t p)
                        (if (seq subtree)
                          (build-dispatch-table subtree)
                          (list '. p '...))]))
                   (seq tree))
         :default :THROWS)))

  (build-dispatch-table
   (build-dispatch-tree '[[int] [float] [double] [long]] '[n]))

  (build-dispatch-table
   (build-dispatch-tree '[[float double] [double double] [float int]] '[x y]))
  
  (build-dispatch-table
   (build-dispatch-tree '[[float double double] [double double double] [float int double]]  '[x y z]))

  (build-dispatch-table
   (build-dispatch-tree '[[java.util.Collection java.util.Comparator]]  '[coll com]))

  (build-dispatch-table
   (build-dispatch-tree '[]  '[]))
)
