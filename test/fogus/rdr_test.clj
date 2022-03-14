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

(deftest default-method
  (is (= "123"
         (with-out-str
           ((rdr/make-fn java.util.ArrayList forEach)
            (java.util.ArrayList. [1 2 3])
            (reify java.util.function.Consumer
              (accept [_ o] (print o))))))))

(deftest constructors
  (is (= 2022)
      (let [^java.util.Date date ((rdr/make-fn java.util.Date java.util.Date))]
        (.getYear date)))

  (is (= "")
      ((rdr/make-fn java.lang.String java.lang.String)))

  (is (= "foo")
      ((rdr/make-fn java.lang.String java.lang.String) "foo")))

(deftest primitve-arrays
  (is (= "foo")
      ((rdr/make-fn java.lang.String java.lang.String) (-> "foo" String. .getBytes))))

(deftest type-sorting
  (let [prims   '[(float n) (double n) (int n) (long n)]
        klasses ['java.util.List 'java.util.ArrayList 'java.util.Date 'java.sql.Timestamp nil]
        mixed '[(int n) (float n) (double n) (long n) (java.util.List n) (java.util.ArrayList n) (java.util.Date n) (java.sql.Timestamp n)]]
    
    (is (= '[(long n) (double n) (int n) (float n) (java.sql.Timestamp n) (java.util.ArrayList n) (java.util.Date n) (java.util.List n)]
           (sort-by identity @#'fogus.rdr/tcompare mixed)))

    (is (= '[(long n) (double n) (int n) (float n)]
           (sort-by identity @#'fogus.rdr/tcompare prims)))

    (is (= [nil 'java.sql.Timestamp 'java.util.ArrayList 'java.util.Date 'java.util.List]
           (sort-by identity @#'fogus.rdr/hierarchy-comparator klasses)))))
