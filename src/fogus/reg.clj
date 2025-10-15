(ns fogus.reg
  "A minimal functional registry system inspired by clojure.spec.alpha.
  
  Provides core registry operations for managing namespaced identifiers.
  Users maintain their own atoms/maps and the registry functions herein to
  manage them. A registry is map from namespaced identifiers (keywords or
  symbols) to arbitrary values, enabling global lookup and reuse of named
  items.

  A couple of ideas that differentiate registries from raw maps held in
  atoms:

  - Alias: A registry entry whose value is an identifier that points to
  another registry entry, creating indirection that allows one key to reference
  another's value.
  - Alias chain: The sequence of identifiers traversed when resolving an
  alias, showing each indirection step from the initial key to the final
  non-identifier value.
  - Cycle: Alias chains may have cycles, and this library can detect and
  annotate them.   
  
  Because registries are just maps - use standard Clojure functions for
  selection/query.
  "
  (:refer-clojure :exclude [alias]))

(defn register
  "Register an item under key k in registry, returning a new registry.
  If item is nil, then the mapping for k is removed from the registry."
  [registry k item]
  (if (nil? item)
    (dissoc registry k)
    (assoc registry k item)))

(defn alias
  "Register k as an alias to target in registry."
  [registry k target]
  {:pre [(ident? target)]}
  (register registry k target))

(defn lookup
  "Lookup key k in registry, following alias chains.
  
  If the value at k is itself an identifier (keyword/symbol),
  recursively looks it up until finding a non-identifier value.
  
  This enables indirection: you can register ::foo as ::bar,
  and resolving ::foo will return whatever ::bar points to.

  Returns the resolved item, or nil if k is not found or is
  not an identifier."
  [reg k]
  (when (ident? k)
    (loop [item k
           seen #{}]
      (let [v (get reg item)]
        (cond
          (not (ident? v))   v
          (contains? seen v) nil
          :else              (recur v (conj seen v)))))))

(defn lookup!
  "Lookup key k in registry, throwing if not found.
  
  Like lookup, but throws an exception if k cannot be resolved.
  Useful when a missing registry entry is an error condition.
  Returns the resolved item."
  [registry k]
  (if (ident? k)
    (or (lookup registry k)
        (throw (ex-info (str "Unable to resolve: " k)
                        {:type ::unresolved
                         :key k})))
    k))

(defn alias-chain
  "Given registry reg, returns the chain of aliases from k to its
  final resolved value. An alias chain is a vector showing the resolution
  chain, or nil if k not found. The last element in the chain is the final
  resolved value or the keyword :fogus.reg/cycle-detected if a cycle was
  detected."
  [reg k]
  (when (ident? k)
    (loop [item k
           chain []
           seen #{}]
      (let [v (get reg item)]
        (cond
          (nil? v)           nil
          (contains? seen v) (conj chain item ::cycle-detected)
          (not (ident? v))   (conj chain item v)
          :else              (recur v (conj chain item) (conj seen v)))))))

(defn cyclic?
  [chain]
  (= ::cycle-detected (last chain)))
