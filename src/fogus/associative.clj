(ns fogus.associative)

(defn dissoc-in
  "Dissociates a value in a nested associative structure asc, where path is a
  sequence of keys. If the path does not resolve to a valid associative mapping
  then this function is a noop."
  [asc path]
  (cond
    (zero? (count path)) asc
    (= 1 (count path)) (dissoc asc (first path))
    :else
    (let [[k & ks] path]
      (if (contains? asc k)
        (update asc k #(dissoc-in % ks))
        asc))))


