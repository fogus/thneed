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

(deftest log-distance-tests
  (testing "basic behavior"
    (is (= (m/log-distance 3 50) (m/log-distance 50 3)))
    (is (== 0 (m/log-distance 10 10)))
    (is (< (Math/abs (- (m/log-distance 3 50) 1.2218487496))
           1e-9)))
  (testing "exact powers of ten"
    (is (== 1 (m/log-distance 1 10)))
    (is (== 2 (m/log-distance 1 100)))
    (is (== 3 (m/log-distance 0.01 10)))
    (is (== 4 (m/log-distance 0.001 10))))
  (testing "non-positive inputs throw"
    (is (thrown? AssertionError (m/log-distance 0 10)))
    (is (thrown? AssertionError (m/log-distance -1 10)))
    (is (thrown? AssertionError (m/log-distance 10 -5)))))
