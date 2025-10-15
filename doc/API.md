
-----
# <a name="fogus.adverbs">fogus.adverbs</a>


Various functions that modify other functions that are not
  (currently) available in clojure.core.




## <a name="fogus.adverbs/cps->fn">`cps->fn`</a><a name="fogus.adverbs/cps->fn"></a>
``` clojure

(cps->fn f callback)
```

Converts a function taking a callback to one that runs synchronously.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/adverbs.clj#L12-L21">Source</a></sub></p>

## <a name="fogus.adverbs/kwargify">`kwargify`</a><a name="fogus.adverbs/kwargify"></a>
``` clojure

(kwargify f)
```

Takes a function that expects a map and returns a function that
   accepts keyword arguments on its behalf.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/adverbs.clj#L5-L10">Source</a></sub></p>

-----
# <a name="fogus.associative">fogus.associative</a>






## <a name="fogus.associative/dissoc-in">`dissoc-in`</a><a name="fogus.associative/dissoc-in"></a>
``` clojure

(dissoc-in asc path)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/associative.clj#L3-L11">Source</a></sub></p>

-----
# <a name="fogus.config">fogus.config</a>


A dead simple config reader for Clojure supporting multiple formats and locations.




## <a name="fogus.config/-read-format">`-read-format`</a><a name="fogus.config/-read-format"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/config.clj#L8-L8">Source</a></sub></p>

## <a name="fogus.config/-reader">`-reader`</a><a name="fogus.config/-reader"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/config.clj#L7-L7">Source</a></sub></p>

## <a name="fogus.config/read-config">`read-config`</a><a name="fogus.config/read-config"></a>
``` clojure

(read-config from _ format)
```

Usage:
      (config-reader "/path/to/cfg.edn" :as :edn)
  
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/config.clj#L23-L33">Source</a></sub></p>

-----
# <a name="fogus.fun">fogus.fun</a>


A bunch of utilities that you might find in interesting
  functional and/or function-level languages.




## <a name="fogus.fun/!pred">`!pred`</a><a name="fogus.fun/!pred"></a>
``` clojure

(!pred p f)
(!pred p f ex-dispatch)
```

Returns function that takes args and if (apply f args) is not nil, returns it.
  Otherwise throw exception per ex-dispatch - nil / string / map throw ex-info, or an
  ifn? constructs an arbitrary exceptions (and is passed the function args).
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/fun.clj#L30-L46">Source</a></sub></p>

## <a name="fogus.fun/downto">`downto`</a><a name="fogus.fun/downto"></a>
``` clojure

(downto end start)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/fun.clj#L22-L23">Source</a></sub></p>

## <a name="fogus.fun/foldr">`foldr`</a><a name="fogus.fun/foldr"></a>
``` clojure

(foldr f acc [h & t :as coll])
```

Fold right... as opposed to fold left (i.e. reduce).
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/fun.clj#L5-L10">Source</a></sub></p>

## <a name="fogus.fun/iota">`iota`</a><a name="fogus.fun/iota"></a>
``` clojure

(iota t nxt stop y)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/fun.clj#L12-L13">Source</a></sub></p>

## <a name="fogus.fun/parse-long">`parse-long`</a><a name="fogus.fun/parse-long"></a>
``` clojure

(parse-long %1)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/fun.clj#L48-L48">Source</a></sub></p>

## <a name="fogus.fun/to">`to`</a><a name="fogus.fun/to"></a>
``` clojure

(to start end)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/fun.clj#L25-L28">Source</a></sub></p>

## <a name="fogus.fun/upto">`upto`</a><a name="fogus.fun/upto"></a>
``` clojure

(upto end start)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/fun.clj#L19-L20">Source</a></sub></p>

## <a name="fogus.fun/validate-long">`validate-long`</a><a name="fogus.fun/validate-long"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/fun.clj#L50-L50">Source</a></sub></p>

## <a name="fogus.fun/validate-not-even">`validate-not-even`</a><a name="fogus.fun/validate-not-even"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/fun.clj#L54-L54">Source</a></sub></p>

-----
# <a name="fogus.it">fogus.it</a>


Utilities and functions pertaining to Information Theory.




## <a name="fogus.it/entropy">`entropy`</a><a name="fogus.it/entropy"></a>
``` clojure

(entropy s)
```

Calculate the information entropy (Shannon entropy) of a
  given input string.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/it.clj#L5-L17">Source</a></sub></p>

-----
# <a name="fogus.java">fogus.java</a>


Java host utilities.




