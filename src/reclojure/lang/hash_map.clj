(ns reclojure.lang.hash-map
  (:require [reclojure.lang.protocols.editable-collection :as ec]
            [reclojure.lang.protocols.transient-map :as tm]
            [reclojure.lang.protocols.persistent-map :as pm]))

(defrecord TransientHashMap [edit root count has-null null-value])
(defrecord PersistentHashMap [meta count root has-null null-value])

(def EMPTY (PersistentHashMap. nil 0 nil false nil))

(defn create-transient [phm]
  (TransientHashMap.
    (java.util.concurrent.atomic.AtomicReference. (Thread/currentThread))
    (:root phm)
    (:count phm)
    (:has-null phm)
    (:null-value phm)))

(defn ->thm-do-persistent [thm]
  (.set (:edit thm) nil)
  (PersistentHashMap. nil (:count thm) (:root thm) (:has-null thm) (:null-value thm)))

(defn ->phm-as-transient [phm]
  (create-transient phm))

(defn create-persistent [^objects xs]
  (let [thm (ec/as-transient EMPTY)]
    (do
      (doall (for [i (filter even? (range 0 (alength xs)))]
               (tm/assoc thm (aget xs i) (aget xs (inc i)))))
      (tm/persistent thm))))

(defn ->thm-ensure-editable [thm]
  (let [owner (.get (:edit thm))]
    (cond
      (identical? owner (Thread/currentThread)) nil
      (not (nil? owner)) (throw (IllegalAccessError. (str "Transient used by non-owner thread owner " owner " current " (Thread/currentThread))))
      ;:else (throw (IllegalAccessError. (str "Transient used after persistent! call for tam " tam)))))
      :else (println "thm->ensureEditable: no match")
      )))

(defn ->phm-assoc [this a b]
  (println "### phm->assoc this" this "a" a "b" b))

(defn ->thm-do-assoc [thm key val]
  (if (nil? key)
    (cond 
      (not= (:null-value thm) nil) 
      (assoc thm :null-value val)
      (not (:has-null thm)) 
      (-> thm 
          (assoc :count (inc (:count thm)))
          (assoc :has-null true))
      :else thm)
    "Uber tangent for BitmapIndexedNode"))

;  if (key == null) {
;      if (this.nullValue != val)
;        this.nullValue = val;
;      if (!hasNull) {
;        this.count++;
;        this.hasNull = true;
;      }
;      return this;
;    }
;    //    Box leafFlag = new Box(null);
;    leafFlag.val = null;
;    INode n = (root == null ? BitmapIndexedNode.EMPTY : root)
;      .assoc(edit, 0, hash(key), key, val, leafFlag);
;    if (n != this.root)
;      this.root = n;
;    if(leafFlag.val != null) this.count++;
;    return this;

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
