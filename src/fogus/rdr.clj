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

(defn attach-qmethod-reader! [rdr]
  (aset (get-field clojure.lang.LispReader :dispatchMacros nil)
        (int \.)
        rdr))

(set! *warn-on-reflection* true)

(declare qmeth)

(defn qmethod-rdr [rdr dot opts pending]
  (#'qmeth rdr dot opts pending))

(defn- ctor? [sym]
  (let [^String nom (and sym (name sym))]
    (= (.indexOf nom (int \.)) (dec (.length nom)))))

(defn- split-symbol [sym]
  (let [[klass method] ((juxt namespace name) sym)
        klass (if (ctor? method) (->> method name seq butlast (string/join "") symbol) klass)]
    (let [nom (-> 'String. name )])
    [(when klass  (symbol klass))
     (when method (symbol method))]))

(defn- classify-symbol [sym])

(defn- overloads [details method-sym]
  (->> details :members (filter (comp #{method-sym} :name))))

(defn qmeth [^PushbackReader rdr dot opts pending]
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
        ;; TODO: what if private?
        ;; TODO: ctor of more than 1-arg?
        (cond static?            `(fn ~params (~form ~@params))
              (ctor? method-sym) (let [p [(gensym)]] `(fn ~p (new ~klass ~@p)))
              :default           `(fn ~params (. ~(first params) ~method-sym ~@(rest params))))))))

(comment
  (split-symbol 'String/toUpperCase)
  (split-symbol 'String.)

  (let [sym 'String
        klass (resolve sym)
        details (ref/reflect klass)]
    (->> details
         :members
         (map :name)))
  
  (attach-qmethod-reader! qmethod-rdr)
  Math

  (read-string "#.Math/abs")
  (read-string "#.String/toUpperCase")
  (read-string "#.String.")

  (map #.Math/abs [-1 2])
  (map #.String/toUpperCase ["a" "foobar" "HELLO"])
  (map #.String. [(StringBuffer. "ab") "foo"])

)
