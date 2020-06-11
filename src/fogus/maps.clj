(ns fogus.maps
  (:require fogus.meta))

(defn keys-apply [f ks m]
  "Takes a function, a set of keys, and a map and applies
   the function to the map on the given keys.  A new map of
   the results of the function applied to the keyed entries
   is returned."
  (let [only (select-keys m ks)]
    (zipmap (keys only) (map f (vals only)))))

(defn manip-map [f ks m]
  "Takes a function, a set of keys, and a map and applies the function
   to the map on the given keys.  A modified version of the original map
   is returned with the results of the function applied to each keyed entry."
  (conj m (keys-apply f ks m)))

(comment
  (keys-apply inc [:a :c] {:a 1, :b 2, :c 3})
  ;=> {:a 2, :c 4}
  (manip-map inc [:a :c] {:a 1, :b 2, :c 3})
  ;=> {:c 4, :b 2, :a 2}
)

(defn assoc-iff
  ([m k v]
   (if (nil? v)
     (fogus.meta/mupdate m ::missed-keys (fnil conj #{}) k)
     (assoc m k v)))
  ([m k v & kvs]
   (reduce (fn [m [k v]] (assoc-iff m k v))
           (assoc-iff m k v)
           (partition 2 kvs))))

(comment
  (-> {}
      (assoc-iff :a 1, :b 2, :c nil)
      meta)

)
