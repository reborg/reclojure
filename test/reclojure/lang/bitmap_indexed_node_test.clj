(ns reclojure.lang.bitmap-indexed-node-test
  (:require [midje.sweet :refer :all]
            [reclojure.lang.bitmap-indexed-node :as bin]
            [reclojure.lang.box]
            [reclojure.lang.protocols.node :as node])
  (:import [reclojure.lang.box Box]))

(facts "creating"
       (fact "empty"
             (:edit bin/EMPTY) => nil
             (:bitmap bin/EMPTY) => 0
             (alength (:array bin/EMPTY)) => 0))

(facts "associng"
       (fact "assoc"
             (nth (:array (node/assoc bin/EMPTY 0 695249801 "key" "value" (Box. nil))) 0) => "key"))
