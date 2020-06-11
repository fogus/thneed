(ns fogus.meta
  "Utilities dealing with metadata.")

(defn massoc [o k v]
   (if-let [m (meta o)]
     (with-meta o (assoc m k v))
     (with-meta o {k v})))



(comment
  (-> {:z 1}
      (massoc :a 42)
      meta)
)
