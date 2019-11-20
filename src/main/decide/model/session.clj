(ns decide.model.session
  (:require
    [clojure.string :refer [split]]
    [decide.server-components.database :as db]
    [datahike.api :as d]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [com.fulcrologic.fulcro.server.api-middleware :as fmw]
    [decide.server-components.ldap :as ldap]))

(defonce account-database (atom {}))

(defresolver current-session-resolver [env input]
  {::pc/output [{::current-session [:session/valid? :account/display-name]}]}
  (let [{:keys [account/name session/valid?]} (get-in env [:ring/request :session])]
    (if valid?
      (do
        (log/info name "already logged in!")
        {::current-session {:session/valid? true :account/display-name name}})
      {::current-session {:session/valid? false}})))

(defn response-updating-session
  "Uses `mutation-response` as the actual return value for a mutation, but also stores the data into the (cookie-based) session."
  [mutation-env mutation-response]
  (let [existing-session (some-> mutation-env :ring/request :session)]
    (fmw/augment-response
      mutation-response
      (fn [resp]
        (let [new-session (merge existing-session mutation-response)]
          (assoc resp :session new-session))))))

(>defn default-display-name [{:keys [firstname]}]
  [(s/keys :req-un [::ldap/firstname]) => string?]
  (first (split firstname #"\s")))

(defmutation login [{:keys [connection] :as env} {:keys [username password]}]
  {::pc/output [:session/valid? :account/id :account/display-name :account/firstname :account/lastname :account/mail]}
  (log/info "Authenticating" username)
  (if-some [{:keys [uid firstname lastname mail] :as account} (ldap/login username password)]
    (response-updating-session env
      {:session/valid?       true
       :account/id           uid
       :account/display-name (default-display-name account)
       :account/firstname    firstname
       :account/lastname     lastname
       :account/mail         mail})
    (do
      (log/error "Invalid credentials supplied for" username)
      (throw (ex-info "Invalid credentials" {:username username})))))

(defmutation logout [env params]
  {::pc/output [:session/valid?]}
  (response-updating-session env {:session/valid? false :account/id ""}))

(defmutation signup! [env {:keys [email password]}]
  {::pc/output [:signup/result]}
  (swap! account-database assoc email {:email    email
                                       :password password})
  {:signup/result "OK"})

(def resolvers [current-session-resolver login logout signup!])
