(ns reclojure.lang.hash-map-test
  (:require [midje.sweet :refer :all]
            [reclojure.lang.hash-map :as hm]))

(facts "persistent hash map"
       (let [arraym (.toArray (doto (java.util.ArrayList.) (.add 1) (.add 2)))]
         (fact "creation with one key-value pair"
               (.phmCount (hm/create-persistent arraym)) => 1)))

(facts "trasient hash map"
       (fact "from a persistent hash map"
             (let [thm (hm/create-transient (hm/create-persistent (.toArray (java.util.Collections/emptyList))))]
              (.thmCount thm) => 0)))
