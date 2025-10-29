;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.reg-test
  (:require [clojure.test :refer :all]
            [fogus.reg :as reg]))

(deftest register-basic-test
  (testing "register adds an item to an empty registry"
    (let [r (reg/register {} ::name string?)]
      (is (= string? (get r ::name)))))
  
  (testing "register adds multiple items"
    (let [r (-> {}
                (reg/register ::name string?)
                (reg/register ::age pos-int?))]
      (is (= string? (get r ::name)))
      (is (= pos-int? (get r ::age)))))
  
  (testing "register overwrites existing items"
    (let [r1 (reg/register {} ::name string?)
          r2 (reg/register r1 ::name pos-int?)]
      (is (= pos-int? (get r2 ::name)))))
  
  (testing "register with nil removes the entry"
    (let [r1 (reg/register {} ::name string?)
          r2 (reg/register r1 ::name nil)]
      (is (contains? r1 ::name))
      (is (not (contains? r2 ::name)))))

  (testing "register works with keywords"
    (let [r (reg/register {} ::keyword-key "value")]
      (is (= "value" (get r ::keyword-key)))))
  
  (testing "register works with symbols"
    (let [r (reg/register {} 'my.ns/symbol-key "value")]
      (is (= "value" (get r 'my.ns/symbol-key)))))
  
  (testing "register works with mixed key types"
    (let [r (-> {}
                (reg/register ::keyword-key "kw-val")
                (reg/register 'my.ns/symbol-key "sym-val"))]
      (is (= "kw-val" (get r ::keyword-key)))
      (is (= "sym-val" (get r 'my.ns/symbol-key)))))

  (testing "register stores any value type"
    (let [r (-> {}
                (reg/register ::fn-val string?)
                (reg/register ::map-val {:a 1})
                (reg/register ::vec-val [1 2 3])
                (reg/register ::str-val "string")
                (reg/register ::num-val 42)
                (reg/register ::nil-val nil))]
      (is (fn? (get r ::fn-val)))
      (is (= {:a 1} (get r ::map-val)))
      (is (= [1 2 3] (get r ::vec-val)))
      (is (= "string" (get r ::str-val)))
      (is (= 42 (get r ::num-val)))
      (is (not (contains? r ::nil-val))))))

(deftest lookup-direct-test
  (testing "lookup returns value for direct key"
    (let [r (reg/register {} ::name string?)]
      (is (= string? (reg/lookup r ::name)))))
  
  (testing "lookup returns nil for missing key"
    (is (nil? (reg/lookup {} ::missing)))))

(deftest lookup-alias-chains-test
  (testing "lookup follows single alias"
    (let [r (-> {}
                (reg/register ::target "value")
                (reg/register ::alias ::target))]
      (is (= "value" (reg/lookup r ::alias)))))
  
  (testing "lookup follows multiple aliases"
    (let [r (-> {}
                (reg/register ::end "final-value")
                (reg/register ::middle ::end)
                (reg/register ::start ::middle))]
      (is (= "final-value" (reg/lookup r ::start)))
      (is (= "final-value" (reg/lookup r ::middle)))
      (is (= "final-value" (reg/lookup r ::end)))))
  
  (testing "lookup follows long alias chains"
    (let [r (-> {}
                (reg/register ::a "value")
                (reg/register ::b ::a)
                (reg/register ::c ::b)
                (reg/register ::d ::c)
                (reg/register ::e ::d))]
      (is (= "value" (reg/lookup r ::e))))))

(deftest lookup-cycle-detection-test
  (testing "lookup detects direct self-reference"
    (let [r (reg/register {} ::self ::self)]
      (is (nil? (reg/lookup r ::self)))))
  
  (testing "lookup detects two-element cycle"
    (let [r (-> {}
                (reg/register ::a ::b)
                (reg/register ::b ::a))]
      (is (nil? (reg/lookup r ::a)))
      (is (nil? (reg/lookup r ::b)))))
  
  (testing "lookup detects longer cycles"
    (let [r (-> {}
                (reg/register ::a ::b)
                (reg/register ::b ::c)
                (reg/register ::c ::a))]
      (is (nil? (reg/lookup r ::a)))
      (is (nil? (reg/lookup r ::b)))
      (is (nil? (reg/lookup r ::c))))))

(deftest lookup!-success-test
  (testing "lookup! returns value when found"
    (let [r (reg/register {} ::name "value")]
      (is (= "value" (reg/lookup! r ::name)))))
  
  (testing "lookup! follows aliases"
    (let [r (-> {}
                (reg/register ::target "value")
                (reg/register ::alias ::target))]
      (is (= "value" (reg/lookup! r ::alias))))))

(deftest lookup!-failure-test
  (testing "lookup! throws for missing key"
    (is (thrown? 
         clojure.lang.ExceptionInfo
         (reg/lookup! {} ::missing))))
  
  (testing "lookup! throws for unresolvable alias"
    (let [r (reg/register {} ::alias ::missing-target)]
      (is (thrown? clojure.lang.ExceptionInfo
                   (reg/lookup! r ::alias)))))
  
  (testing "lookup! throws for cycle"
    (let [r (-> {}
                (reg/register ::a ::b)
                (reg/register ::b ::a))]
      (is (thrown? clojure.lang.ExceptionInfo
                   (reg/lookup! r ::a)))))
  
  (testing "lookup! exception contains key info"
    (try
      (reg/lookup! {} ::missing)
      (is false "Should have thrown")
      (catch clojure.lang.ExceptionInfo e
        (is (= ::reg/unresolved (:type (ex-data e))))
        (is (= ::missing (:key (ex-data e))))))))

(deftest alias-chain-basic-test
  (testing "alias-chain for direct value"
    (let [r (reg/register {} ::name "value")]
      (is (= [::name "value"] (reg/alias-chain r ::name)))))
  
  (testing "alias-chain for single alias"
    (let [r (-> {}
                (reg/register ::target "value")
                (reg/register ::alias ::target))]
      (is (= [::alias ::target "value"] (reg/alias-chain r ::alias)))))
  
  (testing "alias-chain for multiple aliases"
    (let [r (-> {}
                (reg/register ::end "value")
                (reg/register ::middle ::end)
                (reg/register ::start ::middle))]
      (is (= [::start ::middle ::end "value"] (reg/alias-chain r ::start)))))
  
  (testing "alias-chain returns nil for missing key"
    (is (nil? (reg/alias-chain {} ::missing)))))

(deftest alias-chain-cycle-test
  (testing "alias-chain detects self-reference"
    (let [r (reg/register {} ::self ::self)]
      (is (reg/cyclic? (reg/alias-chain r ::self)))))
  
  (testing "alias-chain detects two-element cycle"
    (let [r (-> {}
                (reg/register ::a ::b)
                (reg/register ::b ::a))]
      (is (reg/cyclic? (reg/alias-chain r ::a)))))
  
  (testing "alias-chain detects longer cycle"
    (let [r (-> {}
                (reg/register ::a ::b)
                (reg/register ::b ::c)
                (reg/register ::c ::a))]
      (is (reg/cyclic? (reg/alias-chain r ::a))))))

(deftest alias-basic-test
  (testing "alias creates an alias"
    (let [r1 (reg/register {} ::target "value")
          r2 (reg/alias r1 ::alias ::target)]
      (is (= "value" (reg/lookup r2 ::alias)))))
  
  (testing "alias can chain"
    (let [r (-> {}
                (reg/register ::end "value")
                (reg/alias ::middle ::end)
                (reg/alias ::start ::middle))]
      (is (= "value" (reg/lookup r ::start))))))

(deftest alias-validation-test
  (testing "alias requires identifier as target"
    (is (thrown? AssertionError
                 (reg/alias {} ::alias "not-an-identifier")))
    (is (thrown? AssertionError
                 (reg/alias {} ::alias 42)))))

(deftest stateful-atom-usage-test
  (testing "using swap! with register"
    (let [reg (atom {})]
      (swap! reg reg/register ::name "value")
      (is (= "value" (get @reg ::name)))))
  
  (testing "using swap! for multiple updates"
    (let [reg (atom {})]
      (swap! reg reg/register ::name "val1")
      (swap! reg reg/register ::age "val2")
      (is (= "val1" (get @reg ::name)))
      (is (= "val2" (get @reg ::age)))))
  
  (testing "using swap! to remove entries"
    (let [reg (atom (reg/register {} ::name "value"))]
      (swap! reg reg/register ::name nil)
      (is (not (contains? @reg ::name)))))
  
  (testing "using reset! to clear registry"
    (let [reg (atom (-> {}
                        (reg/register ::name "val1")
                        (reg/register ::age "val2")))]
      (reset! reg {})
      (is (empty? @reg)))))

(deftest edge-cases-test
  (testing "registering with keyword as value"
    (let [r (reg/register {} ::key ::keyword-value)]
      (is (= ::keyword-value (get r ::key)))))
  
  (testing "lookup stops at non-identifier"
    (let [r (-> {}
                (reg/register ::num 42)
                (reg/register ::alias ::num))]
      (is (= 42 (reg/lookup r ::alias)))))
  
  (testing "empty registry behaves correctly"
    (is (nil? (reg/lookup {} ::anything)))
    (is (thrown? clojure.lang.ExceptionInfo (reg/lookup! {} ::anything)))
    (is (nil? (reg/alias-chain {} ::anything)))))

(deftest concurrent-updates-test
  (testing "concurrent swaps maintain consistency"
    (let [reg (atom {})
          futures (doall
                   (for [i (range 100)]
                     (future
                       (swap! reg reg/register 
                              (keyword "test" (str i))
                              i))))]
      ;; wait for all futures
      (doseq [f futures] @f)

      (is (= 100 (count @reg)))

      (is (every? (fn [[k v]]
                    (= v (Integer/parseInt (name k) 10)))
                  (filter (fn [[k _]] (= (namespace k) "test"))
                          @reg))))))


