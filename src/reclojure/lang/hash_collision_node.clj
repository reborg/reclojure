(ns reclojure.lang.hash-collision-node
  (:require [reclojure.lang.protocols.node :as node]))

(defrecord HashCollisionNode [edit hash count array])

(defn ->hash-assoc [node shift hash key val addedLeaf])

(extend HashCollisionNode
  node/Node
  {:assoc #'->hash-assoc})
