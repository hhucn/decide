(ns decide.model.proposal
  (:require
    [datahike.api :as d]
    [datahike.core :refer [squuid db? conn?]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [decide.util :as util]
    [decide.model.process :as process])
  (:import (java.time Instant)))

(>defn add-proposal! [conn process-ident {:proposal/keys [id cost details]
                                          :argument/keys [text author created-when]}]
  [conn? ::process/process-ident any? => map?]
  (d/transact conn
    [(merge
       #:proposal{:db/id       "new-proposal"
                  :argument/id (str id)
                  :cost        cost
                  :details     details}
       #:argument{:text         text
                  :type         :position
                  :subtype      :position
                  :author       author
                  :created-when (str created-when)})
     [:db/add process-ident :process/proposals "new-proposal"]]))

(defmutation new-proposal [{:keys [connection AUTH/account-id] :as env} {:proposal/keys [id cost details]
                                                                         :argument/keys [text]}]
  {::pc/params [:proposal/id :argument/text :proposal/cost :proposal/details]
   ::pc/output [:proposal/id]}
  (when account-id
    (let [real-id   (squuid)
          tx-report (add-proposal! connection
                      [:process/slug "example"]
                      {:proposal/id           real-id
                       :proposal/cost         (Long/parseLong cost)
                       :proposal/subtext      details
                       :argument/text         text
                       :argument/author       [:account/id account-id]
                       :argument/created-when (Instant/now)})]
      (log/debug "New UUID for proposal" real-id)
      {:proposal/id real-id
       ::p/env      (assoc env :db (:db-after tx-report))
       :tempids     {id real-id}})))

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

(def resolvers [resolve-proposal resolve-all-proposals new-proposal])