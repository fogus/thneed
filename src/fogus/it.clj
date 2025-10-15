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

