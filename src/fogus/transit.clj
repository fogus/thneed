(ns fogus.transit
  (:require [cognitect.transit :as transit])
  (:import [java.io File ByteArrayInputStream ByteArrayOutputStream OutputStreamWriter]
           [com.cognitect.transit WriteHandler ReadHandler ArrayReadHandler MapReadHandler
            ArrayReader TransitFactory TransitFactory$Format MapReader]
           [com.cognitect.transit.impl TagProvider TagProviderAware WriteHandlerMap]
           [com.cognitect.transit.SPI ReaderSPI]
           [java.io InputStream OutputStream]
           [java.util.function Function]))

(set! *warn-on-reflection* true)

(def out (ByteArrayOutputStream. 2000))
(def w (transit/writer out :json {:transform transit/write-meta}))

(transit/write w {'key (with-meta 'val {:foo 'bar})})
out

;;(transit/write w {(with-meta 'key {:foo 'bar}) 'val})
out

(defn nsed-name
  "Convert a keyword or symbol to a string in
   namespace/name format."
  [^clojure.lang.Named kw-or-sym]
  (if-let [ns (.getNamespace kw-or-sym)]
    (str ns "/" (.getName kw-or-sym))
    (.getName kw-or-sym)))

(defn- fn-or-val
  [f]
  (if (fn? f) f (constantly f)))

(def default-write-handlers
  "Returns a map of default WriteHandlers for
   Clojure types. Java types are handled
   by the default WriteHandlers provided by the
   transit-java library."
  {
   java.util.List
   (reify WriteHandler
     (tag [_ l] (if (seq? l) "list" "array"))
     (rep [_ l] (if (seq? l) (TransitFactory/taggedValue "array" l) l))
     (stringRep [_ _] nil)
     (getVerboseHandler [_] nil))

   clojure.lang.BigInt
   (reify WriteHandler
     (tag [_ _] "n")
     (rep [_ bi] (str (biginteger bi)))
     (stringRep [this bi] (.rep this bi))
     (getVerboseHandler [_] nil))

   clojure.lang.Keyword
   (reify WriteHandler
     (tag [_ _] ":")
     (rep [_ kw] (nsed-name kw))
     (stringRep [_ kw] (nsed-name kw))
     (getVerboseHandler [_] nil))

   clojure.lang.Ratio
   (reify WriteHandler
     (tag [_ _] "ratio")
     (rep [_ r] (TransitFactory/taggedValue "array" [(numerator r) (denominator r)]))
     (stringRep [_ _] nil)
     (getVerboseHandler [_] nil))

   clojure.lang.Symbol
   (reify WriteHandler
     (tag [_ _] "$")
     (rep [_ sym] (nsed-name sym))
     (stringRep [_ sym] (nsed-name sym))
     (getVerboseHandler [_] nil))

   cognitect.transit.WithMeta
   (reify WriteHandler
     (tag [_ _] "with-meta")
     (rep [_ o]
       (TransitFactory/taggedValue "array"
         [(.-value ^cognitect.transit.WithMeta o)
          (.-meta ^cognitect.transit.WithMeta o)]))
     (stringRep [_ _] nil)
     (getVerboseHandler [_] nil))})

(def whm (TransitFactory/writeHandlerMap default-write-handlers))

(defn- stringable-keys? [^TagProvider tp m]
  (every? (fn [k]
            (let [tag (.getTag tp k)]
              (cond (and tag (> (count tag) 1)) false
                    (and (nil? tag) (not (instance? String k))) false
                    (meta k) false
                    :default true)))
          (keys m)))

(stringable-keys? whm {:a 1 :b 2})
(stringable-keys? whm {'a 1 :b 2})
(stringable-keys? whm {"a" 1 :b 2 'c 3})
(stringable-keys? whm {[] 1 :b 2})
(stringable-keys? whm {(with-meta 'a {:foo 42}) 42})
