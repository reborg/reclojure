(ns reclojure.lang.array-node
  (:require [reclojure.lang.protocols.node :as node]
            [reclojure.lang.util :as u]))

(u/defmutable ArrayNode [edit count array])

(defn ->hash-assoc []
  (throw (RuntimeException. "implement me")))

(extend ArrayNode
  node/Node
  {:assoc #'->hash-assoc})
