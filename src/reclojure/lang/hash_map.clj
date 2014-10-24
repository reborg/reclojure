(ns reclojure.lang.hash-map
  (:require [reclojure.lang.protocols.editable-collection :as ec]
            [reclojure.lang.protocols.transient-map :as tm]
            [reclojure.lang.protocols.node :as node]
            [reclojure.lang.box]
            [reclojure.lang.util :as u]
            [reclojure.lang.bitmap-indexed-node :as bin]
            [reclojure.lang.protocols.persistent-map :as pm])
  (:refer-clojure :exclude [count meta])
  (:import [reclojure.lang.box Box]))

(u/defmutable PersistentHashMap [phmMeta phmCount phmRoot phmHasNull phmNullValue])
(u/defmutable TransientHashMap [thmEdit thmRoot thmCount thmHasNull thmNullValue thmLeafFlag])

(def EMPTY (PersistentHashMap. nil 0 nil false nil))

(defn create-transient [phm]
  (TransientHashMap.
    (java.util.concurrent.atomic.AtomicReference. (Thread/currentThread))
    (.phmRoot phm)
    (.phmCount phm)
    (.phmHasNull phm)
    (.phmNullValue phm)
    (Box. nil)))

(defn ->thm-do-persistent [thm]
  (.thmEdit! thm nil)
  (PersistentHashMap.
    nil
    (.thmCount thm)
    (.thmRoot thm)
    (.thmHasNull thm)
    (.thmNullValue thm)))

(defn ->phm-as-transient [phm]
  (create-transient phm))

(defn create-persistent [^objects xs]
  (let [thm (ec/as-transient EMPTY)]
    (do
      (doall (for [i (filter even? (range 0 (alength xs)))]
               (tm/assoc thm (aget xs i) (aget xs (inc i)))))
      (tm/persistent thm))))

(defn ->thm-ensure-editable [thm]
  (let [owner (.get (.thmEdit thm))]
    (cond
      (identical? owner (Thread/currentThread)) nil
      (not (nil? owner)) (throw (IllegalAccessError. (str "Transient used by non-owner thread owner " owner " current " (Thread/currentThread))))
      ;:else (throw (IllegalAccessError. (str "Transient used after persistent! call for tam " tam)))))
      :else (println "thm->ensureEditable: no match")
      )))

(defn ->phm-assoc [this a b]
  (str "### not implemented phm->assoc this" this "a" a "b" b))

(defn ->thm-do-assoc [thm key val]
  (if (nil? key)
    (cond
      (not= (.thmNullValue thm) nil)
      (.thmNullValue! thm val)
      (not (.thmNullValue thm))
      (-> thm
          (.thmCount! thm (inc (.thmCount thm)))
          (.thmHasNull! thm true))
      :else thm)
    (let [leaf-flag (Box. nil)
          empty-or-current (if (nil? (.thmRoot thm)) bin/EMPTY (.thmRoot thm))
          n (node/assoc empty-or-current (.thmEdit thm) 0 (hash key) key val leaf-flag)]
      (do (if (not (identical? n (.thmRoot thm))) (.thmRoot! thm n))
          (if (not (nil? @(.thmLeafFlag thm))) (inc (.thmCount thm)))
          thm))))

(extend TransientHashMap
  tm/TransientMap
  (assoc tm/TransientMapImpl
         :doPersistent #'->thm-do-persistent
         :doAssoc #'->thm-do-assoc
         :ensureEditable #'->thm-ensure-editable))

(extend PersistentHashMap
  pm/PersistentMap
  (assoc pm/PersistentMapImpl
         :assoc #'->phm-assoc)
  ec/EditableCollection
  {:as-transient #'->phm-as-transient})
