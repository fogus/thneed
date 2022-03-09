(ns fogus.rdr-test
  (:require [clojure.test :refer :all]
            [fogus.rdr :as rdr])
  (:import java.util.Locale
           java.sql.Timestamp
           java.util.Date))

(set! *warn-on-reflection* true)

(deftest static-methods
  (is (= [1 1.2 3 4.25]
         (map (rdr/make-fn java.lang.Math abs) [-1 -1.2 (int -3) (float -4.25)])))
  
  ;; NOTE: this needs an import at compile time
  (is (= [3 6 9]
         (map (rdr/make-fn java.util.Collections max) [[1 2 3] [4 5 6] [7 8 9]])))

  (is (= 1
         ((rdr/make-fn java.util.Collections max) [1 2 3] >)))
  
  (is (= 0.10000000000000002
         ((rdr/make-fn java.lang.Math nextAfter) 0.1 1.1)))

  ;; float truncates
  (is (= (float 0.10000001)
         ((rdr/make-fn java.lang.Math nextAfter) (float 0.1) 1.1)))

  (is (thrown-with-msg?
       Exception #"class clojure.lang.Keyword cannot be cast to class java.lang.Number"
       ((rdr/make-fn java.lang.Math nextAfter) (float 0.1) :bad-arg)))
  )

(deftest instance-methods
  (is (= ["A" "BC"]
         (map (rdr/make-fn java.lang.String toUpperCase) ["a" "bc"])))

  (is (= ["A" "BC"]
         (map #((rdr/make-fn java.lang.String toUpperCase) % Locale/US) ["a" "bc"])))

  (testing "methods that have a bridge method on Object"
    (is (= 0
           ((rdr/make-fn java.sql.Timestamp compareTo) (Timestamp. 0) (Timestamp. 0))))
    (is (= 0
           ((rdr/make-fn java.sql.Timestamp compareTo) (Timestamp. 0) (-> (Timestamp. 0) .getTime (Date.)))))))

(deftest varargs
  (is (= "we are 138"
         ((rdr/make-fn java.lang.String format) "we are %d" (to-array [138]))))

  (is (= "we are 138"
         ((rdr/make-fn java.lang.String format) Locale/US "we are %d" (to-array [138])))))

(deftest constructors
  (is (= 2022)
      (let [^java.util.Date date ((rdr/make-fn java.util.Date java.util.Date))]
        (.getYear date))))
