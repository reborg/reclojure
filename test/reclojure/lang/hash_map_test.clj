(ns reclojure.lang.hash-map-test
  (:require [midje.sweet :refer :all]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [reclojure.lang.hash-map :as hm]
            [reclojure.lang.protocols.persistent-map :as pm]))

(def words
  (-> (slurp (io/resource "test-words.txt"))
      (s/split #"\W")))

(facts "persistent hash map"
       (let [arraym (.toArray (doto (java.util.ArrayList.) (.add 1) (.add 2)))]
         (fact "creation with one key-value pair"
               (.phmCount (hm/create-persistent arraym)) => 1)))

(facts "transient hash map"
       (fact "from a persistent hash map"
             (let [thm (hm/create-transient (hm/create-persistent (.toArray (java.util.Collections/emptyList))))]
              (.thmCount thm) => 0)))

(fact "assoc a few"
      (.phmCount (reduce #(pm/assoc %1 %2 %2) (hm/EMPTY) ["ns" "reclojure" "langaaaaaaa"])) => 3)

(def trace (atom ""))

(defn- map-simple-names [array]
  (java.util.Arrays/toString (amap array idx ret (. (. (or (aget array idx) "") getClass) getSimpleName))))

(defn- collect-trace [node key bit]
  (let [array (.binArray node)
        next-trace (str "key " key
                        " bitmap " (.binBitmap node)
                        " bit " bit
                        " types " (map-simple-names array) "\n")]
    (swap! trace str next-trace)))

(facts "structural and type verification"
       (with-redefs [clojure.tools.logging/trace collect-trace]
         (let [_ (require '[reclojure.lang.node] :reload) ; log/trace macro-exp need to happen after
               _ (reset! trace "")
               res (reduce #(pm/assoc %1 %2 %2) (hm/EMPTY) words)]

           (fact "persistent hash contains right number of entries"
                 (.phmCount res) => 544)
           (fact "diffing with original Java code trace"
                 (count (slurp (io/resource "java-bin-trace.txt"))) => (count @trace)
                 (slurp (io/resource "java-bin-trace.txt")) => @trace))))

(facts "removing"
       (let [snapshot (reduce #(pm/assoc %1 %2 %2) (hm/EMPTY) ["ns" "reclojure" "langaaaaaaa"])]
         (fact "should decrease in size"
               (.phmCount (pm/without snapshot "ns")) => (dec (.phmCount snapshot))
               (.phmCount snapshot) => 3)
         (fact "removed element is gooone"
               (into [] (.binArray (.phmRoot (pm/without snapshot "ns")))) => ["reclojure" "reclojure" "langaaaaaaa" "langaaaaaaa"])))

(fact "removing from big map until exhausted"
      (let [res (reduce #(pm/assoc %1 %2 %2) (hm/EMPTY) words)
            removed (reduce #(pm/without %1 %2) res (distinct words))]
        (.phmCount res) => (count (distinct words))
        (.phmCount removed) => 0))

(fact "lookup"
      (let [res (reduce #(pm/assoc %1 %2 %2) (hm/EMPTY) words)
            all (map #(pm/containsKey res %) words)]
        (every? true? all) => true))

;(def f (slurp (io/resource "test-words.txt")))
;(def words (distinct (remove s/blank? (s/split f #"\W"))))
;(def res (reduce #(pm/assoc %1 %2 %2) (hm/EMPTY) words))
