;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.text.org-test
  (:require [clojure.test :refer :all]
            [clojure.string :as string]
            [fogus.impl.text.md :as mdown]
            [fogus.text :as text]))

(deftest heading-test
  (testing "should parse a single h1 heading"
    (let [markup   "* Top Level"
          expected "<h1>Top Level</h1>"]
      (is (= expected (text/org markup)))))

  (testing "should parse h2 through h6 headings"
    (doseq [[markup expected]
            [["** Section"      "<h2>Section</h2>"]
             ["*** Subsection"  "<h3>Subsection</h3>"]
             ["**** Deep"       "<h4>Deep</h4>"]
             ["***** Deeper"    "<h5>Deeper</h5>"]
             ["****** Deepest"  "<h6>Deepest</h6>"]]]
      (is (= expected (text/org markup)))))

  (testing "should cap heading level at h6 for seven or more asterisks"
    (let [markup   "******* Overflowed"
          expected "<h6>Overflowed</h6>"]
      (is (= expected (text/org markup)))))

  (testing "should parse multiple headings separated by blank lines"
    (let [markup   "* Alpha\n\n** Beta"
          expected "<h1>Alpha</h1>\n<h2>Beta</h2>"]
      (is (= expected (text/org markup)))))

  (testing "should parse a heading immediately followed by another heading"
    (let [markup   "* Alpha\n** Beta"
          expected "<h1>Alpha</h1>\n<h2>Beta</h2>"]
      (is (= expected (text/org markup)))))

  (testing "should apply inline bold and italic rules inside headings"
    (let [markup   "* A *bold* and /italic/ title"
          expected "<h1>A <strong>bold</strong> and <em>italic</em> title</h1>"]
      (is (= expected (text/org markup))))))

(deftest list-test
  (testing "should parse a single list item"
    (let [markup   "- Only item"
          expected "<ul><li>Only item</li></ul>"]
      (is (= expected (text/org markup)))))

  (testing "should parse multiple list items into a single ul"
    (let [markup   "- Item 1\n- Item 2\n- Item 3"
          expected "<ul><li>Item 1</li>\n<li>Item 2</li>\n<li>Item 3</li></ul>"]
      (is (= expected (text/org markup)))))

  (testing "should close a list and open a paragraph on a blank line"
    (let [markup   "- Item A\n\nFinal paragraph."
          expected "<ul><li>Item A</li></ul>\n<p>Final paragraph.</p>"]
      (is (= expected (text/org markup)))))

  (testing "should close a list when a heading is encountered"
    (let [markup   "- Item A\n* New Section"
          expected "<ul><li>Item A</li></ul>\n<h1>New Section</h1>"]
      (is (= expected (text/org markup)))))

  (testing "should parse two separate lists divided by a blank line"
    (let [markup   "- First\n\n- Second"
          expected "<ul><li>First</li></ul>\n<ul><li>Second</li></ul>"]
      (is (= expected (text/org markup)))))

  (testing "should apply inline bold and italic rules inside list items"
    (let [markup   "- A *bold* point and a /note/"
          expected "<ul><li>A <strong>bold</strong> point and a <em>note</em></li></ul>"]
      (is (= expected (text/org markup))))))

(deftest org-list-continuation-test
  (testing "continuation lines are folded into the preceding list item"
    (let [markup "- first item\n    continued here\n- second item"]
      (is (= "<ul><li>first item continued here</li>\n<li>second item</li></ul>"
             (text/org markup :prefix-spaces 0))))

    (testing "multiple continuation lines are all folded into one item"
      (let [markup "- first item\n    line two\n    line three\n- second item"]
        (is (= "<ul><li>first item line two line three</li>\n<li>second item</li></ul>"
               (text/org markup :prefix-spaces 0)))))

    (testing "a blank line terminates the list, not a continuation"
      (let [markup "- first item\n\nnot a continuation"]
        (is (= "<ul><li>first item</li></ul>\n<p>not a continuation</p>"
               (text/org markup :prefix-spaces 0)))))))

