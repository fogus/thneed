(ns fogus.logic
  (:require [clojure.core.logic :as °]))

(defn mapo [fun coll res]
  (°/conda
    [(°/emptyo coll) (°/emptyo res)]
    [(°/fresh [v r tail tail-res]
       (°/conso v tail coll)
       (°/conso r tail-res res)
       (fun v r)
       (mapo fun tail tail-res))]))

