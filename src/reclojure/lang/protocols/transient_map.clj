(ns reclojure.lang.protocols.transient-map
  (:require [reclojure.lang.protocols.persistent-vector :as pv]
            [clojure.tools.logging :refer [debug spy]])
  (:refer-clojure :exclude [conj assoc count]))

(defprotocol TransientMap
  (ensureEditable [this])
  (doAssoc [this key val])
  (doWithout [this key])
  (doValAt [this key notFound])
  (doCount [this])
  (doPersistent [this])
  (conj [this o])
  (invoke
    [this arg1]
    [this arg1 notFound])
  (assoc [this key val])
  (without [this key])
  (persistent [this])
  (valAt
    [this key]
    [this key notFound])
  (count [this]))

(defn ->persistent [this]
  (debug (format "->persistent on type '%s'" (type this)))
  (ensureEditable this)
  (doPersistent this))

(defn ->assoc [this key val]
  (debug (format "->assoc tm for new key '%s'" key))
  (ensureEditable this)
  (doAssoc this key val))

(defn ->conj [this o]
  (debug (format "->conj object '%s'" o))
  (ensureEditable this)
  (cond
    (instance? java.util.Map$Entry o)
    (->assoc this (.getKey o) (.getValue o))
    (satisfies? pv/PersistentVector o)
    (if (not (= 2 (pv/count o)))
      (throw (IllegalArgumentException. "Vector arg to map conj must be a pair"))
      (->assoc this (pv/nth o 0) (pv/nth o 1)))
    :else
    (map #(->assoc this (.getKey %) (.getValue %)) (seq o))))

(def TransientMapImpl
  {:conj #'->conj
   :assoc #'->assoc
   :persistent #'->persistent})
