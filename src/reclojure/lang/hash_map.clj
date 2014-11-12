(ns reclojure.lang.hash-map
  (:require [reclojure.lang.protocols.editable-collection :as ec]
            [reclojure.lang.protocols.transient-map :as tm]
            [reclojure.lang.protocols.node :as node]
            [reclojure.lang.box]
            [clojure.tools.logging :as log]
            [reclojure.lang.util :as u]
            [reclojure.lang.bitmap-indexed-node :as bin]
            [reclojure.lang.protocols.persistent-map :as pm])
  (:refer-clojure :exclude [count meta])
  (:import [reclojure.lang.box Box]))

(u/defmutable PersistentHashMap [phmMeta phmCount phmRoot phmHasNull phmNullValue])
(u/defmutable TransientHashMap [thmEdit thmRoot thmCount thmHasNull thmNullValue thmLeafFlag])

(defn EMPTY [] (PersistentHashMap. nil 0 nil false nil))

(defn create-transient [phm]
  (log/debug (format "create-transient for phm"))
  (TransientHashMap.
    (java.util.concurrent.atomic.AtomicReference. (Thread/currentThread))
    (.phmRoot phm)
    (.phmCount phm)
    (.phmHasNull phm)
    (.phmNullValue phm)
    (Box. nil)))

(defn ->thm-do-persistent [thm]
  (log/debug (format "->thm-do-persistent thmCount %s" (.thmCount thm)))
  (.thmEdit! thm nil)
  (PersistentHashMap.
    nil
    (.thmCount thm)
    (.thmRoot thm)
    (.thmHasNull thm)
    (.thmNullValue thm)))

(defn ->phm-as-transient [phm]
  (log/debug (format "->phm-as-transient"))
  (create-transient phm))

(defn create-persistent [^objects xs]
  (log/debug (format "create-persistent from object array %s" (u/aprint xs)))
  (let [thm (ec/as-transient (EMPTY))]
    (doall
      (for [i (filter even? (range 0 (alength xs)))]
        (tm/assoc thm (aget xs i) (aget xs (inc i)))))
    (tm/persistent thm)))

(defn ->thm-ensure-editable [thm]
  (log/debug (format "->thm-ensure-editable thm count %s" (.thmCount thm)))
  (let [owner (.get (.thmEdit thm))]
    (if (nil? owner)
      (throw (IllegalAccessError. (str "Transient used after persistent! call for thm " thm))))))

(defn ->phm-assoc [this a b]
  (throw (RuntimeException. (format "### not implemented phm->assoc this" ))))

(defn ->thm-do-assoc [thm key val]
  (log/debug (format "->thm-do-assoc thm with key '%s'" key))
  (if (nil? key)
    (do
      (cond
        (not= (.thmNullValue thm) val)
        (.thmNullValue! thm val)
        (not (.thmHasNull thm))
        (-> thm
            (.thmCount! (inc (.thmCount thm)))
            (.thmHasNull! true)))
      thm)
    (let [
          _ (.update (.thmLeafFlag thm) nil)
          empty-or-current (if (nil? (.thmRoot thm)) bin/EMPTY (.thmRoot thm))
          bin (node/assoc empty-or-current (.thmEdit thm) 0 (hash key) key val (.thmLeafFlag thm))]
      (do
        (if (not (identical? bin (.thmRoot thm))) (.thmRoot! thm bin))
        (log/debug (format "->thm-do-assoc thmLeafFlag box '%s'" @(.thmLeafFlag thm)))
        (if (not (nil? @(.thmLeafFlag thm)))
          (.thmCount! thm (inc (.thmCount thm))))
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
