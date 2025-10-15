(ns fogus.adverbs-test
  (:require [clojure.test :refer :all]
            [fogus.adverbs :as a]))


(deftest test-kwargify
  (let [f (a/kwargify (fn [m] (set (keys m))))]
    (is (= #{:a :b} (f :a 1 :b 2)))))

(deftest test-cps->fn
  (testing "straight-line test"
    (let [f (a/cps->fn #(do (Thread/sleep 300)
                            (%1 (apply + %&)))
                       #(identity %))]
      (is (= 6 (f 1 2 3)))))
  (testing "exception prop"
    (let [f (a/cps->fn #(/ 10 %) #(identity %))]
      (is (thrown? Throwable (f 0))))))
