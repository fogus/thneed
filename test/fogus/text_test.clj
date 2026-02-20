;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.text-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [fogus.text :as text]))

(def md0 (fn [s & {:as opts}] (text/md s (assoc opts :prefix-spaces 0))))

(deftest single-line-code-block-test
  (testing "should parse a single-line code block"
    (let [markup "    const x = 1;"
          expected "<pre><code>const x = 1;</code></pre>"]
      (is (= expected (md0 markup)))
      (is (= expected (text/md (str "  " markup)))))))

(deftest multi-line-code-block-test
  (testing "should parse a multi-line code block and preserve internal structure"
    (let [markup "
    function main() { // 4 leading spaces
        // comment 8 leading spaces
        return true; // 8 leading spaces
    } // 4 leading spaces
"
          expected-code-content "function main() { // 4 leading spaces
    // comment 8 leading spaces
    return true; // 8 leading spaces
} // 4 leading spaces"
          expected (str "<pre><code>" expected-code-content "</code></pre>")]
      (is (string/includes? (md0 markup) (string/trim expected))))))

(deftest code-blocks-treat-content-as-literal-test
  (testing "should treat content inside code blocks as literal (ignore links/fragments)"
    (let [markup "This is a paragraph.\n    [Link inside](url) and ``fragment`` ignored."
          expected "<p>This is a paragraph.</p>\n<pre><code>[Link inside](url) and ``fragment`` ignored.</code></pre>"]
      (is (= expected (md0 markup)))))

  (testing "should output the correct language class"
    (let [markup "This is a paragraph.\n    [Link inside](url) and ``fragment`` ignored."
          expected "<p>This is a paragraph.</p>\n<pre><code class=\"language-markdown\">[Link inside](url) and ``fragment`` ignored.</code></pre>"]
      (is (= expected (md0 markup :language "markdown"))))))

(deftest list-test
  (testing "should parse a simple list"
    (let [markup "- Item 1\n- Item 2"
          expected "<ul><li>Item 1</li>\n<li>Item 2</li></ul>"]
      (is (= expected (md0 markup)))))
  (testing "should break list with an empty line"
    (let [markup "- Item A\n\nFinal Paragraph."
          expected "<ul><li>Item A</li></ul>\n<p>Final Paragraph.</p>"]
      (is (= expected (md0 markup))))))

(deftest inline-rules-test
  (testing "should parse inline code fragments in a paragraph"
    (let [markup "Use ``API.call(x)`` to get the value."
          expected "<p>Use <code>API.call(x)</code> to get the value.</p>"]
      (is (= expected (md0 markup)))))
  (testing "should parse inline code fragments and retain embedded single backticks"
    (let [markup "Clojure unquote syntax quote is ```foo``."
          expected "<p>Clojure unquote syntax quote is <code>`foo</code>.</p>"]
      (is (= expected (md0 markup)))))
  (testing "should parse inline code fragments, retain embedded single backticks, and trim"
    (let [markup "Clojure syntax quote is `` `foo ``."
          expected "<p>Clojure syntax quote is <code>`foo</code>.</p>"]
      (is (= expected (md0 markup)))))
  (testing "should parse a link in a paragraph"
    (let [markup "See the [documentation](https://docs.com)."
          expected "<p>See the <a href=\"https://docs.com\">documentation</a>.</p>"]
      (is (= expected (md0 markup)))))
  (testing "should correctly parse an inline code fragment inside a list item"
    (let [markup "- The value is ``10``."
          expected "<ul><li>The value is <code>10</code>.</li></ul>"]
      (is (= expected (md0 markup)))))
  (testing "should correctly parse a link inside a list item"
    (let [markup "- See the [API](https://api.com)."
          expected "<ul><li>See the <a href=\"https://api.com\">API</a>.</li></ul>"]
      (is (= expected (md0 markup)))))
  (testing "should correctly parse link text containing an inline code fragment (Step 3 before Step 4)"
    (let [markup "The [``config.path`` variable](https://docs.com/path) is important."
          expected "<p>The <a href=\"https://docs.com/path\"><code>config.path</code> variable</a> is important.</p>"]
      (is (= expected (md0 markup)))))
  (testing "should ensure html entities are escaped in code fragments"
    (let [markup "Code fragment ``i < 1`` should be escaped."
          expected "<p>Code fragment <code>i &lt; 1</code> should be escaped.</p>"]
      (is (= expected (md0 markup))))))

(deftest docstring-test
  (let [ds (-> #'fogus.text/md meta :doc)
        htmlds (text/md ds)]
    (is (string/includes? htmlds "<code>s</code>"))
    (is (string/includes? htmlds "<code>:language</code>"))
    (is (string/includes? htmlds "<ul><li>Code Blocks:"))
    (is (string/includes? htmlds "\n<li>Lists:"))
    (is (string/includes? htmlds "two separate lists.</li></ul>"))
    (is (string/includes? htmlds "<ul><li>Inline Code Fragments:"))
    (is (string/includes? htmlds "whitespace trimmed.</li></ul>"))
    (is (string/includes? htmlds "<ul><li>Links:"))
    (is (string/includes? htmlds "HTML anchor tags.</li></ul>"))
    (is (string/includes? htmlds "&lt;code class=\"language-LANG\"&gt;"))
    (is (string/includes? htmlds "<p>The function also accepts a"))
    (is (string/includes? htmlds "<a href=\"https://www.markdownguide.org/\">Markdown</a>"))))

(deftest indented-lists-regression
  (testing "unindented lists correctly parse with 0 setting"
    (let [markup "
- This is a list item with
  an indented second line. That
  should pull all of the indented lines
  into a single li element."
          expected "<ul><li>This is a list item with an indented second line. That should pull all of the indented lines into a single li element.</li></ul>"]
      (is (= expected (text/md markup :prefix-spaces 0)))))
  (testing "unindented lists correctly parse with 2 setting"
    (let [markup "
  - This is a list item with
    an indented second line. That
    should pull all of the indented lines
    into a single li element."
          expected "<ul><li>This is a list item with an indented second line. That should pull all of the indented lines into a single li element.</li></ul>"]
      (is (= expected (text/md markup))))))
