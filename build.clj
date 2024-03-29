(ns build
  (:require [clojure.tools.build.api :as b]))

(def lib 'fogus/thneed)
(def version (format "0.1.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def basis (b/create-basis {:project "deps.edn"}))
(def jar-file (format "target/%s-%s.jar" (name lib) version))
(def src ["src"])

(defn clean [_]
  (b/delete {:path "target"}))

(defn compile-clj [_]
  (b/compile-clj {:src-dirs ["src"]
                  :class-dir class-dir
                  :basis basis}))

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis basis
                :src-dirs src})
  (b/copy-dir {:src-dirs src
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))
