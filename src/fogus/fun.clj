(ns fogus.fun
  "A bunch of utilities that you might find in interesting
  functional and/or function-level languages.")

(defn foldr
  "Right-associative fold over a collection. Unlike reduce (which is left-associative
  and processes elements left-to-right), foldr processes elements right-to-left by
  recursing to the end of the collection first, then applying the function f as the
  recursion unwinds.

  - `reduce` computes: f(f(f(acc, x1), x2), x3)
  - `foldr` computes:  f(x1, f(x2, f(x3, acc)))
  
  This difference matters when:
  1. The operation is non-associative (e.g., division, subtraction, list cons)
  2. You need to build right-associative data structures (e.g., linked lists)
  3. You want lazy evaluation (foldr can short-circuit on lazy sequences)
  4. The combining function needs to see the 'rest result' before processing current
  "
  [f acc [h & t :as coll]]
  (if (seq coll)
    (f h (foldr f acc t))
    acc))

(defn iota
  "Generates a lazy sequence by repeatedly applying a transformation function t to 
  the result of a next-step function nxt, starting from initial value y, and 
  continuing while the stop predicate is truthy. This is a generalized iteration 
  function for creating sequences with custom stepping and transformation logic."
  [t nxt stop y]
  (take-while stop (iterate #(t (nxt %)) y)))

(defn upto
  "Generates an ascending lazy sequence of numbers from start (inclusive) up to but not 
  including end."
  [end start]
  (iota identity inc #(< % end) start))

(defn downto
  "Generates an descending lazy sequence of numbers from start (inclusive) down to but not 
  including end."
  [end start]
  (iota identity dec #(> % end) start))

(defn to
  "Generates a lazy sequence from start to end (exclusive), automatically choosing ascending
  or descending direction based on the relationship between start and end values."
  [start end]
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


