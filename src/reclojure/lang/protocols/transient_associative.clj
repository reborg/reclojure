(ns reclojure.lang.protocols.transient-associative
  (:refer-clojure :exclude [assoc]))

(defprotocol TransientAssociative
  (assoc [this key val]))
