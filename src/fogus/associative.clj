;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.associative)

(defn dissoc-in
  "Dissociates a value in a nested associative structure asc, where path is a
  sequence of keys. If the path does not resolve to a valid associative mapping
  then this function is a noop."
  [asc path]
  (cond
    (zero? (count path)) asc
    (= 1 (count path)) (dissoc asc (first path))
    :else
    (let [[k & ks] path]
      (if (contains? asc k)
        (update asc k #(dissoc-in % ks))
        asc))))


