(ns fogus.uuid
  (:refer-clojure :exclude (uuid?)))

(set! *warn-on-reflection* true)

(defn rand-uuid
  {:doc "Returns a pseudo-randomly generated java.util.UUID instance (i.e. type 4).

   See: https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html#randomUUID--"
   :inline (fn [] `(java.util.UUID/randomUUID))
   :inline-arities #{0}
   :added "1.11"}
  (^java.util.UUID [] (java.util.UUID/randomUUID)))

(defn parse-uuid
  {:doc "Takes a string representing a UUID and returns a java.util.UUID instance
   for it. Returns nil if the string cannot be parsed.

   See: https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html#toString--"
   :added "1.11"}
  (^java.util.UUID [^String s]
   (try
     (java.util.UUID/fromString s)
     (catch IllegalArgumentException _))))

(defn uuid?
  {:doc "Return true if x is a java.util.UUID"
   :inline (fn [x] `(instance? java.util.UUID ~x))
   :inline-arities #{1}
   :added "1.9"}
  [x] (instance? java.util.UUID x))

(rand-uuid)
(parse-uuid "3db4db90-37c7-42aa-941c-f621d0cec91f")
(parse-uuid "")
(-> (rand-uuid) str parse-uuid)


(comment

  (-> clojure.lang.Keyword
      class
      java.io.ObjectStreamClass/lookup
      .getSerialVersionUID)

  *clojure-version*

  ;; 1.10.3
  ;;=> 3206093459760846163 

)
