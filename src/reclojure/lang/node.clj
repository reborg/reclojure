(ns reclojure.lang.node
  (:require [reclojure.lang.protocols.node :as node]
            [reclojure.lang.box]
            [clojure.tools.logging :as log]
            [reclojure.lang.util :as util])
  (:import [reclojure.lang.box Box]
           [java.util.concurrent.atomic AtomicReference])
  (:refer-clojure :exclude [hash]))

(util/defmutable BitmapIndexedNode [binEdit binBitmap binArray])
(util/defmutable ArrayNode [anEdit anCount anArray])
(util/defmutable HashCollisionNode [hcnEdit hcnHash hcnCount hcnArray])

(defn EMPTY_BIN [] (BitmapIndexedNode. nil (int 0) (.toArray (java.util.Collections/emptyList))))

(defn index [node bit]
  (Integer/bitCount (bit-and (.binBitmap node) (- bit 1))))

(defn bitpos [hash shift]
  (log/debug (format "bitpos hash '%s' shift '%s'" hash shift))
  (clojure.lang.Numbers/shiftLeftInt 1 (util/mask hash shift)))

(defn hash [k]
  (util/hasheq k))

(defn create-node [shift key1 val1 key2hash key2 val2]
(log/debug (format "create-node shift '%s' key1 '%s' val1 '%s' key2hash '%s' key2 '%s' val2 '%s'" shift key1 val1 key2hash key2 val2))
  (let [key1hash (hash key1)]
    (if (= key1hash key2hash)
      (HashCollisionNode. nil key1hash 2
        (.toArray (doto
                    (java.util.ArrayList.)
                    (.add key1)
                    (.add val1)
                    (.add key2)
                    (.add val2))))
      (let [added-leaf (Box. nil)
            edit (AtomicReference.)]
        (-> (EMPTY_BIN)
            (node/assoc edit shift key1hash key1 val1 added-leaf)
            (node/assoc edit shift key2hash key2 val2 added-leaf))))))

(defn array-copy [array size length]
  (let [new-array (make-array Object size)]
    (do
      (System/arraycopy array 0 new-array 0 length)
      new-array)))


(defn- ensure-editable [bin edit]
  (if (identical? (.binEdit bin) edit)
    bin
    (let [n (Integer/bitCount (.binBitmap bin))
          size (if (>= n 0) (* 2 (inc n)) 4)
          new-array (array-copy (.binArray bin) size (* 2 n))]
      (BitmapIndexedNode. edit (.binBitmap bin) new-array))))

(defn- edit-and-set
  ([bin edit i a]
   (let [editable (ensure-editable bin edit)]
     (doto (.binArray editable) (aset i a))))
  ([bin edit i a j b]
   (let [editable (ensure-editable bin edit)]
     (doto (.binArray editable) (aset i a) (aset j b)))))

