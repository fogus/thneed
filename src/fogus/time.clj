;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.time
  "Utilities dealing with time and durations."
  (:require [clojure.math :as math]))

(defn scale-duration
  "Given a duration t in milliseconds and a scale factor speed, returns a scaled
  duration in milliseconds, rounded to the nearest millisecond per
  #'clojure.math/round."
  [t speed]
  (math/round (/ t speed)))

(defn ms->seconds 
  "Returns the number of whole seconds in a duration of milliseconds ms
  or the milliseconds in a second when no args provided."
  ([] 1000)
  ([ms] (quot ms 1000)))

(defn ms->minutes
  "Returns the number of whole minutes in a duration of milliseconds ms
  or the milliseconds in a minute when no args provided."
  ([] 60000)
  ([ms] (quot ms 60000)))

(defn ms->hours
  "Returns the number of whole hours in a duration of milliseconds ms
  or the milliseconds in an hour when no args provided."
  ([] 3600000)
  ([ms] (quot ms 3600000)))

(defn ms->days
  "Returns the number of whole days in a duration of milliseconds ms
  or the milliseconds in a day when no args provided."
  ([] 86400000)
  ([ms] (quot ms 86400000)))

(defn ms->weeks
  "Returns the number of whole weeks in a duration of milliseconds ms
  or the milliseconds in a week when no args provided."
  ([] 604800000)
  ([ms] (quot ms 604800000)))

(defn seconds->ms 
  "Returns the number of milliseconds in the given number of seconds."
  [seconds] 
  (* seconds (ms->seconds)))

(defn minutes->ms 
  "Returns the number of milliseconds in the given number of minutes."
  [minutes] 
  (* minutes (ms->minutes)))

(defn hours->ms 
  "Returns the number of milliseconds in the given number of hours."
  [hours] 
  (* hours (ms->hours)))

(defn days->ms 
  "Returns the number of milliseconds in the given number of days."
  [days] 
  (* days (ms->days)))

(defn weeks->ms 
  "Returns the number of milliseconds in the given number of weeks."
  [weeks] 
  (* weeks (ms->weeks)))

(defn ms->duration
  "Given a total number of milliseconds ms, returns a map of duration components
  :weeks, :days, :hours, :minutes, :seconds, and any remaining milliseconds :ms.
  This function is calendar agnostic and therefore can only supply its coarsest
  fidelity in weeks."
  [ms]
  (->> [[:weeks ms->weeks] [:days ms->days]
        [:hours ms->hours] [:minutes ms->minutes]
        [:seconds ms->seconds]]
       (reduce (fn [acc [unit-key unit-fn]]
                 (let [rem-ms (get acc :ms)]
                   (assoc acc unit-key (unit-fn rem-ms)
                          :ms (mod rem-ms (unit-fn)))))
               {:ms ms})
       (filter (fn [[k v]] (or (pos? v) (= k :ms)))) 
       (into {})))

(defn duration->ms
  "Converts a duration component map dur into a total number of milliseconds
  represented by that duration."
  [dur]
  (reduce (fn [total [unit-key inverse-fn]]
            (+ total (inverse-fn (get dur unit-key 0))))
          0
          [[:weeks weeks->ms] [:days days->ms]
           [:hours hours->ms] [:minutes minutes->ms]
           [:seconds seconds->ms] [:ms identity]]))
