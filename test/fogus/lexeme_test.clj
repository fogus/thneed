(ns fogus.lexeme-test
  (:require [clojure.test :refer :all]
            [fogus.lexeme :as lex]))

(set! *warn-on-reflection* true)

(deftest lexeme?-test
  (are [r o] (= r (lex/lexeme? o))
    true 'a
    true :a
    true "a"
    false 42
    false []))

(deftest unqualify-test
  (is (= 'bar            (lex/unqualify 'foo/bar)))
  (is (= :bar            (lex/unqualify :foo/bar)))
  (is (= "foo"           (lex/unqualify "foo")))
  (is (thrown? Exception (lex/unqualify 42))))
