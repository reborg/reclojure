(ns reclojure.lang.hash-collision-node
  (:require [reclojure.lang.protocols.node :as node]
            [reclojure.lang.util :as u]))

(u/defmutable HashCollisionNode [edit hash count array])

(defn ->hash-assoc [node shift hash key val addedLeaf]
  (throw (RuntimeException. "implement me")))

(extend HashCollisionNode
  node/Node
  {:assoc #'->hash-assoc})
