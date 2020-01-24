(ns decide.model.session
  (:require
    [decide.application :refer [SPA]]
    [decide.model.account :as account]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.ui-state-machines :as uism]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [clojure.string :as str]
    [com.fulcrologic.guardrails.core :refer [>defn => | <-]]))

(def session-ident [:component/id :session])
(def valid?-query {session-ident [:session/valid?]})

(defsc Session
  "Session representation. Used primarily for server queries. On-screen representation happens in Login component."
  [_ _]
  {:query         [:session/valid?
                   {:>/current-user (comp/get-query account/Account)}]
   :ident         (fn [] session-ident)
   :pre-merge     (fn [{:keys [data-tree]}]
                    (merge {:session/valid? false :>/current-user {:account/id "" :account/display-name ""}}
                      data-tree))
   :initial-state {:session/valid? false :>/current-user {:account/id "" :account/display-name ""}}})

(>defn get-logged-in?
  "Returns whether a user is logged in or not.
  Make sure to query with `valid?-query`."
  [props]
  [map? => boolean?]
  (get-in props [session-ident :session/valid?] false))

(>defn get-user-id-from-state [state]
  [map? => any?]
  (get-in state (into session-ident [:>/current-user 1])))


(defn clear [env]
  (uism/assoc-aliased env :error ""))

(defn logout [env]
  (-> env
    (clear)
    (uism/assoc-aliased :username "" :session-valid? false :current-user "")
    (uism/trigger-remote-mutation :actor/login-form 'decide.model.session/logout {})
    (uism/activate :state/logged-out)))

(defn login [{::uism/keys [event-data] :as env}]
  (-> env
    (clear)
    (uism/trigger-remote-mutation :actor/login-form 'decide.model.session/login
      {:username          (:username event-data)
       :password          (:password event-data)
       ::m/returning      (uism/actor-class env :actor/current-session)
       ::uism/ok-event    :event/complete
       ::uism/error-event :event/failed})
    (uism/activate :state/checking-session)))

(>defn process-session-result [env error-message]
  [::uism/env string? => ::uism/env]
  (let [success? (uism/alias-value env :session-valid?)]
    (when success?
      (dr/change-route SPA ["proposals"]))
    (cond-> (clear env)
      success? (->
                 (uism/assoc-aliased :modal-open? false)
                 (uism/activate :state/logged-in))
      (not success?) (->
                       (uism/assoc-aliased :error error-message)
                       (uism/activate :state/logged-out)))))

(def global-events
  {:event/toggle-modal {::uism/handler (fn [env] (uism/update-aliased env :modal-open? not))}})

(uism/defstatemachine session-machine
  {::uism/actors
   #{:actor/login-form :actor/current-session}

   ::uism/aliases
   {:username       [:actor/login-form :account/id]
    :error          [:actor/login-form :ui/error]
    :modal-open?    [:actor/login-form :ui/open?]
    :session-valid? [:actor/current-session :session/valid?]
    :current-user   [:actor/current-session :>/current-user]}

   ::uism/states
   {:initial
    {::uism/target-states #{:state/logged-in :state/logged-out}
     ::uism/events        {::uism/started  {::uism/handler (fn [env]
                                                             (-> env
                                                               (uism/assoc-aliased :error "")
                                                               (uism/load ::current-session :actor/current-session
                                                                 {::uism/ok-event    :event/complete
                                                                  ::uism/error-event :event/failed
                                                                  :focus             [:session/valid? {:>/current-user [:account/id :account/display-name]}]})))}
                           :event/failed   {::uism/target-state :state/logged-out}
                           :event/complete {::uism/target-states #{:state/logged-in :state/logged-out}
                                            ::uism/handler       #(process-session-result % "")}}}

    :state/checking-session
    {::uism/events (merge global-events
                     {:event/failed   {::uism/target-states #{:state/logged-out}
                                       ::uism/handler       (fn [env]
                                                              (-> env
                                                                (clear)
                                                                (uism/assoc-aliased :error "Server error.")))}
                      :event/complete {::uism/target-states #{:state/logged-out :state/logged-in}
                                       ::uism/handler       #(process-session-result % "Invalid Credentials.")}})}

    :state/logged-in
    {::uism/events (merge global-events
                     {:event/logout {::uism/target-states #{:state/logged-out}
                                     ::uism/handler       logout}})}

    :state/logged-out
    {::uism/events (merge global-events
                     {:event/login {::uism/target-states #{:state/checking-session}
                                    ::uism/handler       login}})}}})

; region signup
(def signup-ident [:component/id :signup])
(defn signup-class [] (comp/registry-key->class :decide.ui.root/Signup))

(defn clear-signup-form*
  "Mutation helper: Updates state map with a cleared signup form that is configured for form state support."
  [state-map]
  (-> state-map
    (assoc-in signup-ident
      {:account/id             ""
       :account/password       ""
       :account/password-again ""})
    (fs/add-form-config* (signup-class) signup-ident)))

(defmutation clear-signup-form [_]
  (action [{:keys [state]}]
    (swap! state clear-signup-form*)))

(defn valid-email? [email] (str/includes? email "@"))
(defn valid-password? [password] (> (count password) 7))

(defmutation signup! [_]
  (action [{:keys [state]}]
    (log/info "Marking complete")
    (swap! state fs/mark-complete* signup-ident))
  (ok-action [{:keys [app state]}]
    (dr/change-route app ["signup-success"]))
  (remote [{:keys [state] :as env}]
    (let [{:account/keys [id password password-again]} (get-in @state signup-ident)]
      (boolean (and (valid-email? id) (valid-password? password)
                 (= password password-again))))))
; endregion
