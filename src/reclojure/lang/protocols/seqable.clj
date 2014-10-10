(ns reclojure.lang.protocols.seqable
  (:refer-clojure :exclude [seq]))

(defprotocol Seqable
  (seq [coll]))
