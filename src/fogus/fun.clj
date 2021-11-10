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

(defn !pred
  {:doc "Returns function that takes args and if (apply f args) is not nil, returns it.
  Otherwise throw exception per ex-dispatch - nil / string / map throw ex-info, or an
  ifn? constructs an arbitrary exceptions (and is passed the function args)."
   :added "1.11"}
  ([p f]
   (!pred p f nil))
  ([p f ex-dispatch]
   (fn [& args]
     (let [ret (apply f args)]
       (if (p ret)
         (throw
           (cond
             (or (nil? ex-dispatch) (string? ex-dispatch)) (ex-info ex-dispatch {})
             (map? ex-dispatch) (ex-info (:ex-info/msg ex-dispatch) ex-dispatch) ;; TODO: get msg from map?
             (ifn? ex-dispatch) (apply ex-dispatch args)))
         ret)))))

(def parse-long #(try (Long/parseLong %) (catch Exception _)))

(def validate-long (!pred nil? parse-long #(ex-info (format "Expected double, got: %s" %) {})))
(validate-long "42")
;(validate-long "one")

(def validate-not-even (!pred even? parse-long #(ex-info (format "Expected odd number, got: %s" %) {})))
(validate-not-even "3")
;(validate-not-even "2")


