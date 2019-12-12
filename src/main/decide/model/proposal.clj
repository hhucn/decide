(ns decide.model.proposal
  (:require
    [datahike.api :as d]
    [datahike.core :refer [squuid db? conn? datom?]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [decide.util :as util]
    [decide.model.process :as process])
  (:import (java.time Instant)))

(s/def :proposal/id any?)
(s/def :proposal/cost string?)
(s/def :proposal/details string?)
(s/def :argument/text string?)
(s/def ::new-proposal (s/keys :req [:proposal/id :proposal/cost :proposal/details
                                    :argument/text]))

(s/def :vote/utility int?)

(s/def :datomic.tx-report/db-before db?)
(s/def :datomic.tx-report/db-after db?)
(s/def :datomic.tx-report/tx-data (s/coll-of datom?))
(s/def :datomic.tx-report/tempids map?)
(s/def :datomic.tx-report/tx-meta any?)
(s/def ::d/tx-report (s/keys :req-un [:datomic.tx-report/db-before ; db value before transaction
                                      :datomic.tx-report/db-after ; db value after transaction
                                      :datomic.tx-report/tx-data ; plain datoms that were added/retracted from db-before
                                      :datomic.tx-report/tempids ; map of tempid from tx-data => assigned entid in db-after
                                      :datomic.tx-report/tx-meta]))

(>defn add-proposal! [conn {:proposal/keys [id cost details]
                            :argument/keys [text author created-when]}]
  [conn? any? => ::d/tx-report]
  (d/transact conn
    [(merge
       #:proposal{:db/id       "new-proposal"
                  :argument/id (str id)
                  :cost        cost
                  :subtext     details}
       #:argument{:text         text
                  :type         :position
                  :subtype      :position
                  :author       author
                  :created-when (str created-when)})]))

(defmutation new-proposal [{:keys [connection AUTH/account-id] :as env}
                           {:proposal/keys [id cost details]
                            :argument/keys [text]}]
  {::pc/params [:proposal/id :argument/text :proposal/cost :proposal/details]
   ::pc/output [:proposal/id]
   ::s/params  ::new-proposal}
  (when account-id
    (let [real-id   (squuid)
          tx-report (add-proposal! connection
                      {:proposal/id           real-id
                       :proposal/cost         (Long/parseLong cost)
                       :proposal/details      details
                       :argument/text         text
                       :argument/author       [:account/id account-id]
                       :argument/created-when (Instant/now)})]
      (log/debug "New UUID for proposal" real-id)
      {:proposal/id real-id
       ::p/env      (assoc env :db (:db-after tx-report))
       :tempids     {id real-id}})))


(s/def ::upsert-vote (s/keys :req [:proposal/id :account/id :vote/utility]))

(>defn upsert-vote! [conn {proposal-id :proposal/id
                           account     :account/id
                           utility     :vote/utility}]
  [conn? ::upsert-vote => ::d/tx-report]
  (d/transact conn
    [{:db/id                 "vote"
      :vote/account+proposal (str account "+" proposal-id)
      :vote/proposal         [:argument/id (str proposal-id)]
      :vote/utility          utility}
     [:db/add [:account/id account] :account/votes "vote"]]))


(defmutation set-vote [{:keys [connection AUTH/account-id] :as env}
                       {proposal-id :proposal/id
                        account     :account/id :as params}]
  {::pc/params [:proposal/id :account/id :account/id]
   ::pc/output [:proposal/id]
   ::s/params  ::upsert-vote}
  (if (and account-id (= account-id account))
    (let [{:keys [db-after]} (upsert-vote! connection params)]
      {:proposal/id proposal-id
       ::p/env      (assoc env :db db-after)})
    (throw (ex-info "Authorization failed" {:account/id account-id}))))

(defresolver resolve-votes [{:keys [db AUTH/account-id]} {proposal :proposal/id}]
  {::pc/input  #{:proposal/id}
   ::pc/output [:vote/utility]}
  (if account-id
    (d/pull db [:vote/utility] [:vote/account+proposal (str account-id "+" proposal)])
    (throw (ex-info "No Authorization"
             {:cause      :AUTH/not-logged-in
              :account/id account-id}))))

(defresolver resolve-proposal [{:keys [db]} {:keys [proposal/id]}]
  {::pc/input  #{:proposal/id}
   ::pc/output [:proposal/id :proposal/details :proposal/cost]}
  (let [result (d/pull db [[:argument/id :as :proposal/id]
                           [:proposal/subtext :as :proposal/details]
                           :proposal/cost]
                 [:argument/id (str id)])]
    (util/str-id->uuid-id result :proposal/id)))

(defresolver resolve-all-proposals [{:keys [db]} _]
  {::pc/output [{:all-proposals [:proposal/id]}]}
  (let [query-result (d/q '[:find [?id ...]
                            :where
                            [?e :argument/type :position]
                            [?e :argument/id ?id]]
                       db)]
    {:all-proposals (for [id query-result]
                      {:proposal/id (util/str->uuid id)})}))

(def resolvers [resolve-proposal resolve-all-proposals new-proposal set-vote resolve-votes])