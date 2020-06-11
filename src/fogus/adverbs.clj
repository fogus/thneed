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
  "Converts a function taking a callback to one that runs synchronously."
  [f callback]
  (fn [& args]
    (let [p (promise)]
      (apply f
             (fn [result]
               (deliver p (callback result)))
             args)
      @p)))


(comment
  (def hap
    (cps->fn #(do (Thread/sleep 3000)
                  (%1 (apply + %&)))
             #(println %)))

  (hap 1 2 3)
)