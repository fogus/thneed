;;
;; Copyright (c) Michael Fogus. All rights reserved.
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 2.0 (https://opensource.org/license/epl-2-0)
;; which can be found in the file LICENSE at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.
;;

(ns fogus.java
  "Java host utilities."
  (:require fogus.maps))

(set! *warn-on-reflection* true)

(def virtual-threads-available?
  (try
    (Class/forName "java.lang.Thread$Builder$OfVirtual")
    true
    (catch ClassNotFoundException _
      false)))

(defn build-system-info-map
  ([] (build-system-info-map {}))
  ([base]
   (fogus.maps/assoc-iff base
     :user/name            (System/getProperty "user.name")
     :user/language        (System/getProperty "user.language")
     :user/country         (System/getProperty "user.country")
     :user/timezone        (System/getProperty "user.timezone")
     :os/arch              (System/getProperty "os.arch")
     :os/name              (System/getProperty "os.name")
     :os/version           (System/getProperty "os.version")
     :sun/os.patch.level   (System/getProperty "sun.os.patch.level")
     :file/encoding        (System/getProperty "file.encoding")
     :java/version         (System/getProperty "java.version")
     :java/runtime.name    (System/getProperty "java.runtime.name")
     :java/runtime.version (System/getProperty "java.runtime.version")
     :java/home            (System/getProperty "java.home")
     :java/class.version   (System/getProperty "java.class.version")
     :java/awt.graphicsenv (System/getProperty "java.awt.graphicsenv")
     :directory/pwd        (.getAbsolutePath (java.io.File. "."))
     :java/vthreads?       virtual-threads-available?)))

(defn array-dim
  "Expects an array instance, or an array class and returns the
  dimensionality of the argument."
  [mc]
  (let [ac (if (class? mc) mc (class mc))]
    (loop [dim 0, ^Class ct ac]
      (if (and ct (.isArray ct))
        (recur (inc dim) (.getComponentType ct))
        dim))))

