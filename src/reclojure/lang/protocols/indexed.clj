(ns reclojure.lang.protocols.indexed
  (:refer-clojure :exclude [nth]))

(defprotocol Indexed
  (nth
    [this i]
    [this i notFound]))
