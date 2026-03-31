;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.config
  "A dead simple config reader for Clojure supporting multiple formats and extensions."
  (:require [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as string]))

(defmulti -reader class)
(defmulti -read-format (fn [frmt & _] frmt))

(defmethod -reader java.lang.String [s]
  (some->> (string/trim s) io/resource))

(defmethod -reader :default [r] r)

(defmethod -read-format :default
  [_ contents]
  (edn/read-string {:readers *data-readers*} contents))

(defmethod -read-format :edn
  [_ contents]
  (-read-format :default contents))

(defmethod -read-format :properties
  [_ contents]
  (let [props (java.util.Properties.)]
    (.load props (java.io.StringReader. contents))
    (into {} props)))

(defn read-config
  "Reads a configuration file from `from` (a resource path string or reader)
  and parses it according to `format`.

  Supported formats:
    :edn        - EDN file, returns a Clojure data structure
    :properties - Java .properties file, returns a map of string keys to string values

  New formats can be supported by extending the `-read-format` multimethod,
  which dispatches on the format keyword. For example, to add JSON support
  using a library like `cheshire`:

      (defmethod fogus.config/-read-format :json
        [_ contents]
        (cheshire.core/parse-string contents true))

  With that in place, callers can use the `:json` tag to read those JSON config files.

  Unrecognized format keywords fall through to the `:default` EDN parsing."
  [from _ format]
  (with-open [rdr (-reader from)]
    (if-let [contents (slurp rdr)]
      (-read-format format contents)
      (throw (ex-info
              (str "Failed to read a configuration file from " from)
              {:from from})))))

