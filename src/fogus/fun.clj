(ns fogus.fun
  "A bunch of utilities that you might find in interesting
  functional and/or function-level languages.")

(defn foldr
  "Fold right... as opposed to fold left (i.e. reduce)."
  [f acc [h & t :as coll]]
  (if (seq coll)
    (f h (foldr f acc t))
    acc))

(defn iota [t nxt stop y]
  (take-while stop (iterate #(t (nxt %)) y)))

(comment
  (iota identity inc #(< % 10) 1)
)

(def upto (fn [end start]
            (iota identity inc #(< % end) start)))

(def downto (fn [end start]
              (iota identity dec #(> % end) start)))

(defn to [start end]
  (if (<= start end)
    (upto end start)
    (downto end start)))
