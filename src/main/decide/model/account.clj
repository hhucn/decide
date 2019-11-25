(ns decide.model.account
  (:require
    [clojure.string :refer [split blank?]]
    [decide.server-components.ldap :as ldap]
    [datahike.api :as d]
    [datahike.core :refer [conn? db?]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]))

(s/def :account/id string?)
(s/def :account/display-name (s/and string? (complement blank?)))
(s/def :account/firstname string?)
(s/def :account/lastname string?)
(s/def :account/email string?)
(s/def :account/active? boolean?)
(s/def ::account (s/keys :req [:account/id
                               :account/firstname :account/lastname
                               :account/display-name
                               :account/email]))

(defmacro subset-of
  "Returns a spec that makes all required keys in a map optional. Leaves `gen` as it is"
  [spec]
  (let [[_ & {:keys [req req-un opt opt-un gen]}] (s/form spec)]
    `(s/keys :opt ~(vec (concat req opt))
       :opt-un ~(vec (concat opt-un req-un))
       :gen ~gen)))

(>defn redact [m & ks]
  [map? (s/* any?) => map?]
  (merge m (apply hash-map (interleave ks (repeat :auth/REDACTED)))))

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
  {::pc/output [{:all-accounts [:account/id]}]}
  {:all-accounts (mapv
                   (fn [id] {:account/id id})
                   (all-account-ids db))})

(>defn upsert-account!
  [conn account]
  [conn? ::account => future?]
  (d/transact! conn [account]))

(>defn get-account [db id subquery]
  [db? ::id (s/coll-of keyword? :kind vector?) => (subset-of ::account)]
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
  [::ldap/ldap-entity => ::account]
  #:account{:id           uid
            :display-name (default-display-name firstname)
            :firstname    firstname
            :lastname     lastname
            :email        mail})


(defresolver account-resolver [{:keys [db] :as env} {:account/keys [id]}]
  {::pc/input  #{:account/id}
   ::pc/output [:account/display-name :account/firstname :account/lastname :account/email :account/active?]}
  (do
    (log/info env)
    (when (account-exists? db id)
      (let [{req-id :account/id valid? :session/valid?} (get-in env [:ring/request :session])
            allowed? (and (= req-id id) valid?)]
        (cond-> (get-account db id [:account/display-name :account/firstname :account/lastname :account/email :account/active?])
          (not allowed?) (redact :account/firstname :account/lastname :account/email :account/active?))))))

(def resolvers [account-resolver])
