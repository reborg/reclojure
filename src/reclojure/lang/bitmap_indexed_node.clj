(ns reclojure.lang.bitmap-indexed-node
  (:require [reclojure.lang.protocols.node :as node]
            [reclojure.lang.hash-collision-node]
            [reclojure.lang.array-node]
            [clojure.tools.logging :as log]
            [reclojure.lang.util :as util])
  (:import [reclojure.lang.hash_collision_node HashCollisionNode]
           [reclojure.lang.array_node ArrayNode]
           [java.util.concurrent.atomic AtomicReference])
  (:refer-clojure :exclude [hash]))

(util/defmutable BitmapIndexedNode [edit bitmap array])

(def EMPTY (BitmapIndexedNode. nil 0 (.toArray (java.util.Collections/emptyList))))

(defn index [node bit]
  (Integer/bitCount (bit-and (.bitmap node) (- bit 1))))

(defn bitpos [hash shift]
  (bit-shift-left 1 (bit-and (unsigned-bit-shift-right hash shift) 0x01f)))

(defn mask [hash shift]
  (bit-and (unsigned-bit-shift-right hash shift) 0x1f))

(defn clone-and-set [array idx obj]
  (let [clone (.clone array)]
    (aset clone idx obj)))

(defn hash [k]
  (util/hasheq k))

(defn create-node [shift key1 val1 key2hash key2 val2]
  (let [key1hash (hash key1)]
    (if (identical? key1hash key2hash)
      (HashCollisionNode.  nil key1hash 2
        (.toArray (doto
                    (java.util.ArrayList.)
                    (.add key1)
                    (.add val1)
                    (.add key2)
                    (.add val2))))
      (let [added-leaf nil
            edit (AtomicReference.)]
        (-> EMPTY
            (.assoc edit shift key1hash key1 val1 added-leaf)
            (.assoc edit shift key2hash key2 val2 added-leaf))))))

(defn array-copy [array size length]
  (let [new-array (make-array Object size)]
    (do
      (System/arraycopy array 0 new-array 0 length)
      new-array)))

