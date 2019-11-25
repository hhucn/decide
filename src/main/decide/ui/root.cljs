(ns decide.ui.root
  (:require
    [decide.model.session :as session]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li p h3 button]]
    [com.fulcrologic.fulcro.dom.html-entities :as ent]
    [com.fulcrologic.fulcro.dom.events :as evt]
    [com.fulcrologic.fulcro.components :as prim :refer [defsc]]
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [com.fulcrologic.fulcro.ui-state-machines :as uism :refer [defstatemachine]]
    [com.fulcrologic.fulcro.components :as comp]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro-css.css :as css]
    [com.fulcrologic.fulcro.algorithms.form-state :as fs]
    [decide.application :refer [history]]
    [decide.model.proposal :as proposal]
    [clojure.string :as str]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [decide.model.argument :as argument]
    [decide.model.proposal :as proposal]))

(defn field [{:keys [label valid? error-message] :as props}]
  (let [input-props (-> props (assoc :name label) (dissoc :label :valid? :error-message))]
    (div :.form-group
      (dom/label {:htmlFor label} label)
      (dom/input :.form-control input-props)
      (dom/div :.ui.error.message {:classes [(when valid? "hidden")]}
        error-message))))

(defsc SignupSuccess [this props]
  {:query         ['*]
   :initial-state {}
   :ident         (fn [] [:component/id :signup-success])
   :route-segment ["signup-success"]
   :will-enter    (fn [app _] (dr/route-immediate [:component/id :signup-success]))}
  (div
    (dom/h3 "Signup Complete!")
    (dom/p "You can now log in!")))

(defsc Signup [this {:account/keys [id password password-again] :as props}]
  {:query             [:account/id :account/password :account/password-again fs/form-config-join]
   :initial-state     (fn [_]
                        (fs/add-form-config Signup
                          {:account/id             ""
                           :account/password       ""
                           :account/password-again ""}))
   :form-fields       #{:account/id :account/password :account/password-again}
   :ident             (fn [] session/signup-ident)
   :route-segment     ["signup"]
   :componentDidMount (fn [this]
                        (comp/transact! this [(session/clear-signup-form {})]))
   :will-enter        (fn [app _] (dr/route-immediate [:component/id :signup]))}
  (let [submit!  (fn [evt]
                   (when (or (identical? true evt) (evt/enter-key? evt))
                     (comp/transact! this [(session/signup! {:email id :password password})])
                     (log/info "Sign up")))
        checked? (log/spy :info (fs/checked? props))]
    (div
      (dom/h3 "Signup")
      (div :.ui.form {:classes [(when checked? "error")]}
        (field {:label         "Email"
                :value         (or id "")
                :valid?        (session/valid-email? id)
                :error-message "Must be an email address"
                :autoComplete  "off"
                :onKeyDown     submit!
                :onChange      #(m/set-string! this :account/id :event %)})
        (field {:label         "Password"
                :type          "password"
                :value         (or password "")
                :valid?        (session/valid-password? password)
                :error-message "Password must be at least 8 characters."
                :onKeyDown     submit!
                :autoComplete  "off"
                :onChange      #(m/set-string! this :account/password :event %)})
        (field {:label         "Repeat Password" :type "password" :value (or password-again "")
                :autoComplete  "off"
                :valid?        (= password password-again)
                :error-message "Passwords do not match."
                :onChange      #(m/set-string! this :account/password-again :event %)})
        (dom/button :.ui.primary.button {:onClick #(submit! true)}
          "Sign Up")))))

(declare Session Account)

(defsc Login [this {:account/keys [id]
                    :ui/keys      [error open?] :as props}]
  {:query         [:ui/open? :ui/error :account/id
                   {[:component/id :session] (comp/get-query Session)}
                   [::uism/asm-id ::session/session]]
   :initial-state {:account/id "" :ui/error ""}
   :ident         (fn [] [:component/id :login])}
  (let [current-state (uism/get-active-state this ::session/session)
        {current-user :account/id
         display-name :account/display-name
         firstname    :account/firstname} (get-in props [[:component/id :session] :>/current-user])
        initial?      (= :initial current-state)
        loading?      (= :state/checking-session current-state)
        logged-in?    (= :state/logged-in current-state)
        password      (or (comp/get-state this :password) "")] ; c.l. state for security
    (dom/div
      (when-not initial?
        (dom/div
          (if logged-in?
            (div :.btn-group
              (div :.btn-group
                (dom/button :.btn.btn-outline-dark.dropdown-toggle
                  {:disabled     false
                   :data-toggle  "dropdown"
                   :onMouseEnter #(df/load! this [:account/id current-user] Account)}
                  (dom/span display-name))
                (let [possible-display-names #{firstname current-user}]
                  (div :.dropdown-menu
                    (for [name possible-display-names]
                      (dom/a :.dropdown-item name)))))
              (dom/button :.btn.btn-outline-dark
                {:onClick #(uism/trigger! this ::session/session :event/logout)}
                "Log out"))
            (dom/div :.dropdown
              (button :.btn.btn-outline-primary
                {:onClick #(uism/trigger! this ::session/session :event/toggle-modal)}
                "Login")
              (dom/div :.dropdown-menu.dropdown-menu-right
                {:classes [(when open? "show")]
                 :style   {:width "300px"}}
                (dom/form :.px-4.py-3
                  {:classes  [(when (seq error) "error")]
                   :onSubmit (fn [e]
                               (evt/prevent-default! e)
                               (uism/trigger! this ::session/session :event/login {:username id
                                                                                   :password password}))}
                  (dom/h3 "Login")
                  (field {:label    "Uni-Kennung"
                          :value    id
                          :onChange #(m/set-string! this :account/id :event %)})
                  (field {:label    "Passwort"
                          :type     "password"
                          :value    password
                          :onChange #(comp/set-state! this {:password (evt/target-value %)})})
                  (div :.ui.error.message error)
                  (dom/button :.btn.btn-primary
                    {:type    "submit"
                     :classes [(when loading? "loading")]} "Login"))))))))))

(def ui-login (comp/factory Login))

(declare Root)
(defsc ProposalsMain [this {:keys [all-proposals detailed-proposals]}]
  {:query         [{:all-proposals (prim/get-query proposal/ProposalCard)}
                   {:detailed-proposals (comp/get-query proposal/ProposalDetails)}]
   :initial-state (fn [_] {:all-proposals (for [i (range 15)] (prim/get-initial-state proposal/ProposalCard (inc i)))})
   :ident         (fn [] [:component/id :proposals])
   :route-segment ["proposals"]
   :will-enter    #(dr/route-immediate [:component/id :proposals])}
  (div :.container
    (div :.card-deck.d-flex.justify-content-center
      (for [proposal all-proposals]
        (dom/div :.col-md-6
          (proposal/ui-proposal-card proposal (proposal/ui-proposal-detail detailed-proposals)))))))

(defsc Main [this {:keys [proposal]}]
  {:query         [{:proposal (prim/get-query proposal/ProposalDetails)}]
   :ident         (fn [] [:component/id :main])
   :route-segment ["main"]
   :will-enter    (fn [_] (dr/route-immediate [:component/id :main]))}
  (div :.container
    #_(proposal/ui-proposal-detail proposal)))

(defsc Settings1 [this props]
  {:query         []
   :ident         (fn [] [:component/id :settings1])
   :route-segment ["settings1"]
   :will-enter    #(dr/route-immediate [:component/id :settings1])
   :initial-state {}}
  (div :.container.segment
    (h3 "Settings1")))

(defsc Settings2 [this props]
  {:query         []
   :ident         (fn [] [:component/id :settings2])
   :route-segment ["settings2"]
   :will-enter    #(dr/route-immediate [:component/id :settings2])
   :will-leave    #(do false)
   :initial-state {}}
  (div :.container.segment
    (h3 "Settings2")))

(dr/defrouter SubRouter [this props]
  {:router-targets [Settings1 Settings2]})

(def ui-sub-router (comp/factory SubRouter))

(defsc Settings [this {:keys [account/time-zone account/real-name sub-router] :as props}]
  {:query         [:account/time-zone :account/real-name {:sub-router (prim/get-query SubRouter)}]
   :ident         (fn [] [:component/id :settings])
   :route-segment ["settings"]
   :will-enter    #(dr/route-immediate [:component/id :settings])
   :initial-state {:sub-router {}}}
  (div :.container.segment
    (h3 "Settings")
    (dom/button :.btn {:onClick #(dr/change-route-relative this this ["settings1"])} "Settings 1")
    (dom/button :.btn {:onClick #(dr/change-route-relative this this ["settings2"])} "Settings 2")
    (dom/div (ui-sub-router sub-router))))


(dr/defrouter TopRouter [this {:keys [current-state pending-path-segment route-factory route-props]}]
  {:router-targets [Main proposal/ProposalCollection Settings proposal/ProposalDetails]}
  (case current-state
    :pending (dom/div :.d-flex.justify-content-center
               (dom/div :.spinner-border.ml-auto {:role "status"}
                 (dom/span :.sr-only "Loading...")))
    :failed (dom/div
              (dom/div "Ooops!")
              (dom/button {:onClick #(dr/change-route this ["settings"])} "Go to settings"))
    (dom/div "No route selected.")))

(def ui-top-router (comp/factory TopRouter))

(defsc Account [_ _]
  {:query [:account/id :account/display-name :account/firstname :account/lastname :account/email]
   :ident :account/id})

(defsc Session
  "Session representation. Used primarily for server queries. On-screen representation happens in Login component."
  [this {:keys [:session/valid?] :as props}]
  {:query         [:session/valid?
                   {:>/current-user (comp/get-query Account)}]
   :ident         (fn [] [:component/id :session])
   :pre-merge     (fn [{:keys [data-tree]}]
                    (merge {:session/valid? false :>/current-user {:account/id "" :account/display-name ""}}
                      data-tree))
   :initial-state {:session/valid? false :>/current-user {:account/id "" :account/display-name ""}}})

(defn nav-link [label href]
  (dom/li :.nav-item (dom/a :.btn.btn-light {:href href} label)))

(def dbas-logo
  (dom/svg {:fill "currentColor" :stroke "currentColor" :viewBox "-100 -100 200 200" :width "2em" :height "2em"}
    (dom/path {:d "M100 0c0 19-5 37-15 53a24 24 0 00-22 1L21 12a24 24 0 002-21l27-21a25 25 0 0039-15c7 13 11 29 11 45zM-24 0c0 6 2 12 5 16l-23 36a24 24 0 00-31 16A100 100 0 01-24-97c1 10 8 18 18 21v52c-11 3-18 13-18 24zM0 25c4 0 9-2 12-4l42 42a24 24 0 00-1 22 100 100 0 01-87 9 24 24 0 003-35l23-36 8 2zM24-97c16 4 31 12 43 23h-2a24 24 0 00-23 33L15-20l-8-4v-52c9-3 16-11 17-21zm-85 40a15 15 0 100 30 15 15 0 000-30z"})))

(defsc TopChrome [this {:root/keys [router current-session login]
                        ::dr/keys [id]}]
  {:query              [{:root/router (comp/get-query TopRouter)}
                        {:root/current-session (comp/get-query Session)}
                        [::uism/asm-id ::TopRouter]
                        [::dr/id '_]
                        {:root/login (comp/get-query Login)}]
   :componentDidUpdate (fn [this _ _]
                         (let [new-token (apply str "/" (interpose "/" (dr/current-route this this)))]
                           (log/debug "re-render" new-token)
                           (if (= new-token (.getToken history))
                             (.replaceToken history new-token)
                             (.setToken history new-token))))
   :ident              (fn [] [:component/id :top-chrome])
   :initial-state      {:root/router          {}
                        :root/login           {}
                        :root/current-session {}}}
  (let [current-tab (some-> (dr/current-route this this) first keyword)]
    (div
      (dom/nav :.navbar.navbar-expand-sm.navbar-light.bg-light
        (dom/div :.container
          (dom/a :.navbar-brand.d-flex.align-items-center
            (dom/picture :.mr-2 dbas-logo)
            "decide")
          (dom/button
            {:type          "button",
             :data-toggle   "collapse",
             :data-target   "#navbarSupportedContent",
             :aria-controls "navbarSupportedContent",
             :aria-expanded "false",
             :aria-label    "Toggle navigation",
             :className     "navbar-toggler"}
            (dom/span {:className "navbar-toggler-icon"}))
          (dom/div :#navbarSupportedContent.collapse.navbar-collapse.mr-auto
            #_(dom/ul :.navbar-nav
                (dom/li :.nav-item
                  (dom/a :.nav-link {:classes [(when (= :main current-tab) "active")]
                                     :href    "/main"
                                     :onClick (fn [e]
                                                (.preventDefault e)
                                                (dr/change-route this ["main"]))} "Main"))
                (dom/li :.nav-item
                  (dom/a :.nav-link {:classes [(when (= :settings current-tab) "active")]
                                     :href    "/settings"
                                     :onClick (fn [e]
                                                (.preventDefault e)
                                                (dr/change-route this ["settings"]))} "Settings")))
            (dom/hr)
            (div :.right.menu
              (ui-login login)))))
      (div :.py-5
        (ui-top-router router)))))

(def ui-top-chrome (comp/factory TopChrome))

(defsc Root [this {:root/keys [top-chrome]}]
  {:query             [{:root/top-chrome (comp/get-query TopChrome)}]
   :initial-state     {:root/top-chrome {}}}
  (ui-top-chrome top-chrome))


