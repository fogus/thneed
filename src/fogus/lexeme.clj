;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.lexeme)

(defn lexeme? [o]
  (or (keyword? o)
      (symbol?  o)
      (string?  o)))

(defn qualified?
  "Given an identifier id, returns true if it's namespace qualified,
  and false otherwise."
  [id]
  (and (ident? id) (namespace id)))

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

