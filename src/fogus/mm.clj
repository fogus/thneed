;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.mm
  "Utilities for working with multimethods.")

(defmacro defmethod-explicit
  "In a standard defmethod body you have no direct reference to the dispatch value itself.
  Instead, you'd have to hardcode it as a literal or look it up from somewhere else.

  This macro allows you to opt into naming the dispatch value to a name via ``:as DV``:

      (defmethod-explicit my-multi :bar :as the-dv [x] (str the-dv :-> x))

  Shadowing ``DV`` in the arglist will nullify the utility of the named dispatch value."
  [multifn dispatch-val & fn-tail]
  (let [[kw n & body] fn-tail]
    (if (= :as kw)
      `(. ~(with-meta multifn {:tag 'clojure.lang.MultiFn})
          addMethod
          ~dispatch-val
          (let [~n ~dispatch-val] (fn ~@body)))
      `(. ~(with-meta multifn {:tag 'clojure.lang.MultiFn})
          addMethod
          ~dispatch-val
          (fn ~@fn-tail)))))
