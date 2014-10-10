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

;public static int hasheq(Object o){
;  if(o == null)
;    return 0;
;  if(o instanceof IHashEq)
;    return dohasheq((IHashEq) o);
;  if(o instanceof Number)
;    return Numbers.hasheq((Number)o);
;  if(o instanceof String)
;    return Murmur3.hashInt(o.hashCode());
;  return o.hashCode();
;}
