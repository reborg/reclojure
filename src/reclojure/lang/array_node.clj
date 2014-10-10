(ns reclojure.lang.array-node
  (:require [reclojure.lang.protocols.node :as node]))

(defrecord ArrayNode [edit count array])

(defn ->hash-assoc [])

(extend ArrayNode
  node/Node
  {:assoc #'->hash-assoc})
