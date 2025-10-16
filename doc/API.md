
-----
# <a name="fogus.adverbs">fogus.adverbs</a>


Various functions that modify other functions that are not
  (currently) available in clojure.core.




## <a name="fogus.adverbs/apply-layering">`apply-layering`</a><a name="fogus.adverbs/apply-layering"></a>
``` clojure

(apply-layering aspects f args)
```

Layers a collection of aspects with a base function and immediately invokes
  with the provided arguments. Supports early termination via (reduced val).
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/adverbs.clj#L54-L58">Source</a></sub></p>

## <a name="fogus.adverbs/cps->fn">`cps->fn`</a><a name="fogus.adverbs/cps->fn"></a>
``` clojure

(cps->fn f callback)
```

Takes a function f that takes a callback and returns a new fn
  that runs synchronously. If callback throws then the exception
  will be propagated outward.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/adverbs.clj#L12-L23">Source</a></sub></p>

## <a name="fogus.adverbs/kwargify">`kwargify`</a><a name="fogus.adverbs/kwargify"></a>
``` clojure

(kwargify f)
```

Takes a function that expects a map and returns a function that
   accepts keyword arguments on its behalf.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/adverbs.clj#L5-L10">Source</a></sub></p>

## <a name="fogus.adverbs/layer">`layer`</a><a name="fogus.adverbs/layer"></a>
``` clojure

(layer f aspects)
```

Layers multiple aspects around a base function by repeatedly nesting them.
  Aspects are applied left-to-right, with earlier aspects applying closer
  to f.

  An aspect is a higher-order function with signature (fn [next-fn & args] ...)
  that can intercept, transform, or short-circuit execution before/after calling next-fn.
  This provides the full range of before/after/around "advice" patterns:

  - (fn [next-fn arg] (next-fn (before arg)))
  - (fn [next-fn arg] (after (next-fn arg)))
  - (fn [next-fn arg] (let [r (next-fn (before arg))] (after r)))
  
  Each aspect receives the nested result of all previous aspects as its first
  argument. Aspects can return (reduced val) to short-circuit remaining layers,
  preventing inner aspects from executing.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/adverbs.clj#L35-L52">Source</a></sub></p>

## <a name="fogus.adverbs/nest">`nest`</a><a name="fogus.adverbs/nest"></a>
``` clojure

(nest inner outer)
```

Nests one function inside of another. The outer function receives the inner
  function as its first argument, creating a nested execution context where
  the outer potentially controls how/when the inner is invoked.
  
  Supports early termination: if outer returns (reduced val), execution halts.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/adverbs.clj#L25-L33">Source</a></sub></p>

-----
# <a name="fogus.associative">fogus.associative</a>






## <a name="fogus.associative/dissoc-in">`dissoc-in`</a><a name="fogus.associative/dissoc-in"></a>
``` clojure

(dissoc-in asc path)
```

Dissociates a value in a nested associative structure asc, where path is a
  sequence of keys. If the path does not resolve to a valid associative mapping
  then this function is a noop.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/associative.clj#L3-L15">Source</a></sub></p>

-----
# <a name="fogus.config">fogus.config</a>


A dead simple config reader for Clojure supporting multiple formats and locations.




## <a name="fogus.config/-read-format">`-read-format`</a><a name="fogus.config/-read-format"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/config.clj#L8-L8">Source</a></sub></p>

## <a name="fogus.config/-reader">`-reader`</a><a name="fogus.config/-reader"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/config.clj#L7-L7">Source</a></sub></p>

## <a name="fogus.config/read-config">`read-config`</a><a name="fogus.config/read-config"></a>
``` clojure

(read-config from _ format)
```

Usage:
      (config-reader "/path/to/cfg.edn" :as :edn)
  
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/config.clj#L23-L33">Source</a></sub></p>

-----
# <a name="fogus.fun">fogus.fun</a>


A bunch of utilities that you might find in interesting
  functional and/or function-level languages.




## <a name="fogus.fun/!pred">`!pred`</a><a name="fogus.fun/!pred"></a>
``` clojure

(!pred p f)
(!pred p f ex-dispatch)
```

