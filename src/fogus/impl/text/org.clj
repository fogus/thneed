(ns ^:no-doc fogus.impl.text.org
  (:require [clojure.string :as str]
            [fogus.impl.text.common :as common]))

(set! *warn-on-reflection* true)

(def ^:private heading-regex   #"^(\*+) (.*)")
(def ^:private link-regex      #"\[\[(.*?)\]\[(.*?)\]\]")
(def ^:private bold-regex      #"\*([^<*]+)\*")
(def ^:private italics-regex #"(?:^|(?<=[\s(\[{]))/([^</>]+)/(?=[\s)\]}.,:;!?']|$)")

(defn- apply-inline-rules
  [segment]
  (-> segment
      (common/inline-rule bold-regex
       #(str "<strong>" (str/trim %) "</strong>"))

      (common/inline-rule italics-regex
       #(str "<em>" (str/trim %) "</em>"))

      (common/inline-rule
       link-regex
       #(str "<a href=\"" (str/trim %1) "\">" (str/trim %2) "</a>"))))

(defn classify-line [line]
  (common/classify-line line
    heading-regex (fn [[_ stars text]] [:heading [(count stars) text]])))

(defn org!
  ([state] (common/complete! state apply-inline-rules))

  ([{:keys [^StringBuilder sb in-list? paragraph-buffer] :as state}
    [line-type line-content]]
   (case line-type
     :heading
     (let [[level text] line-content
           new-para     (common/commit-paragraph! paragraph-buffer sb apply-inline-rules)
           new-list?    (common/maybe-close-list! in-list? sb)
           h-level      (min 6 level)]
       (when (pos? (.length sb)) (.append sb "\n"))
       (.append sb (str "<h" h-level ">" (apply-inline-rules text) "</h" h-level ">"))
       (assoc state
              :in-list?         new-list?
              :paragraph-buffer new-para))

     :list-item (common/step-list-item state line-content apply-inline-rules)
     :empty     (common/step-empty     state apply-inline-rules)
     :text      (common/step-text      state line-content))))
