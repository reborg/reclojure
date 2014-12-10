(ns reclojure.lang.bitmap-indexed-node-test
  (:require [midje.sweet :refer :all]
            [reclojure.lang.node :as bin]
            [reclojure.lang.box]
            [reclojure.lang.protocols.node :as node])
  (:import [reclojure.lang.box Box]))

(facts "creating"
       (fact "empty"
             (.binEdit (bin/EMPTY_BIN)) => nil
             (.binBitmap (bin/EMPTY_BIN)) => 0
             (alength (.binArray (bin/EMPTY_BIN))) => 0))

(facts "associng"
       (fact "assoc"
             (nth (.binArray (node/assoc (bin/EMPTY_BIN) 0 695249801 "key" "value" (Box. nil))) 0) => "key"))
