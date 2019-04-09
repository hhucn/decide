(ns decidotron.server-components.budgets)

(defn approval-score
  [votes]
  (zipmap votes (repeat 1)))

(defn borda-score
  "Given a top-value max-n, return the borda scores for this vote set
  Example: max-n = 5
  votes = [:a :b :c]

  => {:a 5 :b 4 :c 3}
  "

  [max-n votes]
  (into {} (map-indexed (fn [i v] [v (- max-n i)]) votes)))

(defn find-max-n
  "Returns the max number of votes a user has submitted."
  [preferences]
  (if (empty? preferences)
    0
    (apply max (map count preferences))))

(defn initial-scores
  "Returns a map which is initialized with 0 for every proposal."
  [costs]
  (zipmap (keys costs) (repeat 0)))

(defn- tag-scores [tag scores]
  (into {} (map (fn [[k v]] {k {tag v}}) scores)))

(defn form-results [results]
  (map (partial zipmap [:proposal :scores]) results))

(defn borda-budget [votes budget costs]
  (let [borda-score       (partial borda-score (find-max-n votes))
        global-merger     (fn global-merger [scores] (apply merge-with + (initial-scores costs) scores))
        global-scores     (merge-with merge
                            (->> votes (map borda-score) global-merger (tag-scores :borda))
                            (->> votes (map approval-score) global-merger (tag-scores :approval)))
        ordered-proposals (reverse (sort-by (comp (juxt :borda :approval) second) global-scores))]
    (loop [rest-budget budget
           [[proposal scores] & rest-proposals] ordered-proposals
           winners     []]
      (if proposal
        (let [cost (costs proposal)]
          (if (and (< cost rest-budget) (-> scores :borda pos?)) ; only allow proposals which fit in the budget and have at least 1 vote
            (recur (- rest-budget cost) rest-proposals (conj winners [proposal scores]))
            (recur rest-budget rest-proposals winners)))
        {:winners (form-results winners)
         :losers  (form-results (remove (set winners) ordered-proposals))}))))