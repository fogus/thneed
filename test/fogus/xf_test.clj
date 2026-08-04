(ns fogus.xf-test
  (:require [clojure.test :refer [deftest testing is]]
            [fogus.xf :as xf]))

(deftest test-slide
  (is (= [[1 2] [2 3] [3 4] [4 5]]
         (into [] (xf/slide 2) [1 2 3 4 5]))
      "test that windows of size n slide over the input in order")

  (testing "a trailing run shorter than n is dropped rather than emitted"
    (is (= [[1 2]]
           (into [] (xf/slide 3) [1 2])))
    
    (is (= [[1 2 3] [2 3 4]]
           (into [] (xf/slide 3) [1 2 3 4]))))

  (is (= [[1 2 3]]
         (into [] (xf/slide 3) [1 2 3]))
      "test that a single window is emitted when input is exactly n long")

  (is (= [[1] [2] [3]]
         (into [] (xf/slide 1) [1 2 3]))
      "test that n=1 windows are singleton vectors in input order")

  (is (= [3 5 7]
         (into [] (comp (xf/slide 2) (map #(apply + %))) [1 2 3 4]))
      "test that it composes with another xform")

  (is (= [] (into [] (xf/slide 2) []))
      "test that it works over an empty input"))
