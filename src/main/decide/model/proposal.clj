(ns decide.model.proposal
  (:require
    [datahike.api :as d]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [decide.util :as util]))


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

(def resolvers [resolve-proposal resolve-all-proposals])