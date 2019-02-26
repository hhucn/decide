(ns decidotron.server-components.budgets)

(defn preferences-by-slug [state slug]
  (->> (vals state)
    (map slug)
    :preferences
    (map :dbas.position/id)))

(defn score [max-n user-preferences]
  (map-indexed (fn [i v] {v (- max-n i)}) user-preferences))

(comment
  (score 5 [83 42])
  (apply max (map count [[83 42] [83 42] [83 42 666]])))

(defn borda-budget [preferences budget costs]
  (let [max-n        (apply max (map count preferences))
        union        (apply merge-with + (mapcat (partial score max-n) preferences))
        global-order (map first (sort-by second > union))]
    (loop [r global-order
           b []]
      (if (not-empty r)
        (if (>= budget (reduce + (map costs (cons (first r) b))))
          (recur (rest r) (concat b [(first r)]))
          (recur (rest r) b))
        b))))

(comment
  (score 5 [83 42])
  (apply max (map count [[83 42] [83 42] [83 42 666]]))

  (apply merge-with + (mapcat (partial score 3) [[83 42] [83 666] [83 42 666]]))



  (def costs {83 5 42 6 666 4})
  (def budget 10)

  (borda-budget [[83 42] [83 666] [83 42 666]] budget costs))