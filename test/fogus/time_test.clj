;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.time-test
  (:require [clojure.test :refer :all]
            [fogus.time :as time]
            [fogus.numbers :as num]))

(deftest scale-duration-tests
  (is (= 685714 (time/scale-duration 1200000 1.75)))
  (is (= 30000 (time/scale-duration 60000 2.0)))
  (is (= 120000 (time/scale-duration 60000 0.5)))
  (is (= 1000 (time/scale-duration 1000 1.0)))
  (is (= 333 (time/scale-duration 1000 3.0)))
  (is (= 334 (time/scale-duration 1001 3.0))))

(deftest ms->duration-tests
  (is (= {:minutes 20, :ms 0} (time/ms->duration 1200000)))
  (is (= {:days 1, :ms 0} (time/ms->duration 86400000)))
  (is (= {:ms 500} (time/ms->duration 500)))
  (is (= {:ms 0} (time/ms->duration 0)))
  (is (= {:hours 1, :minutes 1, :seconds 1, :ms 250} (time/ms->duration 3661250)))
  (is (= {:weeks 2, :minutes 16, :seconds 40, :ms 0} (time/ms->duration 1210600000))))

(deftest hybrid-tests
  (testing "Scaled time components"
    (let [t-scaled (time/scale-duration (* 4 60 60 1000) 0.75)]
      (is (= {:hours 5, :minutes 20, :ms 0}
             (time/ms->duration t-scaled))))
    
    (let [t-scaled (time/scale-duration 10000 1.33)]
      (is (= 7519 t-scaled))
      (is (= {:seconds 7, :ms 519}
             (time/ms->duration t-scaled))))))

(deftest roundtrip-tests
  (testing "Inverse conversions"
    (is (= (time/ms->seconds) (time/seconds->ms 1)))
    (is (= (time/ms->minutes) (time/minutes->ms 1)))
    (is (= (* 5 (time/ms->seconds)) (time/seconds->ms 5)))
    (is (= (* 3 (time/ms->minutes)) (time/minutes->ms 3)))
    (is (= (* 2 (time/ms->hours)) (time/hours->ms 2)))
    (is (= (* 1 (time/ms->days)) (time/days->ms 1)))
    (is (= (* 2 (time/ms->weeks)) (time/weeks->ms 2))))
  
  (testing "Roundtrip stability"
    (is (= {:seconds 5, :ms 0} (time/ms->duration (time/seconds->ms 5))))
    (is (= {:minutes 10, :ms 0} (time/ms->duration (time/minutes->ms 10))))
    (is (= {:hours 2, :ms 0} (time/ms->duration (time/hours->ms 2))))
    (is (= {:days 3, :ms 0} (time/ms->duration (time/days->ms 3))))
    (is (= {:weeks 1, :ms 0} (time/ms->duration (time/weeks->ms 1))))))

(deftest duration->ms-tests
  (is (= 3600000 (time/duration->ms {:hours 1})))
  (is (= 180000 (time/duration->ms {:minutes 3})))
  (is (= 250 (time/duration->ms {:ms 250})))
  (is (= 0 (time/duration->ms {})))
    
  (is (= (+ (time/weeks->ms 1) (time/days->ms 2) (time/hours->ms 3) (time/minutes->ms 4) (time/seconds->ms 5) 500)
         (time/duration->ms {:weeks 1, :days 2, :hours 3, :minutes 4, :seconds 5, :ms 500})))

  (testing "Roundtrips"
    (is (= 1992615500 (time/duration->ms (time/ms->duration 1992615500))))
    (is (= 0 (time/duration->ms (time/ms->duration 0))))
    (is (= (time/weeks->ms 1) (time/duration->ms (time/ms->duration (time/weeks->ms 1)))))
    (is (= (time/hours->ms 7) (time/duration->ms (time/ms->duration (time/hours->ms 7)))))))

(deftest derive-speed-tests
  (is (== 2.0 (time/derive-speed 10000 5000)))
  (is (== 0.5 (time/derive-speed 10000 20000)))
  (is (== 1.0 (time/derive-speed 15000 15000)))
    
  (let [original-time 1200000
        target-time (time/scale-duration original-time 1.75)]
    (is (num/approx= 0.00001 1.75 (time/derive-speed original-time target-time)))))
 
