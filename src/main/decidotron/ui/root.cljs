(ns decidotron.ui.root
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.routing :as r :refer [defsc-router]]
    [dbas.client :as dbas]
    [decidotron.ui.mdc-components :as material]
    [decidotron.ui.components :as comp]
    [decidotron.ui.discuss.core :as discuss]
    [decidotron.mutations :as ms]))

(def drawer-id 1)

(defn ui-login-button [this connection]
  (if (dbas.client/logged-in? connection)
    (material/icon-button #js {}
      (material/icon #js {:icon "account_circle"}))
    (material/button #js {:onClick #(prim/transact! this
                                      `[(r/set-route {:router :root/router
                                                      :target [:PAGE/login 1]})])}
      "Login")))

(defsc TopBar [this {:keys [dbas/connection]} {:keys [topbar/nav-icon]}]
  {:query [[:dbas/connection '_]]}
  (let [login-button (ui-login-button this connection)]
    (dom/div :#toolbar
      (material/top-app-bar #js {:title          "Decidotron 3000"
                                 :navigationIcon nav-icon})
      (material/top-app-bar-fixed-adjust #js {} []))))      ; spacing between top bar and content ; [] because it requires a child

(def ui-top-bar (prim/factory TopBar))

(defsc Login [this {:keys [db/id router/page login/login-form]}]
  {:query         [:db/id :router/page
                   {:login/login-form (prim/get-query comp/LoginForm)}]
   :ident         (fn [] [page id])
   :initial-state (fn [params]
                    {:db/id            1
                     :router/page      :PAGE/login
                     :login/login-form (prim/get-initial-state comp/LoginForm {})})}
  (comp/ui-login-form login-form))

(defsc-router RootRouter [this {:keys [db/id router/page]}]
  {:router-id      :root/router
   :default-route  discuss/Discuss
   :ident          (fn [] [page id])
   :router-targets {:PAGE/discuss discuss/Discuss
                    :PAGE/login   Login}}
  (dom/p "Unknown Route!"))

(def ui-router (prim/factory RootRouter))

(defsc Root [this {:keys [dbas/connection root/top-bar root/router root/drawer]}]
  {:query         [:dbas/connection
                   {:root/top-bar (prim/get-query TopBar)}
                   {:root/router (prim/get-query RootRouter)}
                   {:root/drawer (prim/get-query comp/NavDrawer)}]
   :initial-state (fn [params]
                    {:root/router     (prim/get-initial-state RootRouter {})
                     :dbas/connection dbas/connection
                     :root/drawer     (prim/get-initial-state comp/NavDrawer {:id :main-drawer})})}
  (let [logged-in? (dbas/logged-in? connection)]
    (dom/div
      (comp/ui-nav-drawer drawer)
      (ui-top-bar
        (prim/computed top-bar
          {:topbar/nav-icon
           (material/icon #js
               {:icon    "menu"
                :onClick #(prim/transact! this `[(ms/toggle-drawer {:drawer/id :main-drawer})])})}))

      (material/grid #js {}
        (ui-router router)))))