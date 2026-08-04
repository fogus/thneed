;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.xf
  "Transducers")

(defn slide
  "Returns a transducer that partitions the input stream into sliding
  windows each of size n. In case there are not enough elements to pad
  out the last window, then returns a partition with less than n items."
  [n]
  (fn [rf]
    (let [q (volatile! clojure.lang.PersistentQueue/EMPTY)]
      (fn
        ([] (rf))
	([acc]
         (rf (if (and (pos? (count @q)) (< (count @q) n))
               (unreduced (rf acc (vec @q)))
               acc)))
	([acc x]
         (let [b (vswap! q (fn [q]
                             (let [q (conj q x)]
                               (if (> (count q) n)
                                 (pop q)
                                 q))))]
           (if (= n (count b))
             (rf acc (vec b))
             acc)))))))
