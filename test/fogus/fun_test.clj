(ns fogus.fun-test
  (:require [clojure.test :refer :all]
            [fogus.fun :as fun]))

(deftest iota-test
  (is (= (fun/iota identity inc #(< % 10) 1)
         [1 2 3 4 5 6 7 8 9])))

(deftest !pred-test
  (let [validate-long     (fun/!pred nil? parse-long #(ex-info (format "Expected double, got: %s" %) {}))
        validate-not-even (fun/!pred even? parse-long #(ex-info (format "Expected odd number, got: %s" %) {}))]
    (is (validate-long "42"))
    (is (thrown? Exception (validate-long "one")))
    (is (validate-not-even "3"))
    (is (thrown? Exception (validate-not-even "2")))))


