(ns reclojure.lang.hash-map
  (:require [reclojure.lang.protocols.editable-collection :as ec]
            [reclojure.lang.protocols.transient-map :as tm]
            [reclojure.lang.protocols.node :as node]
            [reclojure.lang.box]
            [clojure.tools.logging :as log]
            [reclojure.lang.util :as u]
            [reclojure.lang.node :refer [EMPTY_BIN]]
            [reclojure.lang.protocols.persistent-map :as pm])
  (:refer-clojure :exclude [count meta])
  (:import [reclojure.lang.box Box]
           [reclojure.lang.protocols.node Node]))

(u/defmutable PersistentHashMap [phmMeta phmCount phmRoot phmHasNull phmNullValue])
(u/defmutable TransientHashMap [thmEdit thmRoot thmCount thmHasNull thmNullValue thmLeafFlag])

(defn EMPTY []
  (log/debug (format "EMPTY phm"))
  (PersistentHashMap. nil 0 nil false nil))

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

(defn ->phm-assoc [phm k v]
  (log/debug (format "->phm-assoc count '%s' k '%s' v '%s'" (.phmCount phm) k v))
  (let [has-null (.phmHasNull phm)
        null-value (.phmNullValue phm)
        meta (.phmMeta phm)
        root (.phmRoot phm)
        count (.phmCount phm)]
    (if (nil? k)
      (if (and has-null (identical? v null-value))
        phm
        (PersistentHashMap. (.phmMeta phm) (if has-null count (inc count)) root true v))
      (let [added-leaf (Box. nil)
            old-or-new (if (nil? root) (EMPTY_BIN) root)
            new-root (node/assoc old-or-new 0 (hash k) k v added-leaf)]
        (if (identical? new-root root)
          phm
          (PersistentHashMap. (.phmMeta phm) (if (nil? @added-leaf) count (inc count)) new-root has-null null-value))))))

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
          empty-or-current (if (nil? (.thmRoot thm)) (EMPTY_BIN) (.thmRoot thm))
          bin (node/assoc empty-or-current (.thmEdit thm) 0 (unchecked-int (hash key)) key val (.thmLeafFlag thm))]
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

(defn -main [& args]
  (let [f (slurp (clojure.java.io/resource "test-words.txt"))
        words (clojure.string/split f #"\W")]
    (reduce #(pm/assoc %1 %2 %2) (reclojure.lang.hash-map/EMPTY) words)))
