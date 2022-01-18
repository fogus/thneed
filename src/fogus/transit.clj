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

(deftype MapWriteHandler [^:volatile-mutable tag-provider]
  TagProviderAware
  (^void setTagProvider [_ ^TagProvider tp]
   (set! tag-provider tp))

  WriteHandler
  (tag [_ m]
    (if (stringable-keys? tag-provider m)
      "map"
      "cmap"))
  (rep [_ m]
    (if (stringable-keys? tag-provider m)
      (.entrySet ^java.util.Map m)
      (TransitFactory/taggedValue "array" (seq m)))) ;; always needs seq of Map.Entry?
  (stringRep [_ _] nil)
  (getVerboseHandler [_] nil))

(def ^WriteHandlers$MapWriteHandler mwh (->> java.util.Map (get whm)))
(.tag mwh {[] 1})

(class mwh)

(def ^WriteHandler mwh' (->MapWriteHandler whm))
(.setTagProvider ^TagProviderAware mwh' whm)

(.tag mwh' {'a 1 :b 2})
(.tag mwh' {(with-meta 'a {:foo 42}) 1 :b 2})
(.tag mwh' {[] 1 :b 2})

(.rep mwh' {'a 1 :b 2})
(.rep mwh' {[] 1 :b 2})

(def writer-overrides {java.util.Map (->MapWriteHandler whm)})
(def overrides (transit/write-handler-map writer-overrides))

(defn private-field [obj fn-name-string]
  (let [m (.. obj getClass (getDeclaredField fn-name-string))]
    (. m (setAccessible true))
    (. m (get obj))))

;(-> overrides transit/handler-map (private-field "handlers") (.put java.util.Map (->MapWriteHandler (WriteHandlers$MapWriteHandler.) whm)))
;(-> overrides transit/handler-map (private-field "handlers") (.get java.util.Map))

(def out (ByteArrayOutputStream. 2000))
(def w (transit/writer out :json {:transform transit/write-meta, :handlers overrides}))

;(transit/write w {'key (with-meta 'val {:foo 'bar})})
out

(transit/write w {(with-meta 'key {:foo 'bar}) 'val})
out

;(transit/write w {(with-meta 'key {:foo 'bar}) 'val, [] 42})


