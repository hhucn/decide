(ns decide.model.argument
  (:require
    [datahike.api :as d]
    [datahike.core :refer [squuid]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [decide.util :as util]))

(defn pro? [t] (= t :pro))

(s/def :argument/id (s/or :uuid uuid? :str string?))
(s/def :argument/text string?)
(s/def :argument/type #{:pro :con})
(s/def :argument/subtype #{:support :undercut :undermine})
(s/def :argument/author (s/tuple #{:account/id} :account/id))
(s/def :argument/parent (s/tuple any? :argument/id))
(s/def ::new-argument
  (s/keys :req [:argument/id :argument/text :argument/type :argument/subtype :argument/author
                :argument/parent]))

(defmutation new-argument [{:keys [connection AUTH/account-id] :as env}
                           {:argument/keys [id text type subtype parent author] :as params}]
  {::pc/params [:argument/id :argument/text :argument/type :argument/subtype
                :argument/author :argument/parent]
   ::pc/output [:argument/id]
   ::s/params  ::new-argument}
  (if (and account-id (= account-id (second author)))
    (if (s/valid? ::new-argument params)
      (let [real-id   (squuid)
            _         (log/debug "New UUID " (str real-id))
            tx-report (d/transact connection
                        [{:db/id            "new-argument"
                          :argument/id      (str real-id)
                          :argument/text    text
                          :argument/type    type
                          :argument/subtype subtype
                          :argument/author  author}
                         [:db/add [:argument/id (-> parent second str)]
                          (if (pro? type) :argument/pros :argument/cons) "new-argument"]])]
        {:tempids     {id real-id}
         ::p/env      (assoc env :db (:db-after tx-report))
         :argument/id real-id})
      (log/spy :error (s/explain ::new-argument params)))))


(defmutation retract-argument [{:keys [connection]} {:keys [argument/id]}]
  {::pc/params [:argument/id]}
  (when (uuid? id)
    (d/transact connection [[:db.fn/retractEntity [:argument/id (str id)]]]))
  [])

(defresolver resolve-argument [{:keys [db]} {:keys [argument/id]}]
  {::pc/input  #{:argument/id}
   ::pc/output [:argument/id
                :argument/text
                :argument/type
                :argument/subtype
                {:argument/author [:account/id]}
                {:argument/pros [:argument/id]}
                {:argument/cons [:argument/id]}]}
  (let [result (d/pull db '[:argument/id
                            :argument/text
                            :argument/type
                            :argument/subtype
                            {:argument/author [:account/id]}
                            {:argument/pros [:argument/id]}
                            {:argument/cons [:argument/id]}]
                 [:argument/id (str id)])]
    (-> result
      util/str-id->uuid-id
      (update :argument/pros (partial mapv util/str-id->uuid-id))
      (update :argument/cons (partial mapv util/str-id->uuid-id)))))

(def resolvers [new-argument resolve-argument retract-argument])
