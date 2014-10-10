(ns reclojure.lang.protocols.lookup
  (:refer-clojure :exclude [conj]))

(defprotocol Lookup
  (valAt [this key]
         [this key notFound]))
