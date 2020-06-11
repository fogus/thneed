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
