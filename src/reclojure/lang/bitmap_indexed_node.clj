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


(defn- ensure-editable [bin edit]
  (if (identical? (.edit bin) edit)
    bin
    (let [n (Integer/bitCount (.bitmap bin))
          size (if (>= n 0) (* 2 (inc n)) 4)
          new-array (array-copy (.array bin) size (* 2 n))]
      (BitmapIndexedNode. edit (.bitmap bin) new-array))))

(defn- edit-and-set
  ([bin edit i a]
   (let [editable (ensure-editable bin edit)]
     (doto (.array editable) (aset i a))))
  ([bin edit i a j b]
   (let [editable (ensure-editable bin edit)]
     (doto (.array editable) (aset i a) (aset j b)))))

(defn ->bin-assoc
  ([node shift hash key val added-leaf]
   (log/debug (format "->bin-assoc node %s shift %s hash %s key %s val %s added-leaf %s" node shift hash key val added-leaf))
   (let [bit (bitpos hash shift)
         idx (index node bit)
         bitmap (.bitmap node)
         array (.array node)]
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
   (log/debug (format "->bin-assoc node '%s' edit '%s' shift '%s' hash '%s' key '%s' val '%s' added-leaf '%s'" node edit shift hash key val added-leaf))
   (let [bit (bitpos hash shift)
         idx (index node bit)
         bitmap (.bitmap node)
         array (.array node)]
     (log/debug (format "->bin-assoc bit '%s' idx '%s' bitmap '%s' array '%s' bit-and '%s'" bit idx bitmap array (bit-and bitmap bit)))
     (if (not (zero? (bit-and bitmap bit)))
       (let [key-or-null (aget array (* 2 idx))
             val-or-node (aget array (inc (* 2 idx)))]
         (cond
           (nil? key-or-null)
           (let [n (->bin-assoc (+ 5 shift) hash key val added-leaf)]
             (if (= val-or-node n)
               node
               (edit-and-set node edit (inc (* 2 idx)) n)))
           (util/equiv key key-or-null)
           (if (identical? val val-or-node)
             node
             (edit-and-set node edit (inc (* 2 idx)) val))
           :else
           (do
             (.update added-leaf added-leaf)
             (->>
               (create-node (+ 5 shift) key-or-null val-or-node hash key val)
               (edit-and-set node edit (* 2 idx) nil (inc (* 2 idx)))))))
       ; else
       (let [n (Integer/bitCount bitmap)]
         (log/debug (format "->bin-assoc count array '%s' n '%s'" (count array) n))
         (if (< (* 2 n) (count array))
           (let [editable (ensure-editable node edit)]
             (.update added-leaf added-leaf)
             (.bitmap! editable (bit-or (.bitmap editable) bit))
             (doto
               (.array editable)
               (System/arraycopy (* 2 idx) (.array editable) (* 2 (inc idx)) (* 2 (- n idx)))
               (aset (* 2 idx) key)
               (aset (inc (* 2 idx)) val))
             editable))
         (if (>= n 16)
           (let [nodes (make-array BitmapIndexedNode 32)
                 jdx (mask hash shift)
                 noop (aset nodes jdx (->bin-assoc EMPTY edit (+ 5 shift) hash key val added-leaf))
                 j 0]
             (for [i (range 32)
                   j (filter even? (range 32))]
               (when (not (zero? (bit-and (unsigned-bit-shift-right bitmap i) 1)))
                 (if (nil? (aget array j))
                   (aset nodes i (aget array (inc j)))
                   (aset nodes i (.assoc EMPTY
                                         edit
                                         (+ shift 5)
                                         (hash (aget array j))
                                         (aget array j)
                                         (aget array (inc j))
                                         added-leaf)))))
             (ArrayNode. edit (inc n) nodes))
           ; else <16
           (let [new-array (array-copy array (* 2 (+ 4 n)) (* 2 idx))
                 editable (ensure-editable node edit)]
             (doto new-array (aset (* 2 idx) key) (aset (inc (* 2 idx)) val))
             (.update added-leaf added-leaf)
             (System/arraycopy array (* 2 idx) new-array (* 2 (inc idx)) (* 2 (- n idx)))
             (log/debug (format "->bin-assoc array '%s' new-array '%s'" (java.util.Arrays/toString array) (java.util.Arrays/toString new-array)))
             (doto editable (.array! new-array) (.bitmap! (bit-or (.bitmap editable) bit)))
             (log/debug (format "->bin-assoc editable array '%s'" (java.util.Arrays/toString (.array editable))))
             editable)))))))

(extend BitmapIndexedNode
  node/Node
  {:assoc #'->bin-assoc})
