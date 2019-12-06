(ns decide.server-components.ldap
  (:require
    [decide.server-components.config :refer [config]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn >defn- => | ?]]
    [clojure.spec.alpha :as s]
    [mount.core :refer [defstate]]
    [clj-ldap.client :as ldap]
    [taoensso.timbre :as log]))

(s/def ::firstname string?)
(s/def ::lastname string?)
(s/def ::uid string?)
(s/def ::mail string?)
(s/def ::givenName string?)
(s/def ::sn string?)
(s/def ::sAMAccountName string?)
(s/def ::ldap-entity (s/keys :req-un [::firstname ::lastname ::uid ::mail]))

(s/def ::ldap-result (s/keys :req-un [::givenName ::sn ::sAMAccountName ::mail]))

(>defn- filter-uid [uid]
  [::uid => string?]
  (format "(sAMAccountName=%s)" uid))

(>defn create-ldap-resolver [ldap-pool]
  [any? => [::uid string? => (? ::ldap-result)]]
  (fn ldap-resolver [uid password]
    (let [conn (ldap/get-connection ldap-pool)
          {:ldap/keys [base domain]} (get config :ldap)]
      (try
        (when (ldap/bind? conn (str uid "@" domain) password)
          (first (ldap/search conn base {:filter (str (filter-uid uid))
                                         :attributes [:givenName :sn :sAMAccountName :mail]})))
        (finally (ldap/release-connection ldap-pool conn))))))

(defstate ldap-resolve
  :start
  (let [ldap-conf (get-in config [:ldap :clj-ldap/args])
        ldap-pool (ldap/connect (or ldap-conf {:host "ldaps.ad.hhu.de"
                                               :ssl? true}))]
    (create-ldap-resolver ldap-pool)))

(>defn- shape-query-result [{:keys [givenName sn sAMAccountName mail]}]
  [::ldap-result => ::ldap-entity]
  {:firstname givenName
   :lastname sn
   :uid sAMAccountName
   :mail mail})

(>defn login [uid password]
  [::uid string? => (? ::ldap-entity)]
  (when-some [result (ldap-resolve uid password)]
    (shape-query-result result)))