(ns reclojure.lang.util-test
  (:require [midje.sweet :refer :all]
            [reclojure.lang.util :refer :all]))

(defmutable TestType [f1 f2])

(facts "type generation macro"
       (fact "fields are modifiable"
               (.f1 (.f1! (TestType. "a" "b") "c")) => "c"))

(facts "32 way shifting"
       (fact "sample sequences"
             (shift-indeces -375058869) => [[0 0] [1 2] [3 4] [6 6]
                                              [9 8] [10 10] [11 12] [16 14]
                                              [18 16] [21 18] [23 20] [24 22]
                                              [27 24] [29 26] [30 28] [31 30]])
       (fact "sample sequences"
             (shift-indeces 964513957) => [[0 0] [2 2] [5 4] [7 6]
                                             [12 8] [14 10] [16 12] [18 14]
                                             [19 16] [20 18] [21 20] [22 22]
                                             [24 24] [27 26] [28 28] [29 30]]))
