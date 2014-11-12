(ns reclojure.lang.protocols.persistent-map
  (:require [reclojure.lang.protocols.persistent-vector :as pv]
            [clojure.tools.logging :as log])
  (:refer-clojure :exclude [cons empty seq get remove assoc count])
  (:import [clojure.lang RT]))

(defprotocol PersistentMap
  (assoc [this key val]) ;; common-provided
  (assocEx [this key val])
  (without [this key])
  (iterator [this])
  (entryAt [this key])
  (count [this])
  (cons [this o]) ;; specific
  (empty [this])
  (equiv [this o])
  (seq [this])
  (valAt [this key]
         [this key notFound])
  (size [this])
  (isEmpty [this])
  (containsKey [this key])
  (containsValue [this value])
  (get [this key])
  (put [this key value])
  (remove [this key])
  (putAll [this map])
  (clear [this])
  (keySet [this])
  (values [this])
  (entrySet [this])
  (equals [this o])
  (hashCode [this])
  (hasheq [this]))

(defn ^String ->toString [this]
  (RT/printString this))

(defn ->cons [pm o]
  (log/debug (format "->cons object %s" o))
  (cond
    (instance? java.util.Map$Entry o)
    (assoc pm (.getKey o) (.getValue o))
    (satisfies? pv/PersistentVector o)
    (if (= 2 (pv/count o))
      (pv/assoc pm (pv/nth o 0) (pv/nth o 1))
      (throw (IllegalArgumentException. "Vector arg to map conj must be a pair")))
    :else (throw (RuntimeException. "pm/->cons missing implementation"))))

(defn ->get [pm k]
  (valAt pm k))

(def PersistentMapImpl
  {:cons #'->cons
   :get #'->get})
