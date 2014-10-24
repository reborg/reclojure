(ns reclojure.lang.util-test
  (:require [midje.sweet :refer :all]
            [reclojure.lang.util :refer :all]))

(defmutable TestType [f1 f2])

(facts "type generation macro"
       (fact "fields are modifiable"
               (.f1 (.f1! (TestType. "a" "b") "c")) => "c"))
