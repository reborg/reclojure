(ns reclojure.lang.box)

(definterface Holder
  (update [v]))

(deftype Box [^:unsynchronized-mutable value]
  clojure.lang.IDeref
  (deref [this] value)

  Holder
  (update [this v] (set! value v)))
