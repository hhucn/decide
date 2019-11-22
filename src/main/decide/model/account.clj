(ns decide.model.account
  (:require
    [clojure.string :refer [split]]
    [decide.server-components.ldap :as ldap]
    [datahike.api :as d]
    [datahike.core :refer [conn? db?]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]))

(s/def ::id string?)

(>defn all-account-ids
  "Returns a sequence of UUIDs for all of the active accounts in the system"
  [db]
  [any? => (s/coll-of uuid? :kind vector?)]
  (d/q '[:find [?v ...]
         :where
         [?e :account/active? true]
         [?e :account/id ?v]]
    db))

(defresolver all-users-resolver [{:keys [db]} input]
  {;;GIVEN nothing (e.g. this is usable as a root query)
   ;; I can output all accounts. NOTE: only ID is needed...other resolvers resolve the rest
   ::pc/output [{:all-accounts [:account/id]}]}
  {:all-accounts (mapv
                   (fn [id] {:account/id id})
                   (all-account-ids db))})

(>defn get-account [db id subquery]
  [db? ::id vector? => (? map?)]
  (d/pull db subquery [:account/id id]))

(>defn default-display-name [firstname]
  [string? => string?]
  (first (split firstname #"\s")))

(>defn account-exists? [db id]
  [db? ::id => boolean?]
  (not (empty? (d/q '[:find ?e
                      :in $ ?id
                      :where [?e :account/id ?id]]
                 db id))))

(>defn ldap->account [{:keys [uid firstname lastname mail]}]
  [::ldap/ldap-entity => map?]
  #:account{:id           uid
            :display-name (default-display-name firstname)
            :firstname    firstname
            :lastname     lastname
            :mail         mail})


(defresolver account-resolver [{:keys [db] :as env} {:account/keys [id]}]
  {::pc/input  #{:account/id}
   ::pc/output [:account/display-name :account/firstname :account/lastname :account/mail]}
  (when (account-exists? db id)
    (get-account db id [:account/display-name :account/firstname :account/lastname :account/mail])))

(def resolvers [account-resolver])
