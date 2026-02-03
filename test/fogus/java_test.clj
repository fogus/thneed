;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.java-test
  (:require [clojure.test :refer :all]
            [fogus.java :as java]))

(deftest array-dim-test
  (is (= 0 (java/array-dim nil)))
  (is (= 1 (java/array-dim (class (make-array String 0)))))
  (is (= 1 (java/array-dim (class (make-array String 2)))))
  (is (= 2 (java/array-dim (class (make-array String 2 2)))))
  (is (= 1 (java/array-dim (make-array String 2))))
  (is (= 2 (java/array-dim (make-array String 2 2))))
  (is (= 14 (java/array-dim (class (make-array String 2 2 2 2 2 2 2 2 2 2 2 2 2 2)))))
  (is (= 1 (java/array-dim String/1)))
  (is (= 9 (java/array-dim String/9))))
