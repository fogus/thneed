;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.maps-test
  (:require [clojure.test :refer :all]
            [fogus.maps :as m]))

(deftest manip-test
  (is (= (m/keys-apply {:a 1, :b 2, :c 3} [:a :c] inc)
         {:a 2, :c 4}))

  (is (= (m/manip-map {:a 1, :b 2, :c 3} [:a :c] inc)
         {:c 4, :b 2, :a 2}))

  (is (= (m/manip-keys {:a 1, :b 2} [:a] str)
         {:b 2, ":a" 1})))

(deftest assoc-iff-test
  (is (= (m/assoc-iff {} :a 1, :b 2, :c nil)
         {:a 1, :b 2})))

(deftest deep-merge-test
  (is (= (m/deep-merge {:a 1} {:b 2} {:b 3 :c {:d 1}} {:c {:d 2}})
         {:a 1, :b 3, :c {:d 2}})))
