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

(defn identity-score [votes]
  (zipmap votes votes))

(defn neg-id-score [votes]
  (zipmap votes (- votes)))

(defn find-max-n
  "Returns the max number of votes a user has submitted."
  [preferences]
  (if (empty? preferences)
    0
    (apply max (map count preferences))))

(defn- tag-scores [tag scores]
  (into {} (map (fn [[k v]] {k {tag v}}) scores)))

(defn form-results [results]
  (map (partial zipmap [:proposal :scores]) results))

(defn- score-fns->tagging-fns [score-fns]
  (map (fn score-fn->tagging-fn [[tag score-fn]]
         (fn [votes] (tag-scores tag (score-fn votes))))
    score-fns))

(defn score-single
  "Scores the votes of a single participant with the score-fns."
  [votes score-fns]
  (apply merge-with merge ((apply juxt (score-fns->tagging-fns score-fns)) votes)))

(apply merge-with merge ((apply juxt (score-fns->tagging-fns {:approval approval-score :identity identity-score})) [:a :b :c]))


(score-single [:a :b :c]
  {:approval approval-score
   :borda    (partial borda-score 5)
   :identity identity-score})

(defn score
  "Calculates scores for a number of user votes.

  Gets the votes as a list of lists of elements (e.g. ids) and applies the score functions on them.
  Returns the list of lists of all scored elements."
  ([votes score-fns]
   (score votes {} score-fns))
  ([votes init score-fns]
   (conj
     (map #(score-single % score-fns) votes)
     init)))

(defn- aggregate-with [aggs s1 s2]
  (merge-with #(apply %1 (first %&)) aggs (merge-with vector s1 s2)))

(defn aggregate
  "Aggregates scored votes with help of the aggregation functions.

  Most of the time, you just want to add them.
  "
  ([scores agg-fns] (aggregate scores {} agg-fns))
  ([scores init agg-fns]
   (apply merge-with (partial aggregate-with agg-fns) init scores)))

(defn sort-aggregations [aggs score-keys]
  (reverse (sort-by (comp (apply juxt score-keys) second) aggs)))

(defn select-winners [sorted-proposals costs budget]
  (loop [rest-budget budget
         [[proposal scores] & rest-proposals] sorted-proposals
         winners     []]
    (if proposal
      (let [cost (costs proposal)]
        (if (and (< cost rest-budget) (-> scores :borda pos?)) ; only allow proposals which fit in the budget and have at least 1 vote
          (recur (- rest-budget cost) rest-proposals (conj winners [proposal scores]))
          (recur rest-budget rest-proposals winners)))
      {:winners (form-results winners)
       :losers  (form-results (remove (set winners) sorted-proposals))})))

(defn- initial-scores [proposals init-fn]
  (into {} (map #(vector % (init-fn %))) proposals))

(defn borda-budget [votes budget costs]
  (-> votes
    (score
      (initial-scores (keys costs) (fn [v] (hash-map :borda 0 :approval 0 :id v))) ; else proposals that are not once voted for wouldn't occur in loosers.
      {:borda    (partial borda-score (find-max-n votes))
       :approval approval-score
       :id       identity-score})
    (aggregate {:borda    +
                :approval +
                :id       (fn [a _] a)})
    (sort-aggregations [:borda :approval])
    (select-winners costs budget)))
