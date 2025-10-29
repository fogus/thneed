;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.mm)

(defmacro defmethod-explicit
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

(defmacro defmethod-anaphoric
  [multifn dispatch-val & fn-tail]
  `(. ~(with-meta multifn {:tag 'clojure.lang.MultiFn})
      addMethod
      ~dispatch-val
      (let [~'$ ~dispatch-val]
        (fn ~@fn-tail))))
