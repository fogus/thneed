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

(comment
  (set! *warn-on-reflection* true)
  (clojure.core/fn compareTo10601 
    ([self10602 G__10603]
     (clojure.core/cond 
                        (clojure.core/instance? java.sql.Timestamp G__10603) (. ^java.sql.Timestamp self10602 compareTo ^java.sql.Timestamp G__10603) 
                        (clojure.core/instance? java.util.Date G__10603)     (. ^java.sql.Timestamp self10602 compareTo ^java.util.Date G__10603) 
                        :default (. ^java.sql.Timestamp self10602 compareTo ^java.lang.Object G__10603))))

)
