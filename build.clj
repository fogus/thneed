(ns build
  (:require [clojure.tools.build.api :as b]
            [deps-deploy.deps-deploy :as dd]
            [fogus.text :as text]))

(def lib 'me.fogus/thneed)
(def description "An eclectic set of Clojure utilities that I've found useful enough to keep around.")
(def version "1.1.7") ;; unreleased
(def class-dir "target/classes")
(def jar-file (format "target/%s.jar" (name lib)))

;; delay to defer side effects (artifact downloads)
(def basis (delay (b/create-basis{:project "deps.edn"})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn- pom-template [version]
  [[:description description]
   [:url "https://github.com/fogus/thneed"]
   [:licenses
    [:license
     [:name "Eclipse Public License 2.0"]
     [:url "https://www.eclipse.org/legal/epl-2.0/"]]]
   [:developers
    [:developer
     [:name "Fogus"]]]
   [:scm
    [:url "https://github.com/fogus/thneed"]
    [:connection "scm:git:https://github.com/fogus/thneed.git"]
    [:developerConnection "scm:git:ssh:git@github.com:fogus/thneed.git"]
    [:tag (str "v" version)]]])

(defn jar [_]
  (b/write-pom {:class-dir class-dir
                :lib lib
                :version version
                :basis (update-in @basis [:libs] dissoc 'org.clojure/clojure)
                :src-dirs ["src"]
                :src-pom "no-such-pom.xml" ;; prevent default pom copying
                :pom-data (pom-template version)})
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/jar {:class-dir class-dir
          :jar-file jar-file}))

(defn- jar-opts [opts]
  (println "\nVersion:" version)
  (assoc opts
         :lib lib   :version version
         :jar-file  jar-file
         :basis     (b/create-basis {})
         :class-dir class-dir
         :target    "target"
         :src-dirs  ["src"]
         :pom-data  (pom-template version)))

(defn docstring2html
  "Resolves the var named by `:var` in opts, extracts its docstring,
  converts it to HTML via fogus.text/md, and writes VAR.html where
  VAR is the name part of the var symbol. Overwrites any existing file.

  You can run via:

      clj -T:build docstring2html :var \"fogus.text/md\"
  "
  [{var-sym :var :as opts}]
  (let [resolved (requiring-resolve var-sym)
        doc      (:doc (meta resolved))
        html     (text/md doc)
        fname    (str (name var-sym) ".html")]
    (spit fname html)
    opts))

(defn deploy "Deploy the JAR to Clojars." [opts]
  (let [{:keys [jar-file] :as opts} (jar-opts opts)]
    (dd/deploy {:installer :remote
                :sign-releases? false
                :artifact (b/resolve-path jar-file)
                :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))}))
  opts)
