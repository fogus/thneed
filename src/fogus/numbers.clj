;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.numbers
  "Utilities dealing with numbers."
  (:require [clojure.set :as s]))

(def ^:private numerals {\I 1, \V 5, \X 10, \L 50, \C 100, \D 500, \M 1000
                         "IV" 4, "IX" 9, "XL" 40, "XC" 90, "CD" 400, "CM" 900})

(defn parse-roman
  "Converts a Roman numeral string to its numeric value between 1 and 3999, inclusive."
  [s]
  {:post [(and (>= % 1) (<= % 3999))]}
  (if (empty? s)
    0
    (let [vals (map numerals s)]
      (loop [current-vals vals
             total 0]
        (if (empty? current-vals)
          total
          (let [val (first current-vals)
                next-val (second current-vals)]
            (if (and next-val (< val next-val))
              (recur (rest current-vals) (- total val))
              (recur (rest current-vals) (+ total val)))))))))

(defn num->roman
  "Converts a positive number between 1 and 3999, inclusive to a Roman numeral string."
  [n]
  {:pre [(and (>= n 1) (<= n 3999))]}
  (second
   (reduce-kv
    (fn [[remainder result] arabic roman]
      (let [num-symbols (quot remainder arabic)]
        [(rem remainder arabic)
         (str result (apply str (repeat num-symbols roman)))]))
    [n ""]
    (->> (update-keys numerals str) s/map-invert seq flatten (apply sorted-map-by >)))))

(defn approx=
  "Return true if the absolute value of the difference between x and y
   is less than eps."
  [eps x y]
  (< (abs (- x y)) eps))