## <a name="fogus.java/build-system-info-map">`build-system-info-map`</a><a name="fogus.java/build-system-info-map"></a>
``` clojure

(build-system-info-map)
(build-system-info-map base)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/java.clj#L12-L32">Source</a></sub></p>

## <a name="fogus.java/virtual-threads-available?">`virtual-threads-available?`</a><a name="fogus.java/virtual-threads-available?"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/java.clj#L5-L10">Source</a></sub></p>

-----
# <a name="fogus.laziness">fogus.laziness</a>


Utilities dealing with lazy and eager evaluation




## <a name="fogus.laziness/seq1">`seq1`</a><a name="fogus.laziness/seq1"></a>
``` clojure

(seq1 s)
```

Ensures that chunked sequences are evaluated one element
  at a time.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/laziness.clj#L4-L10">Source</a></sub></p>

-----
# <a name="fogus.lexeme">fogus.lexeme</a>






## <a name="fogus.lexeme/lexeme?">`lexeme?`</a><a name="fogus.lexeme/lexeme?"></a>
``` clojure

(lexeme? o)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/lexeme.clj#L3-L6">Source</a></sub></p>

## <a name="fogus.lexeme/qualify">`qualify`</a><a name="fogus.lexeme/qualify"></a>
``` clojure

(qualify s)
(qualify q id)
```

Qualify ident id by resolving it iff it's a symbol, using the given ns name,
  or using the current *ns*.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/lexeme.clj#L8-L20">Source</a></sub></p>

## <a name="fogus.lexeme/unqualify">`unqualify`</a><a name="fogus.lexeme/unqualify"></a>
``` clojure

(unqualify lex)
```

Remove the qualifying ns from the ident.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/lexeme.clj#L22-L29">Source</a></sub></p>

-----
# <a name="fogus.maps">fogus.maps</a>






## <a name="fogus.maps/assoc-iff">`assoc-iff`</a><a name="fogus.maps/assoc-iff"></a>
``` clojure

(assoc-iff m k v)
(assoc-iff m k v & kvs)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/maps.clj#L29-L37">Source</a></sub></p>

## <a name="fogus.maps/deep-merge">`deep-merge`</a><a name="fogus.maps/deep-merge"></a>
``` clojure

(deep-merge & vals)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/maps.clj#L39-L42">Source</a></sub></p>

## <a name="fogus.maps/keys-apply">`keys-apply`</a><a name="fogus.maps/keys-apply"></a>
``` clojure

(keys-apply m ks f)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/maps.clj#L4-L10">Source</a></sub></p>

## <a name="fogus.maps/manip-keys">`manip-keys`</a><a name="fogus.maps/manip-keys"></a>
``` clojure

(manip-keys m ks f)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/maps.clj#L18-L27">Source</a></sub></p>

## <a name="fogus.maps/manip-map">`manip-map`</a><a name="fogus.maps/manip-map"></a>
``` clojure

(manip-map m ks f)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/maps.clj#L12-L16">Source</a></sub></p>

-----
# <a name="fogus.meta">fogus.meta</a>


Utilities dealing with metadata.




## <a name="fogus.meta/massoc">`massoc`</a><a name="fogus.meta/massoc"></a>
``` clojure

(massoc o k v)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/meta.clj#L6-L7">Source</a></sub></p>

## <a name="fogus.meta/mupdate">`mupdate`</a><a name="fogus.meta/mupdate"></a>
``` clojure

(mupdate o k f x)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/meta.clj#L9-L10">Source</a></sub></p>

-----
# <a name="fogus.mm">fogus.mm</a>






## <a name="fogus.mm/defmethod-anaphoric">`defmethod-anaphoric`</a><a name="fogus.mm/defmethod-anaphoric"></a>
``` clojure

(defmethod-anaphoric multifn dispatch-val & fn-tail)
```
Function.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/mm.clj#L16-L22">Source</a></sub></p>

## <a name="fogus.mm/defmethod-explicit">`defmethod-explicit`</a><a name="fogus.mm/defmethod-explicit"></a>
``` clojure

(defmethod-explicit multifn dispatch-val & fn-tail)
```
Function.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/mm.clj#L3-L14">Source</a></sub></p>

-----
# <a name="fogus.reg">fogus.reg</a>


A minimal functional registry system inspired by clojure.spec.alpha.
  
  Provides core registry operations for managing namespaced identifiers.
  Users maintain their own atoms/maps and use foo.reg functions to
  manipulate them. This allows for:
  - Multiple independent registries
  - Testing with isolated registries
  - Pure functional operations
  
  Registries are just maps - use standard Clojure functions for queries:
  - (keys registry) for all keys
  - (get registry k) for direct lookup
  - (filter pred registry) for queries
  - (merge r1 r2) for combining registries
  - (empty? registry) to check if empty
  - etc.




## <a name="fogus.reg/alias-chain">`alias-chain`</a><a name="fogus.reg/alias-chain"></a>
``` clojure

