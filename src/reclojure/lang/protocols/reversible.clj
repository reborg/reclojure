(ns reclojure.lang.protocols.counted
  (:refer-clojure :exclude [rseq]))

(defprotocol Reversible
  (rseq [this]))
