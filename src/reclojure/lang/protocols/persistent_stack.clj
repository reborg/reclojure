(ns reclojure.lang.protocols.persistent-stack
  (:refer-clojure :exclude [peek pop]))

(defprotocol PersistentStack
  (peek [this])
  (pop [this]))
