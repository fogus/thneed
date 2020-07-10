(ns fogus.maps
  (:require fogus.meta))

(defn keys-apply [m ks f]
  "Takes a function, a set of keys, and a map and applies
   the function to the map on the given keys.  A new map of
   the results of the function applied to the keyed entries
   is returned."
  (let [only (select-keys m ks)]
    (zipmap (keys only) (map f (vals only)))))

(defn manip-map [m ks f]
  "Takes a function, a set of keys, and a map and applies the function
   to the map on the given keys.  A modified version of the original map
   is returned with the results of the function applied to each keyed entry."
  (conj m (keys-apply m ks f)))

(defn manip-keys
  [m ks f]
  (reduce (fn [acc key]
            (if-let [v (get m key)]
              (-> acc
                  (dissoc key)
                  (assoc  (f key) v))
              m))
          m
          ks))

(comment
  (keys-apply {:a 1, :b 2, :c 3} [:a :c] inc)
  ;;=> {:a 2, :c 4}
  (manip-map {:a 1, :b 2, :c 3} [:a :c] inc)
  ;;=> {:c 4, :b 2, :a 2}

  (manip-keys {:a 1, :b 2} [:a] str)
  ;;=> {:b 2, ":a" 1}
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

(defn deep-merge [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

(comment
  (-> {}
      (assoc-iff :a 1, :b 2, :c nil)
      meta)

  (deep-merge {:a 1} {:b 2} {:b 3 :c {:d 1}} {:c {:d 2}})
)
