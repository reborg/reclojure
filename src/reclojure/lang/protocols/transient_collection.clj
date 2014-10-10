(ns reclojure.lang.protocols.transient-collection
  (:refer-clojure :exclude [conj]))

(defprotocol TransientCollection
  (conj [coll val])
  (persistent [coll]))
