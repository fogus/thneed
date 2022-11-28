(ns fogus.lexeme-test
  (:require [clojure.test :refer :all]
            [fogus.lexeme :as lex]))

(set! *warn-on-reflection* true)

(deftest unqualify-test
  (is (= 'bar            (lex/unqualify 'foo/bar)))
  (is (= :bar            (lex/unqualify :foo/bar)))
  (is (= "foo"           (lex/unqualify "foo")))
  (is (thrown? Exception (lex/unqualify 42))))
