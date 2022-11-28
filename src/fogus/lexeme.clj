(ns fogus.lexeme)

(defn lexeme? [o]
  (or (keyword? o)
      (symbol?  o)
      (string?  o)))

(defn unqualify [lex]
  (let [ctor (cond
               (symbol? lex)  (comp symbol name)
               (keyword? lex) (comp keyword name)
               :default name)]
    (ctor lex)))

