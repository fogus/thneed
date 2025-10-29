;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

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
