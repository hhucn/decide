(ns decide.model.argument
  (:require
    [datahike.api :as d]
    [datahike.core :refer [squuid]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s])
  (:import (java.util UUID)))

(defn pro? [t] (= t :pro))
(>defn str->uuid [s] [string? => uuid?] (UUID/fromString s))
(>defn str-id->uuid-id
  "Updates :argument/id in map to uuid"
  [m]
  [(comp string? :argument/id) => (comp uuid? :argument/id)]
  (update m :argument/id str->uuid))

(defmutation add-argument [{:keys [connection]} {:keys [id text type subtype parent]}]
  {::pc/output [:argument/id]}
  (let [real-id (squuid)]
    (log/debug "New UUID " (str real-id))
    (d/transact! connection [{:db/id            "new-argument"
                              :argument/id      (str real-id)
                              :argument/text    text
                              :argument/type    type
                              :argument/subtype subtype}
                             [:db/add [:argument/id (str (second parent))] (if (pro? type) :argument/pros :argument/cons) "new-argument"]])
    {:tempids {id real-id}}))

(defresolver resolve-argument [{:keys [db]} {:keys [argument/id]}]
  {::pc/input  #{:argument/id}
   ::pc/output [:argument/id
                :argument/text
                :argument/type
                :argument/subtype
                {:argument/pros [:argument/id]}
                {:argument/cons [:argument/id]}]}
  (-> db
    (d/pull '[:argument/id
              :argument/text
              :argument/type
              :argument/subtype
              {:argument/pros [:argument/id]}
              {:argument/cons [:argument/id]}]
      [:argument/id (str id)])
    str-id->uuid-id
    (update :argument/pros (partial map str-id->uuid-id))
    (update :argument/cons (partial map str-id->uuid-id))))

(def resolvers [add-argument resolve-argument])
