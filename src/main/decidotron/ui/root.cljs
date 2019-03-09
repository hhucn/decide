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

(declare LoginScreen)
(defsc-route-target LoginScreen [_this {:keys [login/login-form]}]
  {:query           [{:login/login-form (prim/get-query login/LoginForm)}]
   :ident           (fn [] [:screens/id :login-screen])
   :initial-state   (fn [_] {:login/login-form (prim/get-initial-state login/LoginForm {})})
   :route-segment   (fn [] ["login"])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _]
                      (dr/route-immediate [:screens/id :login-screen]))
   :will-leave      (fn [_] true)}
  (dom/div :.login-screen
    (dom/p :.lead "Log dich bitte mit deiner Uni Kennung ein.")
    (login/ui-login-form login-form)))

(defrouter RootRouter [_ _]
  {:router-targets [LoginScreen comp/PreferenceScreen]})

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
      (dom/a :.navbar-brand.d-flex.align-items-center {:href "#"}
        (dom/object {:data  "/dbas_logo_round.svg"
                     :type  "image/svg+xml"
                     :style {:height       "2rem"
                             :margin-right "1rem"}})
        "Decidotron 3000")
      (ui-login-button this (dbas.client/logged-in? connection)))
    (dom/div :.container
      (ui-router router))))