(ns ^:no-doc fogus.impl.text.md
  (:require [clojure.string :as str]
            [fogus.impl.text.common :as common]
            [fogus.util :as util]))

(set! *warn-on-reflection* true)

(def ^:private code-block-regex         #"(?m)(?:^    .*\n?)+")
(def ^:private code-prefix-regex        #"(?m)^    ")
(def ^:private code-placeholder-regex   #"^(__CODE_BLOCK_\d+__)$")
(def ^:private inline-code-regex        #"``([\s\S]*?)``")
(def ^:private link-regex               #"\[(.*?)\]\((.*?)\)")

(defn- apply-inline-rules
  [segment]
  (-> segment
      (common/inline-rule inline-code-regex
       #(str "<code>" (str/escape (str/trim %) util/html-escapes) "</code>"))

      (common/inline-rule link-regex
       #(str "<a href=\"" (str/trim %2) "\">" (str/trim %1) "</a>"))))

(defn- extract-code-blocks
  [lines {:keys [language]}]
  (let [code-class (if language (str " class=\"language-" language "\"") "")
        text       (str/join "\n" lines)]
    (loop [remaining    text
           intermediate []
           idx          0]
      (if-let [match (first (re-seq code-block-regex remaining))]
        (let [code-html   (str "<pre><code" code-class ">"
                               (str/escape (str/replace match code-prefix-regex "")
                                           util/html-escapes)
                               "</code></pre>")
              placeholder (str "__CODE_BLOCK_" idx "__")
              updated     (str/replace-first remaining code-block-regex placeholder)]
          (recur updated (conj intermediate code-html) (inc idx)))
        [intermediate (str/split-lines remaining)]))))

(defn init-md-state
  [s opts]
  (let [stripped             (common/strip-prefix-spaces s opts)
        merged               (common/merge-list-continuations stripped)
        [intermediate lines] (extract-code-blocks merged opts)]
    (common/init-state s opts {:intermediate intermediate
                               :lines        lines})))

(defn classify-line [line]
  (common/classify-line line
    code-placeholder-regex (fn [[_ placeholder]] [:code-block placeholder])))

(defn md!
  ([{:keys [^StringBuilder sb in-list? paragraph-buffer intermediate]}]
   (common/maybe-close-list! in-list? sb)
   (common/commit-paragraph! paragraph-buffer sb apply-inline-rules)
   (reduce-kv (fn [html idx code-block-html]
                (str/replace html
                             (re-pattern (str "__CODE_BLOCK_" idx "__"))
                             code-block-html))
              (.toString sb)
              intermediate))

  ([{:keys [^StringBuilder sb in-list? paragraph-buffer intermediate] :as state}
    [line-type line-content]]
   (case line-type
     :code-block
     (let [new-list? (common/maybe-close-list! in-list? sb)
           new-para   (common/commit-paragraph! paragraph-buffer sb apply-inline-rules)]
       (when (pos? (.length sb)) (.append sb "\n"))
       (.append sb line-content)
       (assoc state
              :in-list?         new-list?
              :paragraph-buffer new-para))

     :list-item (common/step-list-item state line-content apply-inline-rules)
     :empty     (common/step-empty     state apply-inline-rules)
     :text      (common/step-text      state line-content))))
