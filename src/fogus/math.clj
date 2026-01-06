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

(defn log-distance
  "Returns the base-10 logarithmic distance between two positive numbers.
   The result is independent of argument order."
  [a b]
  {:pre [(pos? a) (pos? b)]}
  (let [hi (max a b)
        lo (min a b)]
    (clojure.math/log10 (/ hi lo))))
