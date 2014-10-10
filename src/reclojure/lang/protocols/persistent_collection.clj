(ns reclojure.lang.protocols.persistent-collection
  (:refer-clojure :exclude [seq cons empty count]))

(defprotocol PersistentCollection
  (cons [this o])
  (empty [this])
  (equiv [this o])
  (count [this])
  (seq [this]))
