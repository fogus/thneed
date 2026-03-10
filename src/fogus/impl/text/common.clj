(ns ^:no-doc fogus.impl.text.common
  (:require [clojure.string :as str]))

(set! *warn-on-reflection* true)

(def list-item-regex #"^- (.*)")

(defn commit-paragraph!
  [paragraph-buffer ^StringBuilder sb inline-rules-fn]
  (when (seq paragraph-buffer)
    (when (pos? (.length sb))
      (.append sb "\n"))
    (.append sb "<p>")
    (.append sb (inline-rules-fn (str/join "\n" paragraph-buffer)))
    (.append sb "</p>"))
  [])

(defn maybe-close-list!
  [in-list? ^StringBuilder sb]
  (when in-list?
    (.append sb "</ul>"))
  false)

(defn step-list-item
  [{:keys [^StringBuilder sb in-list? paragraph-buffer] :as state}
   line-content
   inline-rules-fn]
  (let [new-para (commit-paragraph! paragraph-buffer sb inline-rules-fn)]
    (when-not in-list?
      (when (pos? (.length sb)) (.append sb "\n"))
      (.append sb "<ul>"))
    (when in-list? (.append sb "\n"))
    (.append sb "<li>")
    (.append sb (inline-rules-fn line-content))
    (.append sb "</li>")
    (assoc state
           :in-list?         true
           :paragraph-buffer new-para)))

(defn step-empty
  [{:keys [^StringBuilder sb in-list? paragraph-buffer] :as state}
   inline-rules-fn]
  (let [new-para  (commit-paragraph! paragraph-buffer sb inline-rules-fn)
        new-list? (maybe-close-list! in-list? sb)]
    (assoc state
           :in-list?         new-list?
           :paragraph-buffer new-para)))

(defn step-text
  [{:keys [^StringBuilder sb in-list? paragraph-buffer] :as state}
   line-content]
  (let [new-list? (maybe-close-list! in-list? sb)]
    (assoc state
           :in-list?         new-list?
           :paragraph-buffer (conj paragraph-buffer line-content))))

(defn complete!
  [{:keys [^StringBuilder sb in-list? paragraph-buffer]} inline-rules-fn]
  (maybe-close-list! in-list? sb)
  (commit-paragraph! paragraph-buffer sb inline-rules-fn)
  (str/trim (.toString sb)))

(defn- strip-prefix-lines
  [lines {:keys [prefix-spaces] :or {prefix-spaces 2}}]
  (let [prefix-patt (re-pattern (str "^ {" prefix-spaces "}"))]
    (mapv #(str/replace-first % prefix-patt "") lines)))

(defn strip-prefix-spaces
  [s opts]
  (strip-prefix-lines (str/split-lines s) opts))

(defn merge-list-continuations [lines]
  (loop [remaining lines
         result    []
         acc-li    nil]
    (if-let [line (first remaining)]
      (cond
        (re-matches list-item-regex line)
        (recur (rest remaining)
               (if acc-li (conj result acc-li) result)
               line)

        (and acc-li
             (not (str/blank? line))
             (re-matches #"^\s+.*" line))
        (recur (rest remaining)
               result
               (str acc-li " " (str/trim line)))

        :default
        (recur (rest remaining)
               (if acc-li (conj result acc-li line) (conj result line))
               nil))

      (if acc-li (conj result acc-li) result))))

(defn init-state
  ([s opts] (init-state s opts {}))
  ([s opts specific-state]
   (merge {:sb               (StringBuilder.)
           :in-list?         false
           :paragraph-buffer []
           :lines            (-> s
                                 str/split-lines
                                 (strip-prefix-lines opts)
                                 merge-list-continuations)}
          specific-state)))

(defn inline-rule [text regex f]
  (str/replace text regex (fn [[_ & matches]] (apply f matches))))

(defn classify-line [line & specific-checks]
  (or (some (fn [[regex f]]
              (when-let [m (re-matches regex line)]
                (f m)))
            (partition 2 specific-checks))
      (when-let [m (re-matches list-item-regex line)]
        [:list-item (second m)])
      (if (str/blank? line)
        [:empty nil]
        [:text line])))
