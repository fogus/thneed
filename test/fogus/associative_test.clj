;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.associative-test
  (:require [clojure.test :refer :all]
            [fogus.associative :as asc]))

(deftest dissoc-in-test
  (is (= {:a 1}                (asc/dissoc-in {:a 1 :b 2} [:b])))
  (is (= {:a 1 :b {:d 3}}      (asc/dissoc-in {:a 1 :b {:c 2 :d 3}} [:b :c])))
  (is (= {:a 1 :b {:c 2 :d 3}} (asc/dissoc-in {:a 1 :b {:c 2 :d 3}} [:b :z])))
  (is (= {:a 1 :b {}}          (asc/dissoc-in {:a 1 :b {:c 2}} [:b :c])))
  (is (= {:a 1}                (asc/dissoc-in {:a 1} [:b :c])))
  (is (thrown? Exception       (asc/dissoc-in {:a 1 :b :not-a-map} [:b :c])))
  (is (= {}                    (asc/dissoc-in {:a 1} [:a])))
  (is (= nil                   (asc/dissoc-in nil [:a])))
  (is (= {}                    (asc/dissoc-in {} [:a]))))
