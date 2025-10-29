;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

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

(defn assoc-iff
  "Like assoc, but only associates key-value pairs when the value is non-nil."
  ([m k v]
   (if (nil? v)
     (fogus.meta/mupdate m ::missed-keys (fnil conj #{}) k)
     (assoc m k v)))
  ([m k v & kvs]
   (reduce (fn [m [k v]] (assoc-iff m k v))
           (assoc-iff m k v)
           (partition 2 kvs))))

(defn deep-merge
  "Recursively merges nested maps. When merging values at the same key:
  
  - If both values are maps, recursively merges them
  - Otherwise, takes the rightmost value (consistent with merge)"
  [& vals]
  (if (every? map? vals)
    (apply merge-with deep-merge vals)
    (last vals)))

