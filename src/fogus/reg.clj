;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.reg
  "A minimal functional registry system inspired by clojure.spec.alpha.
  
Provides core registry operations for managing namespaced identifiers.
Users maintain their own atoms/maps and the registry functions herein to
manage them. A registry is map from namespaced identifiers (keywords or
symbols) to arbitrary values, enabling global lookup and reuse of named
items.

Important ideas that differentiate registries from raw maps held in
atoms:

- Identifier: Either a keyword or a symbol, preferably qualified.
- Alias: A registry entry whose value is an identifier that points to
another identifier, creating indirection that allows one key to reference
another's value.
- Alias chain: The sequence of identifiers traversed when resolving an
alias, showing each indirection step from the initial key to the final
non-identifier value.
- Cycle: Alias chains may have cycles, and this library can detect and
annotate them.   

Because registries are just maps - use standard Clojure functions for
selection/query."
  (:refer-clojure :exclude [alias]))

(defn register
  "Register an item under identifier k in registry, returning a new registry.
  If item is nil, then the mapping for k is removed from the registry."
  [registry k item]
  {:pre [(ident? k)]}
  (if (nil? item)
    (dissoc registry k)
    (assoc registry k item)))

(defn alias
  "Register identifier k as an alias to target identifier in registry."
  [registry k target]
  {:pre [(ident? target) (ident? k)]}
  (register registry k target))

(defn lookup
  "Lookup identifier k in registry, following alias chains.
  
  If the value at k is itself an identifier, then lookup recursively looks
  it up until finding a non-identifier value. This enables indirection, allowing
  you to register ::foo as ::bar, and resolving ::foo will return whatever
  ::bar points to.

  Returns the resolved item, or nil if k is not found. Attempting to lookup
  a non-identifier is undefined and likely an error."
  [reg k]
  {:pre [(ident? k)]}
  (when (ident? k)
    (loop [item k
           seen #{}]
      (let [v (get reg item)]
        (cond
          (not (ident? v))   v
          (contains? seen v) nil
          :else              (recur v (conj seen v)))))))

(defn lookup!
  "Lookup identifier k in registry, throwing if not found.
  
  Like lookup, but throws an exception if k cannot be resolved.
  Useful when a missing registry entry is an error condition.
  Returns the resolved item."
  [registry k]
  {:pre [(ident? k)]}
  (if (ident? k)
    (or (lookup registry k)
        (throw (ex-info (str "Unable to resolve: " k)
                        {:type ::unresolved
                         :key k})))
    k))

(defn cyclic?
  "Given an alias chain, return true if there is a cycle, flase otherwise."
  [chain]
  (contains? (meta chain) ::cycle-detected-in))

(defn alias-chain
  "Given registry reg, returns the chain of aliases from k to its
  final resolved value. An alias chain is a vector showing the resolution
  chain, or nil if k not found. If the chain has a cycle, then the predicate
  cyclic? will return true fir it."
  [reg k]
  {:pre [(ident? k)]}
  (when (ident? k)
    (loop [item k
           chain []
           seen #{}]
      (let [v (get reg item)]
        (cond
          (nil? v)           nil
          (contains? seen v) (with-meta (conj chain item) {::cycle-detected-in item})
          (not (ident? v))   (conj chain item v)
          :else              (recur v (conj chain item) (conj seen v)))))))

