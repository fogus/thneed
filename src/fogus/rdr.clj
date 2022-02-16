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

(defn qmethod-rdr
  "Passthrough to the read handler for the qualified method handler. Calls through
  the Var for development purposes."
  [rdr dot opts pending]
  (#'qmeth rdr dot opts pending))

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

(defn- overloads
  "Returns a seq of the overides for a given method in clojure.reflect/reflect structs."
  [details method-sym]
  (->> details :members (filter (comp #{method-sym} :name))))

(defn qmeth
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
  [^PushbackReader rdr dot opts pending]
  (let [form (LispReader/read rdr true nil false opts)]
    (when (not (symbol? form))
      (throw (RuntimeException. "expecting a symbol for reader form #.")))
    (let [[klass-sym method-sym] (split-symbol form)]
      (when (not (and klass-sym method-sym))
        (throw (RuntimeException. (str "expecting a qualified symbol for reader form #. got #." form " instead"))))

      (let [klass   (resolve klass-sym)
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
              :default           `(fn ~params (. ~(first params) ~method-sym ~@(rest params))))))))

(comment
  (attach-qmethod-reader! qmethod-rdr)

  (read-string "#.Math/abs")
  (read-string "#.String/toUpperCase")
  (read-string "#.String.")

  (map #.Math/abs [-1 2])
  (map #.String/toUpperCase ["a" "foobar" "HELLO"])
  (map #.String. [(StringBuffer. "ab") "foo"])
  (#.Math/abs -1) 
)
