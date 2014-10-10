(ns reclojure.lang.array-map
  (:require [reclojure.lang.hash-map :as hm]
            [reclojure.lang.protocols.transient-map :as tm]
            [reclojure.lang.protocols.editable-collection :as ec]
            [reclojure.lang.protocols.persistent-map :as pm])
  (:import [clojure.lang Util]))

(def ^:dynamic *hashtable_threshold* 16)

(defrecord TransientArrayMap [len array owner])
(defrecord PersistentArrayMap [meta init])

(def EMPTY (PersistentArrayMap. nil nil))

(defn- init-array [^objects from]
  (let [l (max *hashtable_threshold* (alength from))
        to (make-array Object (alength from))]
    (System/arraycopy from 0 to 0 (alength from))
    to))

(defn create-transient [^objects a]
  (TransientArrayMap. (alength a) (init-array a) (Thread/currentThread)))

(defn doPersistent [this]
  (let [to (make-array Object (alength (:array this)))]
    (PersistentArrayMap. nil (System/arraycopy (:array this) 0 to 0 (alength (:array this))))))

(defn as-transient []
  (create-transient (.toArray (java.util.Collections/emptyList))))

(defn ->tam-ensure-editable [tam]
  (cond
    (identical? (:owner tam) (Thread/currentThread)) nil
    (not (nil? (:owner tam))) (throw (IllegalAccessError. "Transient used by non-owner thread"))
    ;:else (throw (IllegalAccessError. (str "Transient used after persistent! call for tam " tam)))))
    :else (println "tam->ensure-editable: no match")
    ))

(defn persistent [this]
  (do
    (->tam-ensure-editable this)
    (doPersistent this)))

(defn create-persistent [m]
  (let [trans (as-transient)
        objects (.toArray (.entrySet m))]
    (persistent (for [i (range (alength objects))]
                  (tm/assoc trans
                         (.getKey (aget objects i))
                         (.getValue (aget objects i)))))))

(defn tam-index-of [k tam]
  (loop [i 0]
    (cond (>= i (:len tam)) -1
          (= k (aget ^objects (:array tam) i)) i
          :else (recur (unchecked-add 2 i)))))

(defn ->tam-do-assoc [tam key val]
  (let [i (tam-index-of key tam)
        a (:array tam)
        l (:len tam)]
    (if (>= i 0)
      (if (not= (aget a (inc i)) val)
        (do (aset a (inc i) val) tam))
      (if (>= l (alength a))
        (assoc (ec/as-transient (hm/create-persistent a)) key val)
        (do 
          (aset a (inc l) key) 
          (aset a (+ 2 l) val) 
          tam)))))

(defn- pam-index-of-object [pam key]
  (let [ep (Util/equivPred key)
        a (:array pam)]
    (or (last (for [i (filter even? (range 0 (alength a)))
                    :while (.equiv ep key (aget a i))] i)) -1)))

(defn- pam-index-of [pam key]
  (let [a (:array pam)]
    (if (instance? clojure.lang.Keyword key)
      (or (last (for [i (filter even? (range 0 (alength a)))
                      :while (identical? key (aget a i))] i)) -1)
      (pam-index-of-object pam key))))

(defn- create [a] "am->create implement me")
(defn- createHT [a] "am->createHT implement me")

(defn ->pam-assoc [pam key val]
  (let [i (pam-index-of pam key)
        a (:array pam)]
    (if (>= i 0)
      (if (identical? (aget a (inc i)) val)
        pam
        (create (aset (.clone a) (inc i) val)))
      (if (> (:length pam) *hashtable_threshold*)
        (assoc (createHT pam) key val)
        (let [to (make-array Object (+ 2 (alength a)))]
          (when (> (:length pam) 0) (System/arraycopy a 0 to 2 (alength a)))
          (aset to 0 key)
          (aset to 1 val)
          (create to))))))

(extend PersistentArrayMap
  pm/PersistentMap
  (assoc pm/PersistentMapImpl
         :assoc #'->pam-assoc))

(extend TransientArrayMap
  tm/TransientMap
  (assoc tm/TransientMapImpl
         :doCount (fn [this] "am->doCount")
         :ensureEditable #'->tam-ensure-editable
         :doAssoc #'->tam-do-assoc))

