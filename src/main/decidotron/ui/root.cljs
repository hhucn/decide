(ns decidotron.ui.root
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target defrouter]])
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [dbas.client :as dbas]
    [decidotron.ui.components :as comp]
    [decidotron.api :as ms]
    [fulcro.incubator.dynamic-routing :as dr]
    [decidotron.ui.routing :as routing]))

(dr/defsc-route-target LoginScreen [this {:keys [login/login-form]}]
  {:query           [{:login/login-form (prim/get-query comp/LoginForm)}]
   :ident           (fn [] [:screens/id :login-screen])
   :initial-state   (fn [_] {:login/login-form (prim/get-initial-state comp/LoginForm {})})
   :route-segment   (fn [] ["login"])
   :route-cancelled (fn [_])
   :will-enter      (fn [_reconciler _]
                      (js/console.log "Enter Login Screen")
                      (dr/route-immediate [:screens/id :login-screen]))
   :will-leave      (fn [_]
                      (js/console.log "Leaving Login Screen")
                      true)}
  (comp/ui-login-form login-form))

(defrouter RootRouter [_ _]
  {:router-targets [LoginScreen comp/PreferenceScreen]})

(def ui-router (prim/factory RootRouter))

(defn ui-login-button [this logged-in?]
  (if logged-in?
    (dom/button :.btn.btn-primary
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
  (dom/div :.root.container
    (dom/nav :.navbar.navbar-light.bg-light
      (dom/a :.navbar-brand {:href "#"} "Decidotron 3000")
      (ui-login-button this (dbas.client/logged-in? connection)))

    (dom/div {:className "container"}
      (ui-router router))))