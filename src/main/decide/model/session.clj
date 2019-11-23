(ns decide.model.session
  (:require
    [clojure.string :refer [split]]
    [datahike.api :as d]
    [datahike.core :refer [conn? db?]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [com.fulcrologic.fulcro.server.api-middleware :as fmw]
    [decide.server-components.ldap :as ldap]
    [decide.model.account :as account]))

(defonce account-database (atom {}))

(defresolver current-session-resolver [env input]
  {::pc/output [{::current-session [:session/valid? :account/id]}]}
  (let [{:keys [account/id session/valid?] :as session} (get-in env [:ring/request :session])]
    (log/info session)
    (if valid?
      (do
        (log/info id "already logged in!")
        {::current-session {:session/valid? true :account/id id}})
      {::current-session {:session/valid? false}})))

(defn response-updating-session
  "Uses `mutation-response` as the actual return value for a mutation, but also stores the data into the (cookie-based) session."
  [mutation-env mutation-response upsert-session]
  (let [existing-session (some-> mutation-env :ring/request :session)]
    (fmw/augment-response
      mutation-response
      (fn [resp]
        (let [new-session (merge existing-session upsert-session)]
          (assoc resp :session new-session))))))

(defmutation login [{:keys [connection] :as env} {:keys [username password]}]
  {::pc/output [:session/valid? :account/id :account/display-name :account/firstname :account/lastname :account/email]}
  (log/info "Authenticating" username)
  (if-some [ldap-entry (ldap/login username password)]
    (let [{:keys [account/id] :as account} (account/ldap->account ldap-entry)]
      (do
        (account/upsert-account! connection account)
        (response-updating-session env
          (merge
            {:session/valid? true}
            account)
          {:session/valid? true
           :account/id     id})))
    (do
      (log/error "Invalid credentials supplied for" username)
      (throw (ex-info "Invalid credentials" {:username username})))))

(defmutation logout [env params]
  {::pc/output [:session/valid?]}
  (response-updating-session env {:session/valid? false} {:session/valid? false :account/id ""}))

(defmutation signup! [env {:keys [email password]}]
  {::pc/output [:signup/result]}
  (swap! account-database assoc email {:email    email
                                       :password password})
  {:signup/result "OK"})

(def resolvers [current-session-resolver login logout])