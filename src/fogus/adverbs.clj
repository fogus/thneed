(ns fogus.adverbs
  "Various functions that modify other functions that are not
  (currently) available in clojure.core.")

(defn kwargify
  "Takes a function that expects a map and returns a function that
   accepts keyword arguments on its behalf."
  [f]
  (fn [& kwargs]
    (f (apply hash-map kwargs))))

(defn cps->fn
  "Takes a function f that takes a callback and returns a new fn
  that runs synchronously. If callback throws then the exception
  will be propagated outward."
  [f callback]
  (fn [& args]
    (let [p (promise)]
      (apply f
             (fn cb [& results]
               (deliver p (apply callback results)))
             args)
      @p)))
