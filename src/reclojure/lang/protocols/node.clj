(ns reclojure.lang.protocols.node
  (:refer-clojure :exclude [assoc find]))

(defprotocol Node
  (find [node shift hash key]
        [node shift hash key notFound])
  (nodeSeq [node])
  (assoc [node shift hash key val addedLeaf]
         [node edit shift hash key val addedLeaf])
  (without [node shift hash key]
    [node edit shift hash key removedLeaf])
  (kvreduce [node f init])
  (fold [node combinef reducef fjtask fjfork fjjoin]))
