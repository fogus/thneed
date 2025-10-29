;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.it
  "Utilities and functions pertaining to Information Theory.")

(defn entropy
  "Calculate the information entropy (Shannon entropy) of a
  given input string."
  [s]
  (let [len (count s)]
    (->> (frequencies s)
         (map (fn [[_ v]]
                (let [rf (/ v len)]
                  (-> (Math/log rf)
                      (/ (Math/log 2.0))
                      (* rf)
                      Math/abs))))
         (reduce +))))