(deftest paragraph-test
  (testing "should wrap a single line of plain text in a paragraph"
    (let [markup   "Hello, world."
          expected "<p>Hello, world.</p>"]
      (is (= expected (text/org markup)))))

  (testing "should join consecutive text lines into a single paragraph"
    (let [markup   "Line one.\nLine two.\nLine three."
          expected "<p>Line one.\nLine two.\nLine three.</p>"]
      (is (= expected (text/org markup)))))

  (testing "should separate two paragraphs divided by a blank line"
    (let [markup   "First paragraph.\n\nSecond paragraph."
          expected "<p>First paragraph.</p>\n<p>Second paragraph.</p>"]
      (is (= expected (text/org markup))))))

(deftest inline-bold-test
  (testing "should parse bold text in a paragraph"
    (let [markup   "This is *bold* text."
          expected "<p>This is <strong>bold</strong> text.</p>"]
      (is (= expected (text/org markup)))))

  (testing "should parse multiple bold spans on one line"
    (let [markup   "*alpha* and *beta*"
          expected "<p><strong>alpha</strong> and <strong>beta</strong></p>"]
      (is (= expected (text/org markup)))))

  (testing "should parse bold text inside a list item"
    (let [markup   "- A *bold* point."
          expected "<ul><li>A <strong>bold</strong> point.</li></ul>"]
      (is (= expected (text/org markup)))))

  (testing "should parse bold text inside a heading"
    (let [markup   "* The *important* point"
          expected "<h1>The <strong>important</strong> point</h1>"]
      (is (= expected (text/org markup))))))

(deftest inline-italic-test
  (testing "should parse italic text in a paragraph"
    (let [markup   "This is /italic/ text."
          expected "<p>This is <em>italic</em> text.</p>"]
      (is (= expected (text/org markup)))))

  (testing "should parse multiple italic spans on one line"
    (let [markup   "/alpha/ and /beta/"
          expected "<p><em>alpha</em> and <em>beta</em></p>"]
      (is (= expected (text/org markup)))))

  (testing "should parse italic text inside a list item"
    (let [markup   "- A /noted/ item."
          expected "<ul><li>A <em>noted</em> item.</li></ul>"]
      (is (= expected (text/org markup)))))

  (testing "should parse italic text inside a heading"
    (let [markup   "* The /secondary/ point"
          expected "<h1>The <em>secondary</em> point</h1>"]
      (is (= expected (text/org markup))))))

(deftest inline-bold-and-italic-test
  (testing "should parse bold and italic on the same line"
    (let [markup   "A *bold* word and an /italic/ word."
          expected "<p>A <strong>bold</strong> word and an <em>italic</em> word.</p>"]
      (is (= expected (text/org markup)))))

  (testing "should parse bold and italic inside a list item"
    (let [markup   "- *Bold* start, /italic/ end."
          expected "<ul><li><strong>Bold</strong> start, <em>italic</em> end.</li></ul>"]
      (is (= expected (text/org markup))))))

(deftest inline-link-test
  (testing "should parse an org-mode double-bracket link in a paragraph"
    (let [markup   "See [[https://docs.com][the documentation]]."
          expected "<p>See <a href=\"https://docs.com\">the documentation</a>.</p>"]
      (is (= expected (text/org markup)))))

  (testing "should preserve forward-slashes in link URLs without encoding"
    (let [markup   "Visit [[https://example.com/path/to][this page]]."
          expected "<p>Visit <a href=\"https://example.com/path/to\">this page</a>.</p>"]
      (is (= expected (text/org markup)))))

  (testing "should parse a link inside a list item"
    (let [markup   "- See [[https://api.com][the API]]."
          expected "<ul><li>See <a href=\"https://api.com\">the API</a>.</li></ul>"]
      (is (= expected (text/org markup)))))

  (testing "should parse multiple links on one line"
    (let [markup   "[[https://a.com][A]] and [[https://b.com][B]]."
          expected "<p><a href=\"https://a.com\">A</a> and <a href=\"https://b.com\">B</a>.</p>"]
      (is (= expected (text/org markup))))))

