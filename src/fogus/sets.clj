;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.sets
  "Utilities dealing with sets."
  (:require [clojure.set :as sets]))

(defn minimize-sets
  "Takes a seq of sets and returns a seq of the mutually different sets. That is, the returned seq
   will contain sets that have no similar items between them."
  [sets]
  (let [commons (apply sets/intersection sets)]
    (reduce (fn [acc tgt]
              (let [prev (last acc)
                    curr (sets/union prev commons)]
                (conj acc (sets/difference tgt curr))))
            [(sets/difference (first sets) commons)]
            (rest sets))))

