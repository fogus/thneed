(ns fogus.lexeme-test
  (:require [clojure.test :refer :all]
            [fogus.lexeme :as lex]))

(set! *warn-on-reflection* true)

(defn ns-fixture
  [f]
  (in-ns 'fogus.lexeme-test)
  (f))

(use-fixtures :once ns-fixture)

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

(alias 'cc 'clojure.core)

(deftest qualify-test
  (is (= (lex/qualify 'cc/foo)
         'clojure.core/foo))

  (is (= (lex/qualify ::cc/foo)
         :clojure.core/foo))
  
  (is (= (lex/qualify 'foo)
         'fogus.lexeme-test/foo))

  (is (= (lex/qualify ::foo)
         ::foo))

  (is (= (lex/qualify 'foo)
         `foo))

  (is (= (lex/qualify :foo)
         ::foo))
  
  (is (= (lex/qualify 'a/foo)
         'a/foo))

  (is (= (lex/qualify :a :foo)
         :a/foo))
  
  (is (= (lex/qualify 'a 'foo)
         'a/foo))

  (is (= (lex/qualify 'a `foo)
         'a/foo)))