(defn ->bin-assoc
  ([node shift hash key val added-leaf]
   (let [bit (bitpos hash shift)
         idx (index node bit)
         bitmap (:bitmap node)
         array (:array node)]
     (log/debug (format "bit %s idx %s" bit idx))
     ; if
     (if (not (zero? (bit-and bitmap bit)))
       (let [key-or-null (aget array (* 2 idx))
             val-or-node (aget array (inc (* 2 idx)))]
         (cond
           (nil? key-or-null)
           (let [n (->bin-assoc (+ 5 shift) hash key val added-leaf)]
             (if (= val-or-node n)
               node
               (BitmapIndexedNode. nil bitmap (clone-and-set array (inc (+ 2 idx)) n))))
           (util/equiv key key-or-null)
           (if (identical? val val-or-node)
             node
             (BitmapIndexedNode. nil bitmap (clone-and-set array (inc (+ 2 idx)) val)))
           :else
           (do
             (.update added-leaf added-leaf)
             (->>
               (create-node (+ 5 shift) key-or-null val-or-node hash key val)
               (clone-and-set array (+ 2 idx) nil (inc (+ 2 idx)))
               (BitmapIndexedNode. nil bitmap)))))
       ; else
       (let [n (Integer/bitCount bitmap)]
         (if (>= n 16)
           (let [nodes (make-array BitmapIndexedNode 32)
                 jdx (mask hash shift)
                 noop (aset nodes jdx (.assoc EMPTY (+ 5 shift) hash key val added-leaf))
                 j 0]
             (for [i (range 32)
                   j (filter even? (range 32))]
               (when (not (zero? (bit-and (unsigned-bit-shift-right bitmap i) 1)))
                 (if (nil? (aget array j))
                   (aset nodes i (aget array (inc j)))
                   (aset nodes i (.assoc EMPTY
                                         (+ shift 5)
                                         (hash (aget array j))
                                         (aget array j)
                                         (aget array (inc j))
                                         added-leaf)))))
             (ArrayNode. nil (inc n) nodes))
           ; else <16
           (let [new-array (array-copy array (* 2 (inc n)) (* 2 idx))
                 noop (aset new-array (* 2 idx) key)
                 noop (.update added-leaf added-leaf)
                 noop (System/arraycopy array (* 2 idx) new-array (* 2 (inc idx)) (* 2 (- n idx)))]
             (BitmapIndexedNode. nil (bit-or bitmap bit) new-array)))))))
  ([node edit shift hash key val added-leaf]
   (let [bit (bitpos hash shift)
         idx (index node bit)
         bitmap (:bitmap node)
         array (:array node)]
     (if (not (zero? (bit-and bitmap bit)))
       (let [key-or-null (aget array (* 2 idx))
             val-or-node (aget array (inc (* 2 idx)))]
         (cond
           (nil? key-or-null)
           (let [n (->bin-assoc (+ 5 shift) hash key val added-leaf)]
             (if (= val-or-node n)
               node
               (editAndSet edit (inc (* 2 idx)) n)))
           (util/equiv key key-or-null)
           (if (identical? val val-or-node)
             node
             (edit-and-set edit (inc (* 2 idx)) val))
           :else
           (do
             (.update added-leaf added-leaf)
             (->>
               (create-node (+ 5 shift) key-or-null val-or-node hash key val)
               (edit-and-set edit (* 2 idx) nil (inc (* 2 idx)))))))
       ; else
       (let [n (Integer/bitCount bitmap)]
         (if (< (* 2 n) (.length array))
           (let [noop (.update added-leaf added-leaf)
                 editable (ensure-editable edit)
             noop (System/arraycopy (.array editable) (* 2 idx) (.array editable) (* 2 (inc idx)) (* 2 (- n idx)))
                    editable.array[2*idx] = key;
                    editable.array[2*idx+1] = val;
                    editable.bitmap |= bit;
                    return editable;
             ))
         (if (>= n 16)
           (let [nodes (make-array BitmapIndexedNode 32)
                 jdx (mask hash shift)
                 noop (aset nodes jdx (.assoc EMPTY (+ 5 shift) hash key val added-leaf))
                 j 0]
             (for [i (range 32)
                   j (filter even? (range 32))]
               (when (not (zero? (bit-and (unsigned-bit-shift-right bitmap i) 1)))
                 (if (nil? (aget array j))
                   (aset nodes i (aget array (inc j)))
                   (aset nodes i (.assoc EMPTY
                                         (+ shift 5)
                                         (hash (aget array j))
                                         (aget array j)
                                         (aget array (inc j))
                                         added-leaf)))))
             (ArrayNode. nil (inc n) nodes))
           ; else <16
           (let [new-array (array-copy array (* 2 (inc n)) (* 2 idx))
                 noop (aset new-array (* 2 idx) key)
                 noop (.update added-leaf added-leaf)
                 noop (System/arraycopy array (* 2 idx) new-array (* 2 (inc idx)) (* 2 (- n idx)))]
             (BitmapIndexedNode. nil (bit-or bitmap bit) new-array)))))))


;(defn ->bin-assoc
;  ([node shift hash key val added-leaf] (->bin-assoc node nil shift hash key val adde-leaf))
;  ([node edit shift hash key val added-leaf]
;   (let [bit (bitpos hash shift)
;         idx (index node bit)
;         bitmap (.bitmap node)
;         array (.array node)]
;     (log/debug (format "bit %s idx %s" bit idx))
;     ; if
;     (if (pos? (bit-and bitmap bit))
;       (let [key-or-null (aget array (* 2 idx))
;             val-or-node (aget array (inc (* 2 idx)))]
;         (cond
;           (nil? key-or-null)
;           (let [n (->bin-assoc (+ 5 shift) hash key val added-leaf)]
;             (cond
;               (= val-or-node n) node
;               (not (nil? edit)) (edit-and-set edit (inc (* 2 idx)) n)
;               :else (BitmapIndexedNode. nil bitmap (clone-and-set array (inc (+ 2 idx)) n))))
;           (util/equiv key key-or-null)
;           (cond
;             (identical? val val-or-node) node
;             (not (nil? edit)) (edit-and-set edit (inc (* 2 idx)) val)
;             :else (BitmapIndexedNode. nil bitmap (clone-and-set array (inc (+ 2 idx)) val)))
;           :else
;           (do
;             (.update added-leaf added-leaf)
;             (if (not (nil? edit))
;               (edit-and-set edit (* 2 idx) nil (inc (* 2 idx)) (create-node edit (+ 5 shift) key-or-null val-or-node hash key val))
;               (->> (create-node (+ 5 shift) key-or-null val-or-node hash key val)
;                    (clone-and-set array (+ 2 idx) nil (inc (+ 2 idx)))
;                    (BitmapIndexedNode. nil bitmap))))))
;       ; else
;       (let [n (Integer/bitCount bitmap)]
;         (if (and (not (nil? edit)) (< (* 2 n) (:length array)))
;           (let [noop (.update added-leaf added-leaf)
;                 editable (ensure-editable edit)
;             ))
;         (if (>= n 16)
;           (let [nodes (make-array BitmapIndexedNode 32)
;                 jdx (mask hash shift)
;                 noop (aset nodes jdx (.assoc EMPTY (+ 5 shift) hash key val added-leaf))
;                 j 0]
;             (for [i (range 32)
;                   j (filter even? (range 32))]
;               (when (not (zero? (bit-and (unsigned-bit-shift-right bitmap i) 1)))
;                 (if (nil? (aget array j))
;                   (aset nodes i (aget array (inc j)))
;                   (aset nodes i (.assoc EMPTY
;                                         (+ shift 5)
;                                         (hash (aget array j))
;                                         (aget array j)
;                                         (aget array (inc j))
;                                         added-leaf)))))
;             (ArrayNode. nil (inc n) nodes))
;           ; else <16
;           (let [new-array (array-copy array (* 2 (inc n)) (* 2 idx))
;                 noop (aset new-array (* 2 idx) key)
;                 noop (.update added-leaf added-leaf)
;                 noop (System/arraycopy array (* 2 idx) new-array (* 2 (inc idx)) (* 2 (- n idx)))]
;             (BitmapIndexedNode. nil (bit-or bitmap bit) new-array))))))))

(extend BitmapIndexedNode
  node/Node
  {:assoc #'->bin-assoc})
