(ns fogus.associative)

(defn dissoc-in  [asc path]
  (cond
    (zero? (count path)) asc
    (= 1 (count path)) (dissoc asc (first path))
    :else
    (let [[k & ks] path]
      (if (contains? asc k)
        (update asc k #(dissoc-in % ks))
        asc))))


