(ns decide.model.proposal
  (:require
    [datahike.api :as d]
    [datahike.core :refer [squuid]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [decide.util :as util])
  (:import (java.time Instant)))

(defmutation add-proposal [{:keys [connection AUTH/account-id]} {:proposal/keys [id cost subtext]
                                                                 :argument/keys [text]}]
  {::pc/params #{:proposal/id :argument/text :proposal/cost :proposal/subtext}
   ::pc/output [:proposal/id]}
  (when account-id
    (let [real-id (squuid)]
      (log/debug "New UUID for proposal" (str real-id))
      (log/spy :info (d/transact connection
                       [#:proposal{:db/id                 "new-proposal"
                                   :argument/id           (str real-id)
                                   :cost                  cost
                                   :subtext               subtext
                                   :argument/text         text
                                   :argument/type         :position
                                   :argument/subtype      :position
                                   :argument/author       [:account/id account-id]
                                   :argument/created-when (str (Instant/now))}

                        [:db/add [:process/slug "example"] :process/proposals "new-proposal"]])) ;; TODO FIX
      {:proposal/id real-id
       :tempids     {id real-id}})))

(defresolver resolve-proposal [{:keys [db]} {:keys [proposal/id]}]
  {::pc/input  #{:proposal/id}
   ::pc/output [:proposal/id :proposal/subtext :proposal/cost]}
  (-> db
    (d/pull [:argument/id
             :proposal/subtext
             :proposal/cost]
      [:argument/id (str id)])
    util/str-id->uuid-id))

(defresolver resolve-all-proposals [{:keys [db]} _]
  {::pc/output [{:all-proposals [:proposal/id]}]}
  (let [query-result (d/q '[:find [?id ...]
                            :where
                            [?e :argument/type :position]
                            [?e :argument/id ?id]]
                       db)]
    {:all-proposals (for [id query-result]
                      {:proposal/id (util/str->uuid id)})}))

(def resolvers [resolve-proposal resolve-all-proposals add-proposal])