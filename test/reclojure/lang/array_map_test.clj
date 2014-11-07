(ns reclojure.lang.array-map-test
  (:require [midje.sweet :refer :all]
            [reclojure.lang.protocols.persistent-vector :as pv]
            [reclojure.lang.protocols.transient-map :as tm]
            [reclojure.lang.protocols.persistent-map :as pm]
            [reclojure.lang.array-map :as am]))

;;#_(facts "persistent"
;;       (fact "consing a HashMap$Entry in"
;;             (let [m (doto (java.util.HashMap.) (.put "a" 1))
;;                   e (first (.entrySet m))
;;                   pam (am/create-persistent m)]
;;               (pm/cons pam e) => 2)))
;;
;;(facts "transient"
;;       (fact "create from a java array"
;;             (let [m (am/create-transient (to-array [1 2 3]))]
;;               (:len m) => 3
;;               (:owner m) => (Thread/currentThread))))
;;
;;(facts "transient read"
;;       (fact "access to key skips odd indexes"
;;             (am/tam-index-of "b" (am/create-transient (to-array ["a" "X" "b" "X"]))) => 2
;;             (am/tam-index-of "X" (am/create-transient (to-array ["a" "X"]))) => -1
;;             (am/tam-index-of "X" (am/create-transient (to-array ["a" "X" "b" "X"]))) => -1))
;;
;;(facts "transient map default implementation"
;;       (fact "it can conj a new object into it"
;;             (let [tam (am/create-transient (to-array [1 "a" 2 "b"]))
;;                   v (reify
;;                       pv/PersistentVector
;;                       (pv/length [this] 2)
;;                       (pv/assocN [this i val] nil)
;;                       (pv/cons [this o] nil)
;;                       (pv/count [this] 2)
;;                       (pv/nth [this i] (if (= 0 i) 3 "c"))
;;                       (pv/nth [this i notFound] (if (= 0 i) 3 "c")))]
;;               (tm/conj tam v) => (am/create-transient (to-array [1 "a" 2 "b" 3 "c"])))))
