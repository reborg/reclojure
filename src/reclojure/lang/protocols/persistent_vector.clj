(ns reclojure.lang.protocols.persistent-vector
  (:refer-clojure :exclude [nth seq cons empty rseq count assoc peek pop]))

(defprotocol PersistentVector
  (length [this])
  (assocN [this i val])
  (cons [this o])
  (containsKey [this key])
  (entryAt [this key])
  (assoc [this key val])
  (peek [this])
  (pop [this])
  (empty [this])
  (equiv [this o])
  (count [this])
  (seq [this])
  (rseq [this])
  (nth
    [this i]
    [this i notFound]))
