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
    (let [f (a/cps->fn #(%1 (/ 10 %2)) #(identity %))]
      (is (thrown? ArithmeticException (f 0))))))

(deftest nest-test
  (let [safe-div (a/nest / (fn [f n d]
                             (if (zero? d)
                               (reduced :undefined)
                               (f n d))))]
    (is (= 5 (safe-div 10 2)))
    (is (= :undefined (safe-div 10 0)))))

(deftest apply-layering-test
  (testing "empty layers returns base function unchanged"
    (is (= 42 (a/apply-layering [] identity [42])))
    (is (= "HELLO" (a/apply-layering [] clojure.string/upper-case ["hello"]))))
  
  (testing "that layers called in order"
    (let [call-log (atom [])
          layer1 (fn [f x]
                   (swap! call-log conj :layer1)
                   (if (= x :stop)
                     (reduced :stopped)
                     (f x)))
          
          layer2 (fn [f x]
                   (swap! call-log conj :layer2)
                   (f x))
          
          layer3 (fn [f x]
                   (swap! call-log conj :layer3)
                   (f x))]
      (testing "full-path apply-layering calls"
        (is (= 11
               [0]))
        (reset! call-log [])
        (is (= :ok (a/apply-layering [layer1 layer2 layer3] (constantly :ok) [:go])))
        (is (= [:layer3 :layer2 :layer1] @call-log)))))

  (testing "early termination in middle of 5-layer chain"
    (let [call-log (atom [])
          layer1 (fn [f x]
                   (swap! call-log conj :layer1)
                   (f (assoc x :layer1 true)))
          layer2 (fn [f x]
                   (swap! call-log conj :layer2)
                   (f (assoc x :layer2 true)))
          layer3 (fn [f x]
                   (swap! call-log conj :layer3)
                   (if (:should-terminate x)
                     (reduced {:status :terminated-at-layer3 :data x})
                     (f (assoc x :layer3 true))))
          layer4 (fn [f x]
                   (swap! call-log conj :layer4)
                   (f (assoc x :layer4 true)))
          layer5 (fn [f x]
                   (swap! call-log conj :layer5)
                   (f (assoc x :layer5 true)))]
      (reset! call-log [])
      (let [result (a/apply-layering [layer1 layer2 layer3 layer4 layer5]
                                     identity
                                     [{:value 42 :should-terminate true}])]
        (is (= {:status :terminated-at-layer3
                :data {:value 42
                       :should-terminate true
                       :layer5 true
                       :layer4 true}}
               result))
        (is (= [:layer5 :layer4 :layer3] @call-log))))))


