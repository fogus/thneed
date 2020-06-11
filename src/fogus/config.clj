(ns fogus.config
  "A dead simple config reader for Clojure supporting multiple formats and locations."
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
  (-read-format _ contents))

(defn read-config
  "Usage:
      (config-reader \"/path/to/cfg.edn\" :as :edn)
  "
  [from _ format]
  (with-open [rdr (-reader from)]
    (if-let [contents (slurp rdr)]
      (-read-format format contents)
      (throw (ex-info
               (str "Failed to read a configuration file from " from)
               {:from from})))))