A higher-order function that creates a validated wrapper around another function.
  Given a predicate p, a function f, and an optional exception handler ex-dispatch,
  a closure is returned that executes f with the provided arguments, the result is
  checked against p. If p returns true then the closure throws according to the
  exception dispatch. If ex-dispatch is a string or a map then that data is used to
  form the relevant contents of an ex-info packet. If ex-dispatch is a fn then
  the arguments given to the closure are passed to it and a Throwable is expected
  as its return.
  
  If the predicate returns false, it returns the result unchanged.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/fun.clj#L53-L75">Source</a></sub></p>

## <a name="fogus.fun/downto">`downto`</a><a name="fogus.fun/downto"></a>
``` clojure

(downto end start)
```

Generates an descending lazy sequence of numbers from start (inclusive) down to but not 
  including end.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/fun.clj#L39-L43">Source</a></sub></p>

## <a name="fogus.fun/foldr">`foldr`</a><a name="fogus.fun/foldr"></a>
``` clojure

(foldr f acc [h & t :as coll])
```

Right-associative fold over a collection. Unlike reduce (which is left-associative
  and processes elements left-to-right), foldr processes elements right-to-left by
  recursing to the end of the collection first, then applying the function f as the
  recursion unwinds.

  - `reduce` computes: f(f(f(acc, x1), x2), x3)
  - [`foldr`](#fogus.fun/foldr) computes:  f(x1, f(x2, f(x3, acc)))
  
  This difference matters when:
  1. The operation is non-associative (e.g., division, subtraction, list cons)
  2. You need to build right-associative data structures (e.g., linked lists)
  3. You want lazy evaluation (foldr can short-circuit on lazy sequences)
  4. The combining function needs to see the 'rest result' before processing current
  
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/fun.clj#L5-L23">Source</a></sub></p>

## <a name="fogus.fun/iota">`iota`</a><a name="fogus.fun/iota"></a>
``` clojure

(iota t nxt stop y)
```

Generates a lazy sequence by repeatedly applying a transformation function t to 
  the result of a next-step function nxt, starting from initial value y, and 
  continuing while the stop predicate is truthy. This is a generalized iteration 
  function for creating sequences with custom stepping and transformation logic.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/fun.clj#L25-L31">Source</a></sub></p>

## <a name="fogus.fun/to">`to`</a><a name="fogus.fun/to"></a>
``` clojure

(to start end)
```

Generates a lazy sequence from start to end (exclusive), automatically choosing ascending
  or descending direction based on the relationship between start and end values.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/fun.clj#L45-L51">Source</a></sub></p>

## <a name="fogus.fun/upto">`upto`</a><a name="fogus.fun/upto"></a>
``` clojure

(upto end start)
```

Generates an ascending lazy sequence of numbers from start (inclusive) up to but not 
  including end.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/fun.clj#L33-L37">Source</a></sub></p>

-----
# <a name="fogus.it">fogus.it</a>


Utilities and functions pertaining to Information Theory.




## <a name="fogus.it/entropy">`entropy`</a><a name="fogus.it/entropy"></a>
``` clojure

(entropy s)
```

Calculate the information entropy (Shannon entropy) of a
  given input string.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/it.clj#L4-L16">Source</a></sub></p>

-----
# <a name="fogus.java">fogus.java</a>


Java host utilities.




## <a name="fogus.java/build-system-info-map">`build-system-info-map`</a><a name="fogus.java/build-system-info-map"></a>
``` clojure

(build-system-info-map)
(build-system-info-map base)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/java.clj#L12-L32">Source</a></sub></p>

## <a name="fogus.java/virtual-threads-available?">`virtual-threads-available?`</a><a name="fogus.java/virtual-threads-available?"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/java.clj#L5-L10">Source</a></sub></p>

-----
# <a name="fogus.laziness">fogus.laziness</a>


Utilities dealing with lazy and eager evaluation




## <a name="fogus.laziness/seq1">`seq1`</a><a name="fogus.laziness/seq1"></a>
``` clojure

(seq1 s)
```

Ensures that chunked sequences are evaluated one element
  at a time.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/laziness.clj#L4-L10">Source</a></sub></p>

-----
# <a name="fogus.lexeme">fogus.lexeme</a>






## <a name="fogus.lexeme/lexeme?">`lexeme?`</a><a name="fogus.lexeme/lexeme?"></a>
``` clojure

(lexeme? o)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/lexeme.clj#L3-L6">Source</a></sub></p>

## <a name="fogus.lexeme/qualified?">`qualified?`</a><a name="fogus.lexeme/qualified?"></a>
``` clojure

(qualified? id)
```

Given an identifier id, returns true if it's namespace qualified,
  and false otherwise.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/lexeme.clj#L8-L12">Source</a></sub></p>

## <a name="fogus.lexeme/qualify">`qualify`</a><a name="fogus.lexeme/qualify"></a>
``` clojure

(qualify s)
(qualify q id)
```

Qualify ident id by resolving it iff it's a symbol, using the given ns name,
  or using the current *ns*.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/lexeme.clj#L14-L26">Source</a></sub></p>

## <a name="fogus.lexeme/unqualify">`unqualify`</a><a name="fogus.lexeme/unqualify"></a>
``` clojure

(unqualify lex)
```

Remove the qualifying ns from the ident.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/lexeme.clj#L28-L35">Source</a></sub></p>

-----
# <a name="fogus.maps">fogus.maps</a>






## <a name="fogus.maps/assoc-iff">`assoc-iff`</a><a name="fogus.maps/assoc-iff"></a>
``` clojure

(assoc-iff m k v)
(assoc-iff m k v & kvs)
```

Like assoc, but only associates key-value pairs when the value is non-nil.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/maps.clj#L29-L38">Source</a></sub></p>

## <a name="fogus.maps/deep-merge">`deep-merge`</a><a name="fogus.maps/deep-merge"></a>
``` clojure

(deep-merge & vals)
```

Recursively merges nested maps. When merging values at the same key:
  
  - If both values are maps, recursively merges them
  - Otherwise, takes the rightmost value (consistent with merge)
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/maps.clj#L40-L48">Source</a></sub></p>

## <a name="fogus.maps/keys-apply">`keys-apply`</a><a name="fogus.maps/keys-apply"></a>
``` clojure

(keys-apply m ks f)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/maps.clj#L4-L10">Source</a></sub></p>

## <a name="fogus.maps/manip-keys">`manip-keys`</a><a name="fogus.maps/manip-keys"></a>
``` clojure

(manip-keys m ks f)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/maps.clj#L18-L27">Source</a></sub></p>

## <a name="fogus.maps/manip-map">`manip-map`</a><a name="fogus.maps/manip-map"></a>
``` clojure

(manip-map m ks f)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/maps.clj#L12-L16">Source</a></sub></p>

-----
# <a name="fogus.meta">fogus.meta</a>


Utilities dealing with metadata.




## <a name="fogus.meta/massoc">`massoc`</a><a name="fogus.meta/massoc"></a>
``` clojure

(massoc o k v)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/meta.clj#L6-L7">Source</a></sub></p>

## <a name="fogus.meta/mupdate">`mupdate`</a><a name="fogus.meta/mupdate"></a>
``` clojure

(mupdate o k f x)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/meta.clj#L9-L10">Source</a></sub></p>

-----
# <a name="fogus.mm">fogus.mm</a>






## <a name="fogus.mm/defmethod-anaphoric">`defmethod-anaphoric`</a><a name="fogus.mm/defmethod-anaphoric"></a>
``` clojure

(defmethod-anaphoric multifn dispatch-val & fn-tail)
```
Function.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/mm.clj#L16-L22">Source</a></sub></p>

## <a name="fogus.mm/defmethod-explicit">`defmethod-explicit`</a><a name="fogus.mm/defmethod-explicit"></a>
``` clojure

(defmethod-explicit multifn dispatch-val & fn-tail)
```
Function.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/mm.clj#L3-L14">Source</a></sub></p>

-----
# <a name="fogus.numbers">fogus.numbers</a>


Utilities dealing with numbers.




## <a name="fogus.numbers/num->roman">`num->roman`</a><a name="fogus.numbers/num->roman"></a>
``` clojure

(num->roman n)
```

Converts a positive number between 1 and 3999, inclusive to a Roman numeral string.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/numbers.clj#L25-L36">Source</a></sub></p>

## <a name="fogus.numbers/parse-roman">`parse-roman`</a><a name="fogus.numbers/parse-roman"></a>
``` clojure

(parse-roman s)
```

Converts a Roman numeral string to its numeric value between 1 and 3999, inclusive.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/numbers.clj#L8-L23">Source</a></sub></p>

-----
# <a name="fogus.reg">fogus.reg</a>


A minimal functional registry system inspired by clojure.spec.alpha.
  
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
selection/query.




## <a name="fogus.reg/alias">`alias`</a><a name="fogus.reg/alias"></a>
``` clojure

(alias registry k target)
```

Register identifier k as an alias to target identifier in registry.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/reg.clj#L36-L40">Source</a></sub></p>

## <a name="fogus.reg/alias-chain">`alias-chain`</a><a name="fogus.reg/alias-chain"></a>
``` clojure

(alias-chain reg k)
```

Given registry reg, returns the chain of aliases from k to its
  final resolved value. An alias chain is a vector showing the resolution
  chain, or nil if k not found. If the chain has a cycle, then the predicate
  cyclic? will return true fir it.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/reg.clj#L83-L99">Source</a></sub></p>

## <a name="fogus.reg/cyclic?">`cyclic?`</a><a name="fogus.reg/cyclic?"></a>
``` clojure

(cyclic? chain)
```

Given an alias chain, return true if there is a cycle, flase otherwise.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/reg.clj#L78-L81">Source</a></sub></p>

## <a name="fogus.reg/lookup">`lookup`</a><a name="fogus.reg/lookup"></a>
``` clojure

(lookup reg k)
```

Lookup identifier k in registry, following alias chains.
  
  If the value at k is itself an identifier, then lookup recursively looks
  it up until finding a non-identifier value. This enables indirection, allowing
  you to register ::foo as ::bar, and resolving ::foo will return whatever
  ::bar points to.

  Returns the resolved item, or nil if k is not found. Attempting to lookup
  a non-identifier is undefined and likely an error.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/reg.clj#L42-L61">Source</a></sub></p>

## <a name="fogus.reg/lookup!">`lookup!`</a><a name="fogus.reg/lookup!"></a>
``` clojure

(lookup! registry k)
```

Lookup identifier k in registry, throwing if not found.
  
  Like lookup, but throws an exception if k cannot be resolved.
  Useful when a missing registry entry is an error condition.
  Returns the resolved item.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/reg.clj#L63-L76">Source</a></sub></p>

## <a name="fogus.reg/register">`register`</a><a name="fogus.reg/register"></a>
``` clojure

(register registry k item)
```

Register an item under identifier k in registry, returning a new registry.
  If item is nil, then the mapping for k is removed from the registry.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/reg.clj#L27-L34">Source</a></sub></p>

-----
# <a name="fogus.sets">fogus.sets</a>


Utilities dealing with sets.




## <a name="fogus.sets/minimize-sets">`minimize-sets`</a><a name="fogus.sets/minimize-sets"></a>
``` clojure

(minimize-sets sets)
```

Takes a seq of sets and returns a seq of the mutually different sets. That is, the returned seq
   will contain sets that have no similar items between them.
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/sets.clj#L5-L15">Source</a></sub></p>

-----
# <a name="fogus.shell">fogus.shell</a>






## <a name="fogus.shell/go">`go`</a><a name="fogus.shell/go"></a>
``` clojure

(go & args)
```
<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/shell.clj#L10-L16">Source</a></sub></p>

## <a name="fogus.shell/parse-args">`parse-args`</a><a name="fogus.shell/parse-args"></a>



<p><sub><a href="https://github.com/fogus/thneed/blob/master/src/fogus/shell.clj#L8-L8">Source</a></sub></p>
