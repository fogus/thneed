(ns fogus.lexeme)

(defn unqualify [lex]
  (let [ctor (condp = (class lex)
               clojure.lang.Symbol (comp symbol name)
               clojure.lang.Keyword (comp keyword name)
               name)]
    (ctor lex)))

(unqualify :a/b)
