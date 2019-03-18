(ns decidotron.ui.root
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target defrouter]])
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [dbas.client :as dbas]
    [decidotron.ui.components :as comp]
    [decidotron.ui.components.login :as login]
    [decidotron.api :as ms]
    [fulcro.incubator.dynamic-routing :as dr]
    [decidotron.ui.routing :as routing]))

(def was-sollen-wir-mit-20-000eur-anfangen ["preferences" "was-sollen-wir-mit-20-000eur-anfangen"])

(defsc-route-target LoginScreen [_this {:keys [login/login-form]}]
  {:query           [{:login/login-form (prim/get-query login/LoginForm)}]
   :ident           (fn [] [:screens/id :login-screen])
   :initial-state   (fn [_] {:login/login-form (prim/get-initial-state login/LoginForm {})})
   :route-segment   (fn [] ["login"])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :login-screen]))
   :will-leave      (fn [_] true)}
  (dom/div :.login-screen
    (dom/p :.lead "Log dich bitte mit deiner Uni Kennung ein.")
    (login/ui-login-form login-form)))

(defsc-route-target MainPage [_ _]
  {:ident           (fn [] [:screens/id :main-screen])
   :route-segment   (fn [] [""])                            ; TODO this currently does not work.
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :main-screen]))
   :will-leave      (fn [_] true)}
  (dom/p "Hello"))

(defn main-page [this]
  (dom/div
    (dom/p "Nothing to see here."
      (dom/button :.btn.btn-sm.btn-link
        {:onClick #(routing/change-route! this was-sollen-wir-mit-20-000eur-anfangen)}
        "Zur Abstimmung"))))

(defrouter RootRouter [this {:keys [current-state]}]
  {:router-targets [LoginScreen comp/PreferenceScreen]}
  (case current-state
    :initial (main-page this)
    :pending (dom/div "Loading...")
    :failed (dom/div "Oops")
    (main-page this)))

(def ui-router (prim/factory RootRouter))

(defn ui-login-button [this logged-in?]
  (if logged-in?
    (dom/button :.btn.btn-light
      {:onClick #(do (prim/transact! this `[(ms/logout {})])
                     (routing/change-route! this ["login"]))}
      (dom/i :.fas.fa-sign-out-alt) " Logout")
    (dom/button :.btn.btn-light
      {:onClick #(routing/change-route! this ["login"])}
      (dom/i :.fas.fa-sign-in-alt) " Login")))

(defsc Root [this {:keys [dbas/connection root/router]}]
  {:query         [:dbas/connection
                   {:root/router (prim/get-query RootRouter)}]
   :initial-state (fn [_]
                    {:root/router     (prim/get-initial-state RootRouter {})
                     :dbas/connection (dbas/new-connection (str js/dbas_host "/api"))})}
  (dom/div :.root.container.mdc-card.mdc-card__root
    (dom/nav :.navbar.navbar-light.bg-light
      (dom/a :.navbar-brand.d-flex.align-items-center
        {:href    "#"
         :onClick #(routing/change-route! this [""])}
        (dom/img :.mr-2 {:src "/dbas_logo_round.svg" :style {:height "2rem"}})
        "Decidotron 3000")
      (ui-login-button this (dbas.client/logged-in? connection)))
    (dom/div :.container
      (ui-router router))))