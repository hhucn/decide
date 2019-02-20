(ns decidotron.database.models
  (:require [korma.core :as k]))

(def positions-query
  "
  SELECT statements.uid as id, dpc.cost, tv.content as text
  FROM (SELECT issue_uid, statements.uid
         FROM statements
         JOIN (SELECT statement_uid, issue_uid
                FROM issues
                JOIN statement_to_issue s on issues.uid = s.issue_uid
                WHERE issues.slug = ?) as sti
         on statements.uid = sti.statement_uid
         WHERE is_position
         and not is_disabled) statements
  JOIN decidotron_position_cost as dpc on statements.uid = dpc.position_id
  JOIN textversions as tv on statements.uid = tv.statement_uid;
  ")

(defn index-by [l k]
  (apply hash-map (mapcat #(vector (k %) %) l)))

(defn positions-for-issue [issue-slug]
  (k/exec-raw [positions-query [issue-slug]] :results))

(comment (positions-for-issue "was-sollen-wir-mit-20-000eur-anfangen"))

(defn pro-con-for-positions [position-ids]
  (k/select ["textversions" :tv]
    (k/fields [:statements.stmt_uid :position_uid] :statements.uid [:tv.content :text] :statements.is_supportive)
    (k/join :inner
      [(k/subselect "premises"
         (k/fields [:statement_uid :uid] :arguments.is_supportive :arguments.stmt_uid)
         (k/join :inner
           [(k/subselect "arguments"
              (k/fields :premisegroup_uid :is_supportive [:conclusion_uid :stmt_uid])
              (k/where (and
                         (in :conclusion_uid (vec position-ids))
                         (= :is_disabled false))))
            :arguments]
           (= :arguments.premisegroup_uid :premises.premisegroup_uid)))
       :statements]
      (= :statements.uid :tv.statement_uid))))

(defn pro-con-for-position [position-id]
  (pro-con-for-positions [position-id]))

(comment
  (pro-con-for-position 85)
  (pro-con-for-positions [83, 85]))