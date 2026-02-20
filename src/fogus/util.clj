;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.util)

(def html-escapes
  {\& "&amp;"
   \< "&lt;"
   \> "&gt;"})

(defn parse-kw-chain
  "Parses a string of concatenated keywords into a vector."
  [s]
  (mapv clojure.edn/read-string (re-seq #":?[^:]+" s)))

(defn parse-path
  "Parses a comma-delimited path string into a vector of path elements."
  [s]
  (let [normalized (clojure.string/replace s #"," " ")]
    (clojure.edn/read-string (str "[" normalized "]"))))
