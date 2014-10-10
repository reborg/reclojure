(ns reclojure.lang.protocols.associative
  (:refer-clojure :exclude [assoc]))

(defprotocol Associative
  (containsKey [this key])
  (entryAt [this key])
  (assoc [this key val]))
