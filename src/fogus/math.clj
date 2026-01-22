;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.math
  "Math utilities"
  (:require clojure.math))

(defn order-distance
  "Returns the base-10 logarithmic (order of magnititude) distance between
  two positive numbers. The result is independent of argument order."
  [a b]
  {:pre [(pos? a) (pos? b)]}
  (let [hi (max a b)
        lo (min a b)]
    (clojure.math/log10 (/ hi lo))))

(defn order-apart?
  "Returns true if a and b differ by _at least_ n orders of magnitude.
   With two arguments, defaults to 1.0 order of magnitude."
  ([a b]
   (order-apart? 1.0 a b))
  ([n a b]
   {:pre [(pos? a) (pos? b) (>= n 0)]}
   (>= (order-distance a b) n)))

(defn within-order?
  "Returns true if a and b are _within_ the same order of magnitude."
  [a b]
  {:pre [(pos? a) (pos? b)]}
  (< (order-distance a b) 1))