(defn ->bin-assoc
  ([node shift hash key val added-leaf]
   (let [bit (bitpos hash shift)
         shift (unchecked-int shift)
         hash (unchecked-int hash)
         idx (index node bit)
         bitmap (.binBitmap node)
         array (.binArray node)]
     (log/debug (format "->bin-assoc (no edit) let bindings: bit '%s' shift '%s' hash '%s' idx '%s' bitmap '%s'. (bit-and bitmap bit)? '%s'" bit shift hash idx bitmap (bit-and bitmap bit)))
     ; if
     (if (not (zero? (bit-and bitmap bit)))
       (let [key-or-null (aget array (* 2 idx))
             val-or-node (aget array (inc (* 2 idx)))]
             (log/debug (format "->bin-assoc (no edit) key '%s' key-or-null '%s' (nil? key-or-null) '%s' (util/equiv key key-or-null) '%s'" key key-or-null (nil? key-or-null) (util/equiv key key-or-null)))
         (cond
           (nil? key-or-null)
           (let [n (->bin-assoc node (+ 5 shift) hash key val added-leaf)]
             (if (= val-or-node n)
               node
               (BitmapIndexedNode. nil bitmap (util/clone-and-set array (inc (* 2 idx)) n))))
           (util/equiv key key-or-null)
           (if (identical? val val-or-node)
             node
             (BitmapIndexedNode. nil bitmap (util/clone-and-set array (inc (* 2 idx)) val)))
           :else
           (do
             (.update added-leaf added-leaf)
             (->>
               (create-node (+ 5 shift) key-or-null val-or-node hash key val)
               (util/clone-and-set array (* 2 idx) nil (inc (* 2 idx)))
               (BitmapIndexedNode. nil bitmap)))))
       ; else
       (let [n (Integer/bitCount bitmap)]
         (log/debug (format "->bin-assoc (no edit) bitcount '%s'" n))
         (if (>= n 16)
           (let [nodes (make-array BitmapIndexedNode 32)
                 jdx (util/mask hash shift)
                 noop (aset nodes jdx (node/assoc (EMPTY_BIN) (+ 5 shift) hash key val added-leaf))
                 j 0]
             (for [i (range 32)
                   j (filter even? (range 32))]
               (when (not (zero? (bit-and (unsigned-bit-shift-right bitmap i) 1)))
                 (if (nil? (aget array j))
                   (aset nodes i (aget array (inc j)))
                   (aset nodes i (node/assoc (EMPTY_BIN)
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
             (log/debug (format "->bin-assoc (no edit, <16) bitmap '%s' bit '%s'" bitmap bit))
             (BitmapIndexedNode. nil (bit-or bitmap bit) new-array)))))))
  ([node edit shift hash key val added-leaf]
   (log/debug (format "->bin-assoc node '%s' edit '%s' shift '%s' hash '%s' key '%s' val '%s' added-leaf '%s'" node edit shift hash key val added-leaf))
   (let [bit (bitpos hash shift)
         idx (index node bit)
         bitmap (.binBitmap node)
         array (.binArray node)]
     (log/debug (format "->bin-assoc bit '%s' idx '%s' bitmap '%s' array '%s' bit-and '%s'" bit idx bitmap array (bit-and bitmap bit)))
     (if (not (zero? (bit-and bitmap bit)))
       (let [key-or-null (aget array (* 2 idx))
             val-or-node (aget array (inc (* 2 idx)))]
         (cond
           (nil? key-or-null)
           (let [n (->bin-assoc node (+ 5 shift) hash key val added-leaf)]
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
         (log/debug (format "->bin-assoc [edit] count array '%s' n '%s'" (count array) n))
         (if (< (* 2 n) (count array))
           (let [editable (ensure-editable node edit)]
             (.update added-leaf added-leaf)
             (.binBitmap! editable (bit-or (.binBitmap editable) bit))
             (doto
               (.binArray editable)
               (System/arraycopy (* 2 idx) (.binArray editable) (* 2 (inc idx)) (* 2 (- n idx)))
               (aset (* 2 idx) key)
               (aset (inc (* 2 idx)) val))
             editable))
         (if (>= n 16)
           (let [nodes (make-array BitmapIndexedNode 32)
                 jdx (util/mask hash shift)
                 noop (aset nodes jdx (->bin-assoc (EMPTY_BIN) edit (+ 5 shift) hash key val added-leaf))
                 j 0]
             (for [i (range 32)
                   j (filter even? (range 32))]
               (when (not (zero? (bit-and (unsigned-bit-shift-right bitmap i) 1)))
                 (if (nil? (aget array j))
                   (aset nodes i (aget array (inc j)))
                   (aset nodes i (node/assoc (EMPTY_BIN)
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
             (doto editable (.binArray! new-array) (.binBitmap! (bit-or (.binBitmap editable) bit)))
             (log/debug (format "->bin-assoc editable array '%s'" (java.util.Arrays/toString (.binArray editable))))
             editable)))))))

;public INode assoc(int shift, int hash, Object key, Object val, Box addedLeaf){
;    int idx = mask(hash, shift);
;    INode node = array[idx];
;    if(node == null)
;      return new ArrayNode(null, count + 1, cloneAndSet(array, idx, BitmapIndexedNode.EMPTY.assoc(shift + 5, hash, key, val, addedLeaf)));
;    INode n = node.assoc(shift + 5, hash, key, val, addedLeaf);
;    if(n == node)
;      return this;
;    return new ArrayNode(null, count, cloneAndSet(array, idx, n));
;}

(defn ->an-assoc
  ([node shift hash key val addedLeaf]
   (log/debug (format "->an-assoc node '%s' shift '%s' hash '%s' key '%s' val '%s' addedLeaf '%s'" node shift hash key val addedLeaf))
   (let [idx (util/mask hash shift)
         node-idx (aget (.anArray node) idx)]
     (if (nil? node-idx)
       (let [new-count (inc (.anCount node))
             new-bin (node/assoc (EMPTY_BIN) (+ 5 shift) hash key val addedLeaf)
             cloned (util/clone-and-set (.anArray node) idx new-bin)]
         (ArrayNode. nil new-count cloned))
       (let [n (node/assoc node (+ 5 shift) hash key val addedLeaf)]
         (if (= n node-idx)
           node
           (ArrayNode. nil (.anCount node) (util/clone-and-set (.anArray node) idx n)))))))
  ([node edit shift hash key val addedLeaf]
   (log/debug (format "->an-assoc node '%s' edit '%s' shift '%s' hash '%s' key '%s' val '%s' addedLeaf '%s'" node edit shift hash key val addedLeaf))))

(defn ->hcn-assoc [node shift hash key val addedLeaf]
  (throw (RuntimeException. "implement me")))

(extend BitmapIndexedNode
  node/Node
  {:assoc #'->bin-assoc})

(extend HashCollisionNode
  node/Node
  {:assoc #'->hcn-assoc})

(extend ArrayNode
  node/Node
  {:assoc #'->an-assoc})