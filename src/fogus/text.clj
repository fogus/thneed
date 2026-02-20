;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.text
  (:require [fogus.impl.text.md :as mdown]))

(set! *warn-on-reflection* true)

(defn md
  "Takes a string ``s`` containing Markdown-like text and returns a string of the
  representational HTML.

  Supports a minimal and highly constrained [Markdown](https://www.markdownguide.org/)
  dialect designed specifically for small source code documentation comment blocks.
  It supports two block-level elements:

  - CODE BLOCKS: Signaled by four or more leading spaces. The parser preserves all
    internal formatting and newlines. Content is wrapped in pre and code tags with
    HTML entities escaped.
  - LISTS: Created using a hyphen followed by a space at the start of a line. Lists
    are terminated by an empty line or the start of a new block-level element. You
    may use spacing to align continuation text of a given list item, and all will
    be combined into continuous text. Lists are rendered as ul and li tags.
    As an example, the two list items in this section are part of a single larger
    list, but the two list items in the next section constitute two separate lists.

  Two inline elements are supported:

  - INLINE CODE FRAGMENTS: Literal text wrapped by matching double-backticks. These
    can be nested within links and lists. Content is wrapped in code tags with HTML
    entities escaped and whitespace trimmed.

  - LINKS: Standard Markdown format with square brackets for anchor text and
    parentheses for URLs. Link text may contain plain text or nested inline code
    fragments, but no other markup. Links are converted to HTML anchor tags.

  All other text is wrapped in paragraph tags. HTML special characters are escaped
  throughout.

  The function also accepts a ``:language \"LANG\"`` keyword arg that, if present will
  add a ``class`` element to all code blocks in the form:

      <code class=\"language-LANG\">

  Additionally, the ``:prefix-space N`` keyword arg takes a number used to 'ignore'
  the leading N spaces of all text lines. The default value is 2."
  [s & {:as opts}]
  (let [{:keys [lines] :as init-state} (mdown/init-md-state s opts)]
    (transduce (map mdown/classify-line) mdown/md! init-state lines)))
