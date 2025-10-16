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

(defn upto
  [end start]
  (iota identity inc #(< % end) start))

(defn downto
  [end start]
  (iota identity dec #(> % end) start))

(defn to [start end]
  (if (<= start end)
    (upto end start)
    (downto end start)))

(defn !pred
  "A higher-order function that creates a validated wrapper around another function.
  Given a predicate p, a function f, and an optional exception handler ex-dispatch,
  a closure is returned that executes f with the provided arguments, the result is
  checked against p. If p returns true then the closure throws according to the
  exception dispatch. If ex-dispatch is a string or a map then that data is used to
  form the relevant contents of an ex-info packet. If ex-dispatch is a fn then
  the arguments given to the closure are passed to it and a Throwable is expected
  as its return.
  
  If the predicate returns false, it returns the result unchanged."
  ([p f]
   (!pred p f nil))
  ([p f ex-dispatch]
   (fn [& args]
     (let [ret (apply f args)]
       (if (p ret)
         (throw
           (cond
             (or (nil? ex-dispatch) (string? ex-dispatch)) (ex-info ex-dispatch {})
             (map? ex-dispatch) (ex-info (:ex-info/msg ex-dispatch) ex-dispatch)
             (ifn? ex-dispatch) (apply ex-dispatch args)))
         ret)))))


