;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.fun-test
  (:require [clojure.test :refer :all]
            [fogus.fun :as fun]))

(deftest foldr-test
  (testing "subtraction shows left vs right associativity"
    (is (= -6 (reduce - 0 [1 2 3])))
    (is (= 2 (fun/foldr - 0 [1 2 3]))))
  
  (testing "division shows associativity difference"
    (is (= 1/24 (reduce / 1 [2 3 4])))
    (is (= 8/3 (fun/foldr / 1 [2 3 4]))))
  
  (testing "cons builds lists naturally with foldr"
    (is (= '(1 2 3 4) (fun/foldr cons '() [1 2 3 4])))
    (is (= [4 3 2 1] (fun/foldr #(conj %2 %1) [] [1 2 3 4]))))
  
  (testing "empty collection returns accumulator"
    (is (= 42 (fun/foldr + 42 [])))
    (is (= [] (fun/foldr cons [] []))))
  
  (testing "single element collection"
    (is (= 11 (fun/foldr + 10 [1])))
    (is (= '(1) (fun/foldr cons '() [1]))))
  
  (testing "associative operations match reduce"
    (is (= (reduce + 0 [1 2 3 4 5])
           (fun/foldr + 0 [1 2 3 4 5])))
    (is (= (reduce * 1 [2 3 4])
           (fun/foldr * 1 [2 3 4]))))
  
  (testing "building nested structure right-to-left"
    (is (= {:value 1 :rest {:value 2 :rest {:value 3 :rest nil}}}
           (fun/foldr (fn [x acc] {:value x :rest acc}) 
                  nil 
                  [1 2 3]))))
  
  (testing "foldr processes rightmost elements first"
    (let [call-order (atom [])]
      (fun/foldr (fn [x acc] 
               (swap! call-order conj x)
               (cons x acc))
             []
             [1 2 3])
      (is (= [3 2 1] @call-order))))
  
  (testing "foldr vs reduce with side effects"
    (let [reduce-order (atom [])
          foldr-order (atom [])]
      (reduce (fn [acc x] (swap! reduce-order conj x) (+ acc x)) 0 [1 2 3])
      (fun/foldr (fn [x acc] (swap! foldr-order conj x) (+ x acc)) 0 [1 2 3])
      (is (= [1 2 3] @reduce-order))
      (is (= [3 2 1] @foldr-order)))))

(deftest iota-test
  (is (= [1 2 3 4 5 6 7 8 9]
         (fun/iota identity inc #(< % 10) 1)))
  
  (is (= [1 2 4 8 16 32]
         (fun/iota identity #(* 2 %) #(< % 64) 1))))

(deftest upto-test
  (is (= [1 2 3 4 5] (fun/upto 6 1)))
  (is (= [] (fun/upto 5 5)))
  (is (= [10] (fun/upto 11 10)))
  (is (= [-5 -4 -3 -2 -1 0] (fun/upto 1 -5))))

(deftest downto-test
  (is (= [10 9 8 7 6] (fun/downto 5 10)))
  (is (= [] (fun/downto 5 5)))
  (is (= [5] (fun/downto 4 5)))
  (is (= [3 2 1 0 -1 -2] (fun/downto -3 3))))

(deftest to-test
  (is (= [1 2 3 4 5] (fun/to 1 6)))
  (is (= [10 9 8 7 6] (fun/to 10 5)))
  (is (= [] (fun/to 5 5)))
  (is (= [-3 -2 -1 0 1 2] (fun/to -3 3)))
  (is (= [3 2 1 0 -1 -2] (fun/to 3 -3)))
  (is (= [0] (fun/to 0 1)))
  (is (= [1] (fun/to 1 0))))

(deftest !pred-test
  (testing "base cases"
    (let [validate-long     (fun/!pred nil? parse-long #(ex-info (format "Expected double, got: %s" %) {}))
          validate-not-even (fun/!pred even? parse-long #(ex-info (format "Expected odd number, got: %s" %) {}))]
      (is (validate-long "42"))
      (is (thrown? Exception (validate-long "one")))
      (is (validate-not-even "3"))
      (is (thrown? Exception (validate-not-even "2"))))

    (let [no-nil (fun/!pred nil? identity)]
      (is (= 42 (no-nil 42)))
      (is (thrown? Exception (no-nil nil))))

    (let [safe-str (fun/!pred #(> (count %) 50) str "Result too long")]
      (is (= "hello" (safe-str "hel" "lo")))
      (is (thrown? Exception (safe-str (apply str (repeat 60 "x")))))))

  (testing "ex-dispatches"
    (let [positive-only (fun/!pred #(<= % 0) identity "Number must be positive")]
      (is (= 5 (positive-only 5)))
      (is (thrown-with-msg? Exception #"Number must be positive" (positive-only -3))))
    
    (let [non-empty (fun/!pred empty? vec {:ex-info/msg "Collection cannot be empty" 
                                           :error-code 400})]
      (is (= [1 2 3] (non-empty [1 2 3])))
      (is (thrown? Exception (non-empty []))))
    
    (let [in-range (fun/!pred #(or (< % 0) (> % 100)) 
                              identity 
                              (fn [x] (ex-info (format "Value %d out of range [0,100]" x) 
                                               {:value x :min 0 :max 100})))]
      (is (= 50 (in-range 50)))
      (is (thrown-with-msg? Exception #"Value 150 out of range" (in-range 150)))))

  (testing "corner cases"
    (let [always-ok (fun/!pred (constantly false) inc)]
      (is (= 6 (always-ok 5)))
      (is (= 1 (always-ok 0))))
    
    (let [always-throw (fun/!pred (constantly true) identity "Always fails")]
      (is (thrown? Exception (always-throw "anything")))))

  (testing "original CP use cases"
    (let [non-negative? (fun/!pred neg? identity 
                                   #(ex-info "Contract violation: result must be non-negative" 
                                             {:value %}))
          non-empty?    (fun/!pred empty? identity
                                   #(ex-info "Contract violation: result cannot be empty"
                                             {:value %}))
          sorted?       (fun/!pred #(not (apply <= %)) identity
                                   #(ex-info "Contract violation: result must be sorted"
                                             {:value %}))
          withdraw (fn [balance amount]
                     {:post [(non-negative? %)]}
                     (- balance amount))

          filter-evens (fn [coll]
                         {:post [(non-empty? %)]}
                         (filter even? coll))

          merge-sorted (fn [xs ys]
                         {:post [(sorted? %)]}
                         (sort (concat xs ys)))

          broken-merge (fn [xs ys]
                         {:post [(sorted? %)]}
                         (concat xs ys))]
      
      (is (= 50 (withdraw 100 50)))
      (is (thrown-with-msg? Exception #"Contract violation: result must be non-negative"
                            (withdraw 100 150)))
      
      (is (= [2 4] (filter-evens [1 2 3 4])))
      (is (thrown-with-msg? Exception #"Contract violation: result cannot be empty"
                            (filter-evens [1 3 5])))
      
      (is (= [1 2 3 4 5 6] (merge-sorted [1 3 5] [2 4 6])))
      
      (is (thrown-with-msg? Exception #"Contract violation: result must be sorted"
                            (broken-merge [3 1] [2])))))

  (testing "bounds checking"
    (let [valid-account? (fun/!pred 
                          #(or (neg? (:balance %)) 
                               (empty? (:transactions %)))
                          identity
                          (fn [acct] 
                            (ex-info "Contract violation: invalid account state"
                                     {:reason (cond
                                                (neg? (:balance acct)) "negative balance"
                                                (empty? (:transactions acct)) "no transactions")
                                      :account acct})))

          create-account (fn [initial-deposit]
                           {:post [(valid-account? %)]}
                           {:balance initial-deposit
                            :transactions [{:type :deposit :amount initial-deposit}]})

          transfer (fn [account amount]
                     {:post [(valid-account? %)]}
                     (-> account
                         (update :balance - amount)
                         (update :transactions conj {:type :withdrawal :amount amount})))]

      (is (valid-account? (create-account 100)))
      (is (valid-account? (transfer (create-account 100) 50)))
      
      (is (try (transfer (create-account 100) 150)
               (catch clojure.lang.ExceptionInfo ei
                 (= "negative balance" (-> ei ex-data :reason)))))

      (is (try (valid-account? {:balance 100 :transactions []})
               (catch clojure.lang.ExceptionInfo ei
                 (= "no transactions" (-> ei ex-data :reason))))))))
