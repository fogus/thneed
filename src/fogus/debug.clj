(ns fogus.debug
  "Debug utilities."
  (:require fogus.maps))

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
     :os/patch-level       (System/getProperty "sun.os.patch.level")
     :file/encoding        (System/getProperty "file.encoding")
     :java/version         (System/getProperty "java.version")
     :java/runtime.name    (System/getProperty "java.runtime.name")
     :java/runtime.version (System/getProperty "java.runtime.version")
     :java/home            (System/getProperty "java.home")
     :java/class.version   (System/getProperty "java.class.version")
     :java/graphics.env    (System/getProperty "java.awt.graphicsenv")
     :directory/pwd        (.getAbsolutePath (java.io.File. ".")))))

