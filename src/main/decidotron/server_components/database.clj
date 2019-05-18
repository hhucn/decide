(ns decidotron.server-components.database
  (:require [korma.core :as k :refer [select where with fields subselect]]
            [decidotron.server-components.config :refer [config]]
            [clj-time.core :as t]
            [clj-time.coerce :as tc]
            [mount.core :refer [defstate]]
            [korma.db :as kdb]
            [fulcro.logging :as log]))

(defstate db
  :start (let [db (kdb/postgres (get-in config [:dbas :database]))]
           (kdb/defdb kdatabase db)
           db))

(declare cost textversion issue statement position decision-process)

(k/defentity cost
  (k/pk :position_id)
  (k/table :decidotron_position_cost)
  (k/entity-fields :cost))

(k/defentity textversion
  (k/pk :uid)
  (k/table
    (k/subselect
      [(k/subselect "textversions"
         (fields :* (k/raw "rank() over (partition by textversions.statement_uid order by textversions.timestamp DESC)"))
         (where {:is_disabled false})) :ranked]
      (where {:ranked.rank 1}))
    :textversions)
  (k/entity-fields :content))

(k/defentity issue
  (k/pk :uid)
  (k/table :issues)
  (k/has-one decision-process {:fk :issue_id})
  (k/many-to-many statement :statement_to_issue {:lfk :statement_to_issue.issue_uid :rfk :statement_to_issue.statement_uid}))

(k/defentity statement
  (k/pk :uid)
  (k/table :statements)
  (k/has-one cost {:fk :position_id})
  (k/has-one textversion {:fk :statement_uid})
  (k/many-to-many issue :statement_to_issue {:rfk :statement_to_issue.issue_uid
                                             :lfk :statement_to_issue.statement_uid}))

(k/defentity position
  (k/pk :uid)
  (k/table (k/raw "(SELECT * FROM statements WHERE statements.is_position = true)") :position)
  (k/has-one cost {:fk :position_id})
  (k/has-one textversion {:fk :statement_uid})
  (k/many-to-many issue :statement_to_issue {:rfk :statement_to_issue.issue_uid
                                             :lfk :statement_to_issue.statement_uid}))

(k/defentity decision-process
  (k/pk :issue_id)
  (k/table :decidotron_decision_process)
  (k/belongs-to issue {:fk :issue_id}))

(defn positions-by-ids
  [ids]
  {:pre  [(do (log/trace "Query for the ids" (vec ids)) true)]
   :post [(do (log/trace "Got results" (with-out-str (clojure.pprint/pprint (vec %)))) true)]}
  (for [{:keys [uid text cost is_disabled]}
        (k/select statement
          (k/fields :* [:textversions.content :text])
          (k/limit (count ids))
          (k/where (and (in :uid ids) (= :is_position true)))
          (k/with cost)
          (k/join textversion))]
    #:dbas.position{:id        uid
                    :text      text
                    :cost      cost
                    :disabled? is_disabled}))

(defn position-by-id [id]
  (first (positions-by-ids [id])))

(defn positions-for-issue [slug]
  (for [{:keys [uid cost text]}
        (:statements (first (k/select issue
                              (k/limit 1)
                              (k/with statement
                                (k/fields :uid :decidotron_position_cost.cost [:textversions.content :text])
                                (k/join textversion)
                                (k/with cost)
                                (k/where (and
                                           (= :is_position true)
                                           (= :is_disabled false))))
                              (k/where (and (= :slug slug))))))]
    #:dbas.position{:id uid :cost cost :text text}))

(defn pro-con-for-positions [position-ids]
  (select [textversion :tv]
    (k/fields [:statements.stmt_uid :position_uid]
      :statements.uid [:tv.content :text] :statements.is_supportive
      :arg_uid)
    (k/join :inner
      [(k/subselect "premises"
         (k/fields [:statement_uid :uid] :arguments.is_supportive :arguments.stmt_uid :arguments.arg_uid)
         (k/join :inner
           [(k/subselect "arguments"
              (k/fields :premisegroup_uid :is_supportive
                [:conclusion_uid :stmt_uid] [:uid :arg_uid])
              (k/where (and
                         (in :conclusion_uid (vec position-ids))
                         (= :is_disabled false))))
            :arguments]
           (= :arguments.premisegroup_uid :premises.premisegroup_uid)))
       :statements]
      (= :statements.uid :tv.statement_uid))))

(defn pro-con-for-position [position-id]
  (pro-con-for-positions [position-id]))

(defn get-issue [slug]
  (let [{:keys [votes_end votes_start slug uid title long_info currency_symbol positions_end info budget statements]}
        (first
          (k/select issue
            (k/fields :uid :title :slug :info :long_info)
            (k/limit 1)
            (k/with statement
              (k/fields :* [:textversions.content :text])
              (k/join textversion)
              (k/with cost)
              (k/where {:is_position true
                        :is_disabled false}))
            (k/where (and (= :slug slug)))
            (k/with decision-process)))]
    #:dbas.issue{:id              uid
                 :title           title
                 :slug            slug
                 :info            info
                 :long-info       long_info
                 :positions-end   (tc/to-date (tc/from-sql-date positions_end))
                 :votes-end       (tc/to-date (tc/from-sql-date votes_end))
                 :votes-start     (tc/to-date (tc/from-sql-date votes_start))
                 :budget          budget
                 :currency-symbol currency_symbol
                 :positions       (for [{:keys [uid cost text]} statements]
                                    #:dbas.position{:id uid :cost cost :text text})}))

(defn get-costs
  "Given an issue, returns a mapping from the position-ids to their costs.

  ```clojure
  (-> \"was-sollen-wir-mit-20-000eur-anfangen\" get-issue get-costs)

  ==> {83 400000, 86 200000, 88 999900, 93 1000099}
  ```
  "
  [issue]
  (into {} (map (fn [{:dbas.position/keys [id cost]}] [id cost]))
    (:dbas.issue/positions issue)))

(defn- position-ids
  "Returns all not disabled position ids from an issue."
  [slug]
  (->> (select issue (where {:slug slug})
         (with statement
           (where {:is_disabled false :is_position true})))
    first :statements (map :uid) set))

(defn filter-disabled-positions
  "Filters out all positions which are disabled do not belong to the issue."
  [slug positions]
  (filter #((position-ids slug) (:dbas.position/id %)) positions))

(defn filter-disabled-positions-fn
  "Filters out all positions which are disabled do not belong to the issue."
  [slug]
  (let [ids (position-ids slug)]
    (fn [positions]
      (filter #(ids (:dbas.position/id %)) positions))))

(defn- votes-start-end [slug]
  (let [[{:keys [votes_start votes_end]}]
        (select issue (where {:slug slug})
          (k/limit 1)
          (with decision-process
            (fields :votes_start :votes_end :issue_id)))]
    {:start (tc/from-sql-date votes_start)
     :end   (tc/from-sql-date votes_end)}))

(defn allow-voting?
  "Returns true if 'now' is in the voting phase."
  [slug]
  (let [{:keys [start end]} (votes-start-end slug)]
    (t/within?
      (or start (t/epoch))
      (or end (-> 1 t/hours t/from-now))
      (t/now))))

(defn show-results?
  "Returns whether the results for the issue with the given slug should be shown."
  [slug]
  (if-let [end (:end (votes-start-end slug))]
    (t/after? (t/now) end)
    true))