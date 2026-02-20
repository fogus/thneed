(ns ^:no-doc fogus.impl.text.md
  (:require [clojure.string :as str]
            [fogus.util :as util]))

(set! *warn-on-reflection* true)

(def ^:private code-block-start-pattern "    ")
(def ^:private code-block-regex #"(?m)(?:^    .*\n?)+")
(def ^:private removal-regex #"(?m)^    ")
(def ^:private code-placeholder-regex #"^__CODE_BLOCK_\d+__$")
(def ^:private inline-code-regex #"``([\s\S]*?)``")
(def ^:private link-regex #"\[(.*?)\]\((.*?)\)")

(defn- apply-inline-rules
  [segment]
  (-> segment
      (str/replace inline-code-regex
                   (fn [[_ content]]
                     (str "<code>"
                          (str/escape (str/trim content) util/html-escapes)
                          "</code>")))
      (str/replace link-regex
                   (fn [[_ text url]]
                     (str "<a href=\"" (str/trim url) "\">" (str/trim text) "</a>")))))

(defn- commit-paragraph!
  [paragraph-buffer ^StringBuilder sb]
  (when (seq paragraph-buffer)
    (when (pos? (.length sb))
      (.append sb "\n"))
    (.append sb "<p>")
    (.append sb (apply-inline-rules (str/join "\n" paragraph-buffer)))
    (.append sb "</p>"))
  [])

(defn- maybe-close-list!
  "Close list if in-list? is true, return false. Otherwise return in-list? unchanged."
  [in-list? ^StringBuilder sb]
  (when in-list?
    (.append sb "</ul>"))
  false)

(defn- merge-list-continuations
  [lines]
  (loop [remaining lines
         result []
         acc-li nil]
    (if-let [line (first remaining)]
      (cond
        (str/starts-with? line "- ") (if acc-li
                                       (recur (rest remaining)
                                              (conj result acc-li)
                                              line)
                                       (recur (rest remaining)
                                              result
                                              line))
        (and acc-li
             (not (str/blank? line))
             (re-matches #"^\s+.*" line)) (recur (rest remaining)
                                                 result
                                                 (str acc-li
                                                      " "
                                                      (str/trim line)))
        :else
        (if acc-li
          (recur (rest remaining)
                 (conj result acc-li line)
                 nil)
          (recur (rest remaining)
                 (conj result line)
                 nil)))

      (if acc-li
        (conj result acc-li)
        result))))

(defn- prep-md-text
  "Extract code blocks, replace with placeholders, split text into lines, identify all list
  blocks and merge them into single lines, then return [intermediate lines]"
  [s {:keys [language prefix-spaces] :as opts :or {prefix-spaces 2}}]
  (let [prefix-patt (re-pattern (str "^ {" prefix-spaces "}"))
        dedented (str/join "\n" (map #(str/replace-first % prefix-patt "") (str/split-lines s)))
        merged (str/join "\n" (merge-list-continuations (str/split-lines dedented)))
        matches (re-seq code-block-regex merged)
        code-class (if language (str " class=\"language-" language "\"") "")]
    (loop [remaining merged
           intermediate []
           idx 0]
      (if-let [match (first (re-seq code-block-regex remaining))]
        (let [code-html (str "<pre><code" code-class ">"
                           (str/escape
                             (str/replace match removal-regex "")
                             util/html-escapes)
                           "</code></pre>")
              placeholder (str "__CODE_BLOCK_" idx "__")
              updated (str/replace-first remaining
                                         code-block-regex
                                         placeholder)]
          (recur updated
                 (conj intermediate code-html)
                 (inc idx)))
        [intermediate (str/split-lines remaining)]))))

(defn init-md-state
  [s opts]
  (let [[intermediate lines] (prep-md-text s opts)]
    {:sb (StringBuilder.)
     :in-list? false
     :paragraph-buffer []
     :intermediate intermediate
     :lines lines}))

(defn md!
  ([md-state]
   (let [{:keys [^StringBuilder sb in-list? paragraph-buffer intermediate]} md-state]
       (maybe-close-list! in-list? sb)
       (commit-paragraph! paragraph-buffer sb)

       (str/trim
        (reduce-kv
         (fn [html idx code-block-html]
           (str/replace html
                        (re-pattern (str "__CODE_BLOCK_" idx "__"))
                        code-block-html))
         (.toString sb)
         intermediate))))

    ([md-state [line-type line-content]]
     (let [{:keys [^StringBuilder sb in-list? paragraph-buffer intermediate]} md-state]
       (case line-type
         :code-block
         (let [in-list? (maybe-close-list! in-list? sb)
               new-buffer (commit-paragraph! paragraph-buffer sb)]
           (when (pos? (.length sb)) (.append sb "\n"))
           (.append sb line-content)
           {:sb sb :in-list? in-list? :paragraph-buffer new-buffer
            :intermediate intermediate})

         :list-item
         (do
           (commit-paragraph! paragraph-buffer sb)
           ;; Open list if needed
           (when-not in-list?
             (when (pos? (.length sb)) (.append sb "\n"))
             (.append sb "<ul>"))
           ;; Separate from previous item if in list
           (when in-list? (.append sb "\n"))
           ;; Add the complete list item (already merged by prep-md-text)
           (.append sb "<li>")
           (.append sb (apply-inline-rules line-content))
           (.append sb "</li>")
           {:sb sb :in-list? true :paragraph-buffer []
            :intermediate intermediate})

         :empty
         (let [new-buffer (commit-paragraph! paragraph-buffer sb)]
           {:sb sb :in-list? (maybe-close-list! in-list? sb)
            :paragraph-buffer new-buffer :intermediate intermediate})

         :text
         {:sb sb :in-list? (maybe-close-list! in-list? sb)
          :paragraph-buffer (conj paragraph-buffer line-content)
          :intermediate intermediate}))))

(defn classify-line
  [line]
  (cond
    (re-matches code-placeholder-regex line)
    [:code-block line]

    (str/starts-with? line "- ")
    [:list-item (subs line 2)]

    (str/blank? line)
    [:empty nil]

    :else
    [:text line]))
