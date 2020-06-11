(ns fogus.meta
  "Utilities dealing with metadata.")

(defn massoc [o k v]
  (with-meta o (assoc (meta o) k v)))

(defn mupdate [o k f x]
  (with-meta o (update (meta o) k f x)))

(comment
  (-> {:z 1}
      (massoc :a 42)
      meta)

  (update nil :a (constantly 1) 2)
)
