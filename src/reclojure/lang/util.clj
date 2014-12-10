(ns reclojure.lang.util
  (:require [reclojure.lang.protocols.persistent-collection :as pc]
            [reclojure.lang.protocols.hash-eq :as he]
            [clojure.tools.logging :as log]
            [reclojure.lang.numbers :as nums])
  (:import [clojure.lang Numbers Murmur3]
           [reclojure.lang.protocols.persistent_collection PersistentCollection]))


(defn pcequiv [k1 k2]
  (log/debug (format "->pcequiv k1 '%s' k2 '%s'" k1 k2))
  (if (satisfies? k1 pc/PersistentCollection)
    (pc/equiv k1 k2)
    (.equiv k2 k1)))

(defn equiv [k1 k2]
  (log/debug (format "->equiv k1 '%s' type '%s' k2 '%s' type '%s'" k1 (type k1) k2 (type k2)))
  (cond
    (identical? k1 k2) true
    (not= k1 nil) (cond
                    (and (instance? Number k1) (instance? Number k2)) (nums/equal k1 k2)
                    (or (instance? PersistentCollection k1)
                        (instance? PersistentCollection k2)) (pc/equiv k1 k2)
                    :else (.equals k1 k2))
    :else false))

(defn dohasheq [o]
  (.hasheq o))

(defn hasheq [o]
  (int (cond
         (nil? o) 0
         (satisfies? he/HashEq o) (dohasheq o)
         (instance? Number o) (nums/hasheq o)
         (instance? String o) (Murmur3/hashInt (.hashCode o))
         :else (.hashCode o))))

(defmacro defmutable
  "Creates a JavaBean style object on top of a deftype definition."
  [t attrs]
  (let [iface (symbol (str "I" (name t)))
        set-names (map #(symbol (str (name %) "!")) attrs)
        set-declare (map (fn [set-name] `(~set-name [this# v#])) set-names)
        set-impls (map (fn [set-name attr] `(~set-name [this# v#] (set! ~attr v#) this#)) set-names attrs)
        get-declare (map (fn [attr] `(~attr [this#])) attrs)
        get-impls (map (fn [attr] `(~attr [this#] ~attr)) attrs)
        annotated-attrs (vec (map (fn [name] (with-meta name (assoc (meta name) :volatile-mutable true))) attrs))]
    `(do
       (defprotocol ~iface
         ~@get-declare
         ~@set-declare)
       (deftype ~t ~annotated-attrs
         Object
         (toString [this#] (pr-str ~attrs))
         ~iface
         ~@get-impls
         ~@set-impls))))

(defn aprint [o]
  "If object contains an array or object is an array, print its content."
  (try
    (java.util.Arrays/toString (.array o))
    (catch Exception e (java.util.Arrays/toString o))))

(defn mask [hash shift]
  (bit-and (clojure.lang.Numbers/unsignedShiftRightInt hash shift) 0x01f))

(defn clone-and-set
  ([array idx obj]
   (log/debug (format "clone-and-set 3 array '%s' idx '%s' obj '%s'" array idx obj))
   (log/debug (format "want to store object of type %s into array of type %s" (type obj) (type (aclone array))))
   (doto (aclone array) (aset idx obj)))
  ([array idx a jdx b]
   (log/debug (format "clone-and-set 5 array '%s' idx '%s' a '%s' jdx '%s' b '%s'" array idx a jdx b))
   (doto (aclone array) (aset idx a) (aset jdx b))))
