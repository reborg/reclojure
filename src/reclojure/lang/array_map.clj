(ns reclojure.lang.array-map
  (:require [reclojure.lang.hash-map :as hm]
            [reclojure.lang.protocols.transient-map :as tm]
            [clojure.tools.logging :as log]
            [reclojure.lang.util :as u]
            [reclojure.lang.protocols.editable-collection :as ec]
            [reclojure.lang.protocols.persistent-map :as pm])
  (:import [clojure.lang Util]))

(def ^:dynamic *hashtable_threshold* 16)

(u/defmutable TransientArrayMap [len array owner])
(u/defmutable PersistentArrayMap [array meta])

(def EMPTY (PersistentArrayMap. nil nil))

(defn- init-array [^objects from]
  (let [l (max *hashtable_threshold* (alength from))
        to (make-array Object (alength from))]
    (System/arraycopy from 0 to 0 (alength from))
    to))

(defn create-transient [^objects a]
  (log/debug (format "create-transient for array '%s'" (java.util.Arrays/toString a)))
  (TransientArrayMap. (alength a) (init-array a) (Thread/currentThread)))

(defn doPersistent [tam]
  (log/debug (format "doPersistent on tam array %s " (java.util.Arrays/toString (.array tam))))
  (let [to (make-array Object (alength (.array tam)))]
    (do
      (System/arraycopy (.array tam) 0 to 0 (alength (.array tam)))
      (PersistentArrayMap. to meta))))

(defn as-transient []
  (create-transient (.toArray (java.util.Collections/emptyList))))

(defn ->tam-ensure-editable [tam]
  (log/debug (format "->tam-ensure-editable tam length '%s' array '%s' " (.len tam) (u/aprint tam)))
  (cond
    (identical? (.owner tam) (Thread/currentThread)) nil
    (not (nil? (.owner tam))) (throw (IllegalAccessError. "Transient used by non-owner thread"))
    :else (throw "tam->ensure-editable: no match")))

(defn persistent [this]
  (log/debug (format "persistent on '%s'" (type this)))
  (do
    (->tam-ensure-editable this)
    (doPersistent this)))

(defn create-persistent [m]
  (log/debug (format "create-persistent from map %s" m))
  (let [tam (as-transient)
        objects (.toArray (.entrySet m))]
    (persistent
      (do
        (for [i (range (alength objects))]
          (tm/assoc tam
                    (.getKey (aget objects i))
                    (.getValue (aget objects i))))
        tam))))

(defn tam-index-of [k tam]
  (loop [i 0]
    (cond (>= i (.len tam)) -1
          (= k (aget ^objects (.array tam) i)) i
          :else (recur (unchecked-add 2 i)))))

(defn ->tam-do-assoc [tam key val]
  (log/debug (format "->tam-do-assoc key %s val %s" key val))
  (let [i (tam-index-of key tam)
        a (.array tam)
        l (.len tam)]
    (if (>= i 0) ;already have key
      (if (not= (aget a (inc i)) val)
        (do (aset a (inc i) val) tam))
      (if (>= l (alength a))
        (let [p (hm/create-persistent a)
              t (ec/as-transient p)]
          (tm/assoc t key val))
        (do
          (aset a (inc l) key)
          (aset a (+ 2 l) val)
          tam)))))

(defn- pam-index-of-object [pam key]
  (log/debug (format "pam-index-of-object key '%s'" key))
  (let [ep (Util/equivPred key)
        a (.array pam)]
    (or (last (for [i (filter even? (range 0 (alength a)))
                    :while (.equiv ep key (aget a i))] i)) -1)))

(defn- pam-index-of [pam key]
  (let [a (.array pam)]
    (if (instance? clojure.lang.Keyword key)
      (or (last (for [i (filter even? (range 0 (alength a)))
                      :while (identical? key (aget a i))] i)) -1)
      (pam-index-of-object pam key))))

(defn- create [pam a]
  "Creates a new pam instance using the same meta as the given one"
  (PersistentArrayMap. a (.meta pam)))

(defn ->pam-assoc [pam key val]
  (log/debug (format "->pam-assoc key '%s' val '%s'" key val))
  (let [i (pam-index-of pam key)
        a (.array pam)]
    (if (>= i 0)
      (if (identical? (aget a (inc i)) val)
        pam
        (create pam (doto (.clone a) (aset (inc i) val))))
      (if (> (alength (.array pam)) *hashtable_threshold*)
        (assoc (create pam) key val)
        (let [to (make-array Object (+ 2 (alength a)))]
          (when (> (alength (.array pam)) 0) (System/arraycopy a 0 to 2 (alength a)))
          (aset to 0 key)
          (aset to 1 val)
          (create pam to))))))

(defn ->pam-val-at
  ([pam k] (->pam-val-at pam k nil))
  ([pam k not-found]
   (let [i (pam-index-of pam k)]
     (if (>= i 0)
       (aget (.array pam) (inc i))
       not-found))))

(extend PersistentArrayMap
  pm/PersistentMap
  (assoc pm/PersistentMapImpl
         :assoc #'->pam-assoc
         :valAt #'->pam-val-at))

(extend TransientArrayMap
  tm/TransientMap
  (assoc tm/TransientMapImpl
         :doCount (fn [this] "am->doCount")
         :ensureEditable #'->tam-ensure-editable
         :doAssoc #'->tam-do-assoc))
