(ns decide.util)

(defn prefixed-keyfn [prefix f]
  (comp (partial str prefix "/") f))