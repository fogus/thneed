(ns fogus.shell
  (:require clojure.java.shell)
  (:import
    [java.io InputStream StringWriter File]
    [java.lang ProcessBuilder ProcessBuilder$Redirect]
    [java.util List]))

(def parse-args @#'clojure.java.shell/parse-args)

(defn go [& args]
  (let [[cmd opts] (parse-args args)
        pb (ProcessBuilder. ^List cmd)]
    (.redirectOutput pb ProcessBuilder$Redirect/DISCARD)
    (.redirectError pb ProcessBuilder$Redirect/DISCARD)
    (let [proc (.start pb)]
      {:exit (.waitFor proc)})))



(comment

  (go "touch" "my-test-file")
  (go "bash" "-c" "sleep 1; z=0; while [ $z -lt 25000 ] ; do echo $z; z=$((z+1)) ; done")
  (go "bash" "-c" "sleep 1; z=0; while [ $z -lt 25000 ] ; do echo $z; z=$((z+1)) ; done")
  
)
