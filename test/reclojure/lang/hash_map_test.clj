(ns reclojure.lang.hash-map-test
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [reclojure.lang.hash-map :as hm]
            [reclojure.lang.protocols.persistent-map :as pm]))

(facts "persistent hash map"
       (let [arraym (.toArray (doto (java.util.ArrayList.) (.add 1) (.add 2)))]
         (fact "creation with one key-value pair"
               (.phmCount (hm/create-persistent arraym)) => 1)))

(facts "trasient hash map"
       (fact "from a persistent hash map"
             (let [thm (hm/create-transient (hm/create-persistent (.toArray (java.util.Collections/emptyList))))]
              (.thmCount thm) => 0)))

;(facts "porting of PersistentHashMap::main test method"
;       (let [f (slurp (io/resource "reclojure/lang/bitmap_indexed_node.clj"))
;             words (remove s/blank? (s/split f #"\W"))
;             phm (hm/EMPTY)]
;         (doall (map #(pm/assoc phm % %) words))
;         (first words) => "aa"))
