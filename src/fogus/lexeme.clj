(ns fogus.lexeme)

(defn lexeme? [o]
  (or (keyword? o)
      (symbol?  o)
      (string?  o)))

(defn qualify
  "Qualify ident id by resolving it iff it's a symbol, using the given ns name,
  or using the current *ns*."
  ([s] (qualify nil s))
  ([q id]
   {:pre  [(ident? id)]}
   (let [ctor (if (symbol? id) symbol keyword)]
     (if q
       (ctor (name q) (name id))
       (if-let [ns-sym (some-> id namespace symbol)]
         (or (some-> (get (ns-aliases *ns*) ns-sym) str (ctor (name id)))
             id)
         (ctor (str (.name *ns*)) (name id)))))))

(defn unqualify
  "Remove the qualifying ns from the ident."
  [lex]
  (let [ctor (cond
               (symbol? lex)  (comp symbol name)
               (keyword? lex) (comp keyword name)
               :default name)]
    (ctor lex)))

