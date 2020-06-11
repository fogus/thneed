(ns fogus.rel
  "Additional functions for relational algebra.")

(defn tableify
  "Takes a vector of vectors representing data where the first
  vector contains the column names and the remaining columns contain
  the data aligned to fit into the given columns.  The data that
  comes out should be useable by clojure.set functions."
  [vecs]
  (let [[headers & data] vecs
        headers (vec (map keyword headers))
        data (map #(zipmap headers %) data)]
    (with-meta (set data)
      {::column-names headers})))

(defn typeify
  "Takes a table as provided by `tableify` and a map of keys->convertor and
  converts each row's values to the right type as defined by the convertor."
  [conversions table]
  (let [nothing (:conversion/not-set (meta conversions) ::nothing)
        safe (fn [f]
               (fn [val]
                 (if (= val nothing)
                   ::nothing
                   (f val))))]
    (set
     (for [row table]
       (reduce (fn [r k]
                 (let [cnv (get conversions k identity)]
                   (update-in r [k] (safe cnv))))
               row
               (keys row))))))

(comment

  (typeify (with-meta {:a first} {:conversion/not-set ""})
           #{{:a [1 2] :b 42 :c ""}})

)