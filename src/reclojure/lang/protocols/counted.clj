(ns reclojure.lang.protocols.counted
  (:refer-clojure :exclude [count]))

(defprotocol Counted
  (count [this]))
