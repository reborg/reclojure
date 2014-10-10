(ns reclojure.lang.numbers
  (:import [clojure.lang Murmur3]))

(defn equal [n1 n2])

(defn hasheq [x]
  (let [xc (.getClass x)]
    (cond (or (= (type xc) java.lang.Long)
              (= (type xc) java.lang.Short)
              (= (type xc) java.lang.Integer)
              (= (type xc) java.lang.Byte)
              (and (= (type xc) java.math.BigInteger)
                   (<= x java.lang.Long/MAX_VALUE)
                   (>= x java.lang.Long/MIN_VALUE)))
          (clojure.lang.Murmur3/hashLong (.longValue x))
          (= (type xc) java.math.BigDecimal)
          (when (= xc java.math.BigDecimal)
            (if (clojure.lang.Numbers/isZero x)
              (.hashCode (java.math.BigDecimal/ZERO))
              (.hashCode (.stripTrailingZeros x))))
          :else
          (.hashCode x))))
