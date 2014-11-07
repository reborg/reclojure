(ns reclojure.lang.util
  (:require [reclojure.lang.protocols.persistent-collection :as pc]
            [reclojure.lang.protocols.hash-eq :as he]
            [reclojure.lang.numbers :as nums])
  (:import [clojure.lang Numbers Murmur3]))

(defn pcequiv [k1 k2]
  (if (satisfies? k1 pc/PersistentCollection)
    (pc/equiv k1 k2)
    (.equiv k2 k1)))

(defn equiv [k1 k2]
  (cond
    (identical? k1 k2) true
    (not= k1 nil) (cond
                    (and (instance? k1 Number) (instance? k2 Number)) (nums/equal k1 k2)
                    (or (satisfies? k1 pc/PersistentCollection) (satisfies? k2 pc/PersistentCollection)) (pc/equiv k1 k2)
                    :else (.equals k1 k2))
    :else false))

(defn dohasheq [o]
  (.hasheq o))

(defn hasheq [o]
  (cond
    (nil? o) 0
    (satisfies? he/HashEq o) (dohasheq o)
    (instance? Number o) (nums/hasheq o)
    (instance? String o) (Murmur3/hashInt (.hashCode o))
    :else (.hashCode o)))

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