(alias-chain registry k)
```

Return the chain of aliases from k to its final resolved value.
  
  Args:
    registry - A registry map or atom
    k - An identifier (keyword or symbol)
  
  Returns:
    A vector showing the resolution chain, or nil if k not found.
    The last element is the final resolved value.
  
  Examples:
    Given: {::c "value", ::b ::c, ::a ::b}
    (alias-chain reg ::a) => [::a ::b ::c "value"]
    (alias-chain reg ::b) => [::b ::c "value"]
    (alias-chain reg ::c) => [::c "value"]
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/reg.clj#L109-L145">Source</a></sub></p>

## <a name="fogus.reg/all-keys-in">`all-keys-in`</a><a name="fogus.reg/all-keys-in"></a>
``` clojure

(all-keys-in registry)
```

Return all keys in a registry (atom or map).
  
  Convenience function for (set (keys registry)).
  Handles both atoms and plain maps.
  
  Args:
    registry - A registry map or atom
  
  Returns:
    A set of all keys.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/reg.clj#L167-L182">Source</a></sub></p>

## <a name="fogus.reg/register">`register`</a><a name="fogus.reg/register"></a>
``` clojure

(register registry k item)
```

Register an item under key k in registry, returning new registry.
  
  This is a pure function - it returns a new registry map without
  modifying the input.
  
  Args:
    registry - A registry map
    k - An identifier (keyword or symbol)
    item - The item to register (can be anything)
  
  Returns:
    A new registry map with the item registered under k.
    If item is nil, removes k from the registry.
  
  Examples:
    (register {} ::my-spec my-spec-fn)
    (register reg ::name string?)
    (register reg ::old-spec nil) ; removes ::old-spec
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/reg.clj#L22-L44">Source</a></sub></p>

## <a name="fogus.reg/register-alias">`register-alias`</a><a name="fogus.reg/register-alias"></a>
``` clojure

(register-alias registry k target)
```

Register k as an alias to target in registry.
  
  This is a convenience function that simply registers k with
  the value of target (an identifier).
  
  Args:
    registry - A registry map
    k - The alias identifier
    target - The identifier this alias points to
  
  Returns:
    A new registry with the alias registered.
  
  Examples:
    (register-alias reg ::short-name ::my.long.namespace/name)
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/reg.clj#L147-L165">Source</a></sub></p>

## <a name="fogus.reg/resolve">`resolve`</a><a name="fogus.reg/resolve"></a>
``` clojure

(resolve registry k)
```

Resolve key k in registry, following alias chains.
  
  If the value at k is itself an identifier (keyword/symbol),
  recursively looks it up until finding a non-identifier value.
  
  This enables indirection: you can register ::foo as ::bar,
  and resolving ::foo will return whatever ::bar points to.
  
  Args:
    registry - A registry map or atom
    k - An identifier (keyword or symbol)
  
  Returns:
    The resolved item, or nil if k is not found or is not an identifier.
  
  Examples:
    Given: {::bar "value", ::foo ::bar}
    (resolve reg ::foo) => "value"
    (resolve reg ::bar) => "value"
    (resolve reg ::baz) => nil
    (resolve reg "not-ident") => nil
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/reg.clj#L46-L84">Source</a></sub></p>

## <a name="fogus.reg/resolve!">`resolve!`</a><a name="fogus.reg/resolve!"></a>
``` clojure

(resolve! registry k)
```

Resolve key k in registry, throwing if not found.
  
  Like resolve, but throws an exception if k cannot be resolved.
  Useful when a missing registry entry is an error condition.
  
  Args:
    registry - A registry map or atom
    k - An identifier (keyword or symbol)
  
  Returns:
    The resolved item
  
  Throws:
    ExceptionInfo if k is an identifier but cannot be resolved
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/reg.clj#L86-L107">Source</a></sub></p>

-----
# <a name="fogus.sets">fogus.sets</a>


Utilities dealing with sets.




## <a name="fogus.sets/minimize-sets">`minimize-sets`</a><a name="fogus.sets/minimize-sets"></a>
``` clojure

(minimize-sets sets)
```

Takes a seq of sets and returns a seq of the mutually different sets. That is, the returned seq
   will contain sets that have no similar items between them.
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/sets.clj#L5-L15">Source</a></sub></p>

-----
# <a name="fogus.shell">fogus.shell</a>






## <a name="fogus.shell/go">`go`</a><a name="fogus.shell/go"></a>
``` clojure

(go & args)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/shell.clj#L10-L16">Source</a></sub></p>

## <a name="fogus.shell/parse-args">`parse-args`</a><a name="fogus.shell/parse-args"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/main/src/fogus/shell.clj#L8-L8">Source</a></sub></p>
