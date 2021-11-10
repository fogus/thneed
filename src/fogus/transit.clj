(ns fogus.transit
  (:require [cognitect.transit :as transit])
  (:import [java.io File ByteArrayInputStream ByteArrayOutputStream OutputStreamWriter]
           [com.cognitect.transit WriteHandler ReadHandler ArrayReadHandler MapReadHandler
            ArrayReader TransitFactory TransitFactory$Format MapReader]
           [com.cognitect.transit.impl TagProvider TagProviderAware WriteHandlerMap WriteHandlers WriteHandlers$MapWriteHandler]
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

(def whm (TransitFactory/writeHandlerMap transit/default-write-handlers))

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

(deftype MapWriteHandler [^WriteHandlers$MapWriteHandler core-impl ^:volatile-mutable tag-provider]
  TagProviderAware
  (^void setTagProvider [_ ^TagProvider tp]
   (.setTagProvider core-impl tp)
   (set! tag-provider tp))

  WriteHandler
  (tag [_ m]
    (if (stringable-keys? tag-provider m)
      "map"
      "cmap"))
  (rep [_ o]
    (.rep core-impl o))
  (stringRep [_ _] nil)
  (getVerboseHandler [_] nil))

(def mwh (->> java.util.Map (get whm)))
(.tag mwh {[] 1})

(def ^WriteHandler mwh' (->MapWriteHandler (WriteHandlers$MapWriteHandler.) whm))
(.setTagProvider mwh' whm)

(.tag mwh' {'a 1 :b 2})
(.tag mwh' {(with-meta 'a {:foo 42}) 1 :b 2})
(.tag mwh' {[] 1 :b 2})

(.rep mwh' {'a 1 :b 2})
(.rep mwh' {[] 1 :b 2})

