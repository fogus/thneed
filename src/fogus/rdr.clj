(ns fogus.rdr
  (:import java.io.PushbackReader
           clojure.lang.LispReader))

(defn get-field
  "Access to private or protected field.  field-name is a symbol or
  keyword."
  [klass field-name obj]
  (-> klass (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))

(defn attach-qmethod-reader! [rdr]
  (aset (get-field clojure.lang.LispReader :dispatchMacros nil)
        (int \.)
        rdr))

(defn qmeth [^PushbackReader rdr dot opts pending]
  (let [form (LispReader/read rdr true nil false opts)]
    (list 'quote form)))

(defn qmethod-rdr [rdr dot opts pending]
  (#'qmeth rdr dot opts pending))

(comment

  (attach-qmethod-reader! qmethod-rdr)

  (read-string "#.Foo/bar")

  #.Foo/Bar
)
