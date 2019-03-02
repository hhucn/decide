(ns decidotron.ui.root
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.routing :as r :refer [defsc-router]]
    [dbas.client :as dbas]
    [decidotron.ui.components :as comp]
    [decidotron.ui.discuss.core :as discuss]
    [decidotron.ui.routing :as routing]
    [decidotron.api :as ms]))

(defsc Login [this {:keys [db/id router/page login/login-form]}]
  {:query         [:db/id :router/page
                   {:login/login-form (prim/get-query comp/LoginForm)}]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {:db/id            1
                     :router/page      :PAGE/login
                     :login/login-form (prim/get-initial-state comp/LoginForm {})})}
  (comp/ui-login-form login-form))

(defsc-router RootRouter [_this {:keys [db/id router/page]}]
  {:router-id      :root/router
   :default-route  discuss/Discuss
   :ident          (fn [] [page id])
   :router-targets {:PAGE/login       Login
                    :PAGE/preferences comp/PreferenceScreen}}
  (dom/p "Unknown Route!"))

(def ui-router (prim/factory RootRouter))

(defn ui-login-button [this connection]
  (if (dbas.client/logged-in? connection)
    (dom/button :.btn.btn-primary
      {:onClick #(prim/transact! this `[(ms/logout {})])}
      (dom/i :.fas.fa-sign-out-alt) " Logout")
    (dom/button :.btn.btn-light
      {:onClick #(prim/transact! this
                   `[(r/set-route {:router :root/router
                                   :target [:PAGE/login 1]})])}
      (dom/i :.fas.fa-sign-in-alt) " Login")))

(defsc Root [this {:keys [dbas/connection root/router]}]
  {:query         [:dbas/connection
                   {:root/router (prim/get-query RootRouter)}]
   :initial-state (fn [_]
                    (merge
                      {:root/router     (prim/get-initial-state RootRouter {})
                       :dbas/connection (dbas/new-connection (str js/dbas_host "/api"))}
                      routing/app-routing-tree))}
  (dom/div :.root
    (dom/nav :.navbar.navbar-light.bg-light
      (dom/a :.navbar-brand {:href "#"} "Decidotron 3000")
      (ui-login-button this connection))

    (dom/div {:className "container"}
      (ui-router router))))