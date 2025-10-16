(ns fogus.numbers-test
  (:require [clojure.test :refer :all]
            [fogus.numbers :as num]))

(deftest parse-roman-test
  (is (= 1994 (num/parse-roman "MCMXCIV")))
  (is (= 1666 (num/parse-roman "MDCLXVI")))
  (is (= 3 (num/parse-roman "III")))
  (is (= 1 (num/parse-roman "I")))
  (is (= 3999 (num/parse-roman "MMMCMXCIX"))))

(deftest num->roman-test
  (is (= "MCMXCIV" (num/num->roman 1994)))
  (is (= "MMXXV" (num/num->roman 2025)))
  (is (= "LVIII" (num/num->roman 58)))  
  (is (= "III" (num/num->roman 3)))
  (is (= "I" (num/num->roman 1)))
  (is (= "MMMCMXCIX" (num/num->roman 3999))))

(deftest round-trip-test
  (doseq [n (range 1 4000)]
    (is (= (-> n num/num->roman num/parse-roman num/num->roman)))))
