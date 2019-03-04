(ns decidotron.server-components.budgets)

(defn preferences-by-slug [state slug]
  (->> (vals state)
    (map slug)
    :preferences
    (map :dbas.position/id)))

(defn score
  "Given a top-value max-n, return the borda scores for this vote set
  Example: max-n = 5
  votes = [:a :b :c]

  => {:a 5 :b 4 :c 3}
  "

  [max-n votes]
  (map-indexed (fn [i v] {v (- max-n i)}) votes))

(comment
  (score 5 [83 42])
  (apply max (map count [[83 42] [83 42] [83 42 666]])))


(defn find-max-n
  "Returns the max number of votes a user has submitted."
  [preferences]
  (apply max (map count preferences)))

(comment (find-max-n [[83 42] [83 666] [83 42 666]])
  ((partial score 3) [83 42]))

(defn borda-budget [votes budget costs]
  (let [max-n             (find-max-n votes)
        score             (partial score max-n)
        union             (apply merge-with +
                            (zipmap (keys costs) (repeat 0)) ; initialise
                            (mapcat score votes))           ; get the global map: position -> score
        ordered-positions (map (fn [[id score]] {:dbas.position/id id
                                                 :score            score})
                            (sort-by second > union))       ; Sort by score and take only position
        in-budget?        (fn [extension] (>= budget (reduce + (map (comp costs :dbas.position/id) extension))))]
    (loop [[position & r] ordered-positions
           winners []]                                      ; ATTENTION! This has to be a vector, or else `conj` prepends...
      (if position
        (let [extension (conj winners position)]
          (recur r
            (if (and (in-budget? extension) (pos? (:score position)))
              extension                                     ; extend winners
              winners)))                                    ; skip this position
        {:winners winners
         :losers  (remove (set winners) ordered-positions)}))))

(comment
  (score 5 [83 42])
  (apply max (map count [[83 42] [83 42] [83 42 666]]))

  (apply merge-with +
    (zipmap (keys {83 5 42 6 666 4 123 2}) (repeat 0))      ; initialise
    (mapcat (partial score 3) [[83 42] [83 666] [83 42 666]]))

  (sort-by second > (apply merge-with + (mapcat (partial score 3) [[83 42] [83 666] [83 42 666]])))



  (def costs {83 5 42 6 666 4 123 2})
  (def budget 10)

  (apply merge-with +
    (zipmap (keys costs) (repeat 0))                        ; initialise
    (mapcat score votes))

  (borda-budget [[83 42] [83 666] [83 42 666]] budget costs))