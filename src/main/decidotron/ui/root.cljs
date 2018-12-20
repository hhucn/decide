(ns decidotron.ui.root
  (:require
    [fulcro.client.dom :as dom :refer [div]]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.routing :as r :refer [defsc-router]]
    [dbas.client :as dbas]
    [decidotron.ui.mdc-components :as material]
    [decidotron.ui.components :as comp]
    [decidotron.mutations :as ms]
    [fulcro.client.mutations :as m]))

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

(defsc Discuss [this {:keys [db/id router/page discuss/dialog-area]}]
  {:query         [:db/id
                   :router/page
                   {:discuss/dialog-area (prim/get-query comp/DBASDialogArea)}]
   :ident         (fn [] [page id])
   :initial-state (fn [params]
                    {:db/id               1
                     :router/page         :PAGE/discuss
                     :discuss/dialog-area (prim/get-initial-state comp/DBASDialogArea
                                            {:bubble-area {:bubble-area/bubbles [{:bubble/text "I want to talk about the position that ... "
                                                                                  :bubble/type "system"}
                                                                                 {:bubble/text "Now"
                                                                                  :bubble/type "status"}
                                                                                 {:bubble/text "You said that: Now you say that.."
                                                                                  :bubble/type "user"}]}
                                             :choice-area {:choice-list/choices
                                                           [{:choice/text "Cats are great"}
                                                            {:choice/text "Dogs a great"}
                                                            {:choice/text "Neither of them is great, we should banish both!"}]}})})}
  (dom/div
    (comp/ui-dialog-area dialog-area)))

(defsc-router RootRouter [this {:keys [db/id router/page]}]
  {:router-id      :root/router
   :default-route  Discuss
   :ident          (fn [] [page id])
   :router-targets {:PAGE/discuss Discuss
                    :PAGE/login   Login}}
  (dom/p "Unknown Route!"))

(def ui-router (prim/factory RootRouter))

(defsc Root [this {:keys [dbas/connection root/top-bar root/router root/drawer]}]
  {:query         [:dbas/connection
                   {:root/top-bar (prim/get-query TopBar)}
                   {:root/router (prim/get-query RootRouter)}
                   {:root/drawer (prim/get-query comp/Drawer)}]
   :initial-state (fn [params]
                    {:root/router     (prim/get-initial-state RootRouter {})
                     :root/drawer     (prim/get-initial-state comp/Drawer {:id drawer-id})
                     :dbas/connection dbas/connection})}

  (let [logged-in? (dbas/logged-in? connection)]
    (dom/div
      (comp/ui-drawer drawer)
      (ui-top-bar (prim/computed top-bar
                    {:topbar/nav-icon
                     (material/icon #js {:icon    "menu"
                                         :onClick #(prim/transact! this `[(ms/open-drawer {:drawer/id ~drawer-id})])})}))
      (material/button #js {:onClick #(prim/transact! this
                                        `[(r/set-route {:router :root/router
                                                        :target [:PAGE/discuss 1]})])} "Discuss")

      (ui-router router)
      (dom/p (if logged-in? (str "Logged in as: " (::dbas/nickname connection)) "Not logged in")))))