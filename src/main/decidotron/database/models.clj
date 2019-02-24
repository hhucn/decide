(ns decidotron.database.models
  (:require [korma.core :as k]))


(declare cost textversion issue statement position)

(k/defentity cost
  (k/pk :position_id)
  (k/table :decidotron_position_cost)
  (k/entity-fields :cost))

(k/defentity textversion
  (k/pk :uid)
  (k/table :textversions)
  (k/entity-fields :content))

(k/defentity issue
  (k/pk :uid)
  (k/table :issues)
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

(defn positions-by-ids [ids]
  (for [{:keys [uid content cost]}
        (k/select statement
          (k/where (and (in :uid ids) (= :is_position true)))
          (k/with cost)
          (k/with textversion))]
    #:dbas.position{:id uid
                    :text content
                    :cost cost}))

(defn position-by-id [id]
  (first (positions-by-ids [id])))

(defn positions-for-issue [slug]
  (for [{:keys [uid cost content]}
        (:statements (first (k/select issue
                              (k/with statement
                                (k/with textversion)
                                (k/with cost)
                                (k/where (and
                                           (= :is_position true)
                                           (= :is_disabled false))))
                              (k/where (and (= :slug slug))))))]
     #:dbas.position{:id uid :cost cost :text content}))

(defn index-by [l k]
  (apply hash-map (mapcat #(vector (k %) %) l)))

(defn pro-con-for-positions [position-ids]
  (k/select ["textversions" :tv]
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

(comment
  (pro-con-for-position 85)
  (pro-con-for-positions [83, 85]))