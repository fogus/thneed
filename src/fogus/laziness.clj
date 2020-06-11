(ns fogus.laziness
  "Utilities dealing with lazy and eager evaluation")

(defn seq1
  "Ensures that chunked sequences are evaluated one element
  at a time."
  [s]
  (lazy-seq
    (when-let [[x] (seq s)]
      (cons x (seq1 (rest s))))))