(deftest mixed-blocks-test
  (testing "should parse a heading followed by a paragraph"
    (let [markup   "* Introduction\nSome introductory text."
          expected "<h1>Introduction</h1>\n<p>Some introductory text.</p>"]
      (is (= expected (text/org markup)))))

  (testing "should parse a heading, paragraph, and list in sequence"
    (let [markup   "* Title\nSome text.\n\n- Item 1\n- Item 2"
          expected "<h1>Title</h1>\n<p>Some text.</p>\n<ul><li>Item 1</li>\n<li>Item 2</li></ul>"]
      (is (= expected (text/org markup)))))

  (testing "should handle leading and trailing blank lines gracefully"
    (let [markup   "\n\n* Heading\n\n"
          expected "<h1>Heading</h1>"]
      (is (= expected (text/org markup))))))

(def org0 #(text/org % :prefix-spaces 0))

(deftest prefix-spaces-test
  (testing "default of 2 strips two leading spaces from each line"
    (let [markup   "  * Heading\n  Some text."
          expected "<h1>Heading</h1>\n<p>Some text.</p>"]
      (is (= expected (text/org markup)))))

  (testing "setting of 0 leaves lines unmodified"
    (let [markup   "* Heading\nSome text."
          expected "<h1>Heading</h1>\n<p>Some text.</p>"]
      (is (= expected (org0 markup)))))

  (testing "setting of 4 strips four leading spaces from each line"
    (let [markup   "    * Heading\n    - Item 1\n    - Item 2"
          expected "<h1>Heading</h1>\n<ul><li>Item 1</li>\n<li>Item 2</li></ul>"]
      (is (= expected (text/org markup :prefix-spaces 4)))))

  (testing "lines with fewer spaces than prefix-spaces are left unmodified"
    (let [markup   "* Short\n- Indented item"
          expected "<h1>Short</h1>\n<ul><li>Indented item</li></ul>"]
      (is (= expected (org0 markup))))))

(deftest docstring-test
  (let [ds     (-> #'fogus.text/org meta :doc)
        htmlds (text/org ds :prefix-spaces 2)]
    (is (string/includes? htmlds "<ul><li>HEADINGS:"))
    (is (string/includes? htmlds "<li>LISTS:"))
    (is (string/includes? htmlds "<ul><li>BOLD:"))
    (is (string/includes? htmlds "<ul><li>ITALIC:"))
    (is (string/includes? htmlds "<ul><li>LINKS:"))
    (is (string/includes? htmlds "<strong>word</strong>"))
    (is (string/includes? htmlds "<em>word</em>"))
    (is (string/includes? htmlds "<a href=\"https://orgmode.org\">Org-mode</a>"))
    (is (string/includes? htmlds "``:prefix-spaces N``"))
    (is (string/includes? htmlds "[fogus.me](https://www.fogus.me/code/dsmd/#org)"))))

(deftest composed-docstring-test
  (let [ds        (-> #'fogus.text/org meta :doc)
        org+md (-> ds
                   (text/org :prefix-spaces 2)
                   (text/md :prefix-spaces 2))
        md+org (-> ds
                   (text/md :prefix-spaces 2)
                   (text/org :prefix-spaces 2))]
    (is (string/includes? org+md "<a href=\"https://www.fogus.me/code/dsmd/#org\">fogus.me</a>"))
    (is (string/includes? org+md "<a href=\"https://orgmode.org\">Org-mode</a>"))
    (is (string/includes? org+md "<code>:prefix-spaces N</code>"))
    (is (string/includes? md+org "<a href=\"https://www.fogus.me/code/dsmd/#org\">fogus.me</a>"))
    (is (string/includes? md+org "<a href=\"https://orgmode.org\">Org-mode</a>"))
    (is (string/includes? md+org "<code>:prefix-spaces N</code>"))))
