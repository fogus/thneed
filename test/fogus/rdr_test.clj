(ns fogus.rdr-test
  (:require [clojure.test :refer :all]
            [fogus.rdr :as rdr])
  (:import java.util.Collections))

(deftest static-methods
  (is (= [1 1.2 3 4.25]
         (map (rdr/make-fn Math abs) [-1 -1.2 (int -3) (float -4.25)])))

  (is (thrown? IllegalArgumentException
               ((rdr/make-fn Math abs) :bad-arg)))
  
  ;; NOTE: this needs an import at compile time
  (is (= [3 6 9]
         (map (rdr/make-fn Collections max) [[1 2 3] [4 5 6] [7 8 9]])))

  (is (= 0.10000000000000002
         ((rdr/make-fn Math nextAfter) 0.1 1.1)))
  )

(deftest instance-methods
  (is (= ["A" "BC"]
         (map (rdr/make-fn String toUpperCase) ["a" "bc"])))
  )

(deftest varargs
  (is (= "we are 138"
         ((rdr/make-fn String format) "we are %d" (to-array [(int 138)])))))
