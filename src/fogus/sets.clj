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

