(ns reclojure.lang.protocols.persistent-map-test
  (:require [midje.sweet :refer :all]
            [reclojure.lang.protocols.persistent-map :as pm]))

(facts "output to string rapresentation"
       (fact "string again"
             (pm/->toString {:a 1 :b 2}) => "{:b 2, :a 1}"))
