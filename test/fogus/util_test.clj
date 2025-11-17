(ns fogus.util-test
  (:require [clojure.test :refer [deftest testing is]]
            [fogus.util :as util]))

(deftest parse-kw-chain-test
  (testing "parsing concatenated keywords"
    (is (= [:foo :bar :baz]
           (util/parse-kw-chain ":foo:bar:baz")))
    (is (= [:hello :world]
           (util/parse-kw-chain ":hello:world")))
    (is (= [:single]
           (util/parse-kw-chain ":single"))))
  
  (testing "parsing with leading symbol"
    (is (= '[foo :bar :baz]
           (util/parse-kw-chain "foo:bar:baz")))
    (is (= '[sym :key]
           (util/parse-kw-chain "sym:key"))))
  
  (testing "single elements"
    (is (= [:keyword]
           (util/parse-kw-chain ":keyword")))
    (is (= '[symbol]
           (util/parse-kw-chain "symbol"))))
  
  (testing "keywords and symbols with numbers"
    (is (= [:foo123 :bar456]
           (util/parse-kw-chain ":foo123:bar456")))
    (is (= '[sym99 :key88]
           (util/parse-kw-chain "sym99:key88"))))

  (testing "oddly formed keywords"
    (is (= [:a :b]
           (util/parse-kw-chain "::::::a:::::::::::b")))
    (is (= '[a :b]
           (util/parse-kw-chain "a:::::::::::b")))))

(deftest parse-path-test
  (testing "parsing mixed path elements"
    (is (= '[foo 42 :bar :baz qux 1]
           (util/parse-path "foo,42,:bar,:baz,qux,1")))
    (is (= [:key1 'sym2 123 :key3]
           (util/parse-path ":key1,sym2,123,:key3")))
    (is (= [100 :foo 'bar]
           (util/parse-path "100,:foo,bar"))))
  
  (testing "paths with only one type"
    (is (= [:foo :bar :baz]
           (util/parse-path ":foo,:bar,:baz")))
    (is (= '[foo bar baz]
           (util/parse-path "foo,bar,baz")))
    (is (= [1 2 3]
           (util/parse-path "1,2,3"))))
  
  (testing "single element paths"
    (is (= [:keyword]
           (util/parse-path ":keyword")))
    (is (= '[symbol]
           (util/parse-path "symbol")))
    (is (= [42]
           (util/parse-path "42"))))
  
  (testing "names with numbers"
    (is (= '[foo123 :bar2 99 qux456]
           (util/parse-path "foo123,:bar2,99,qux456")))
    (is (= '[:key100 sym200 300]
           (util/parse-path ":key100,sym200,300"))))
  
  (testing "empty string"
    (is (= []
           (util/parse-path ""))))
  
  (testing "complex real-world paths"
    (is (= '[user 123 :profile :settings 0]
           (util/parse-path "user,123,:profile,:settings,0")))
    (is (= [:data 'records 456 :name]
           (util/parse-path ":data,records,456,:name")))))
