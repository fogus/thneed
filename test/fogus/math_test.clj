;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.math-test
  (:require [clojure.test :refer :all]
            [fogus.math :as m]))

(deftest order-distance-tests
  (testing "basic behavior"
    (is (= (m/order-distance 3 50) (m/order-distance 50 3)))
    (is (== 0 (m/order-distance 10 10)))
    (is (< (Math/abs (- (m/order-distance 3 50) 1.2218487496))
           1e-9)))
  (testing "exact powers of ten"
    (is (== 1 (m/order-distance 1 10)))
    (is (== 2 (m/order-distance 1 100)))
    (is (== 3 (m/order-distance 0.01 10)))
    (is (== 4 (m/order-distance 0.001 10))))
  (testing "non-positive inputs throw"
    (is (thrown? AssertionError (m/order-distance 0 10)))
    (is (thrown? AssertionError (m/order-distance -1 10)))
    (is (thrown? AssertionError (m/order-distance 10 -5)))))

(deftest oom-tests
  (testing "order-apart?"
    (is (m/order-apart? 3 50))
    (is (not (m/order-apart? 3 50 2)))
    (is (m/order-apart? 1 1000 3)))

  (testing "within-order?"
    (is (m/within-order? 3 9))
    (is (not (m/within-order? 10 100)))
    (is (not (and (m/within-order? 50 60)
                  (m/order-apart? 50 60 1))))))
