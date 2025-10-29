;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.meta
  "Utilities dealing with metadata.")

;; TODO combinator for below

(defn massoc [o k v]
  (with-meta o (assoc (meta o) k v)))

(defn mupdate [o k f x]
  (with-meta o (update (meta o) k f x)))

(comment
  (-> {:z 1}
      (massoc :a 42)
      meta)
)
