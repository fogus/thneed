;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.adverbs
  "Various functions that modify other functions that are not
  (currently) available in clojure.core.")

(defn kwargify
  "Takes a function that expects a map and returns a function that
   accepts keyword arguments on its behalf."
  [f]
  (fn [& kwargs]
    (f (apply hash-map kwargs))))

(defn cps->fn
  "Takes a function f that takes a callback and returns a new fn
  that runs synchronously. If callback throws then the exception
  will be propagated outward."
  [f callback]
  (fn [& args]
    (let [p (promise)]
      (apply f
             (fn cb [& results]
               (deliver p (apply callback results)))
             args)
      @p)))

(defn nest
  "Nests one function inside of another. The outer function receives the inner
  function as its first argument, creating a nested execution context where
  the outer potentially controls how/when the inner is invoked.
  
  Supports early termination: if outer returns (reduced val), execution halts."  
  [inner outer]
  (fn [& args]
    (unreduced (apply outer inner args))))

(defn layer
  "Layers multiple aspects around a base function by repeatedly nesting them.
  Aspects are applied left-to-right, with earlier aspects applying closer
  to f.

  An aspect is a higher-order function with signature (fn [next-fn & args] ...)
  that can intercept, transform, or short-circuit execution before/after calling next-fn.
  This provides the full range of before/after/around \"advice\" patterns:

  - (fn [next-fn arg] (next-fn (before arg)))
  - (fn [next-fn arg] (after (next-fn arg)))
  - (fn [next-fn arg] (let [r (next-fn (before arg))] (after r)))
  
  Each aspect receives the nested result of all previous aspects as its first
  argument. Aspects can return (reduced val) to short-circuit remaining layers,
  preventing inner aspects from executing."
  [f aspects]
  (reduce nest f aspects))

(defn apply-layering
  "Layers a collection of aspects with a base function and immediately invokes
  with the provided arguments. Supports early termination via (reduced val)."
  [aspects f args]
  (unreduced (apply (layer f aspects) args)))
