(ns fogus.associative-test
  (:require [clojure.test :refer :all]
            [fogus.associative :as asc]))

(deftest dissoc-in-test
  (is (= {:a 1}                (asc/dissoc-in {:a 1 :b 2} [:b])))
  (is (= {:a 1 :b {:d 3}}      (asc/dissoc-in {:a 1 :b {:c 2 :d 3}} [:b :c])))
  (is (= {:a 1 :b {:c 2 :d 3}} (asc/dissoc-in {:a 1 :b {:c 2 :d 3}} [:b :z])))
  (is (= {:a 1 :b {}}          (asc/dissoc-in {:a 1 :b {:c 2}} [:b :c])))
  (is (= {:a 1}                (asc/dissoc-in {:a 1} [:b :c])))
  (is (thrown? Exception       (asc/dissoc-in {:a 1 :b :not-a-map} [:b :c])))
  (is (= {}                    (asc/dissoc-in {:a 1} [:a])))
  (is (= nil                   (asc/dissoc-in nil [:a])))
  (is (= {}                    (asc/dissoc-in {} [:a]))))
