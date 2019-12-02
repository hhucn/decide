(ns decide.model.argument
  (:require
    [datahike.api :as d]
    [datahike.core :refer [squuid]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [decide.util :as util]))

(defn pro? [t] (= t :pro))

(defmutation add-argument [{:keys [connection]} {:keys [id text type subtype parent author]}]
  {::pc/output [:argument/id]}
  (let [real-id (squuid)]
    (log/debug "New UUID " (str real-id))
    (d/transact connection [{:db/id            "new-argument"
                             :argument/id      (str real-id)
                             :argument/text    text
                             :argument/type    type
                             :argument/subtype subtype
                             :argument/author  author}
                            [:db/add [:argument/id (str (second parent))] (if (pro? type) :argument/pros :argument/cons) "new-argument"]])
    {:tempids {id real-id}}))


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

(def resolvers [add-argument resolve-argument retract-argument])
