(ns decidotron.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.dom :as dom]
    [fulcro.client.mutations :as m :refer [defmutation]]
    [decidotron.mutations :as ms]
    [decidotron.ui.mdc-components :as material]
    [fulcro.client.routing :as r]
    [decidotron.loads :as loads]
    [decidotron.ui.discuss.core :as discuss]
    [decidotron.ui.routing :as routing]))

(defsc InputField
  [this {:keys [db/id input/value] :as props} {:keys [ui/label ui/type] :as computed}]
  {:query         [:db/id :input/value]
   :ident         [:input/by-id :db/id]
   :initial-state (fn [{:keys [value] :or {value ""}}]
                    {:db/id       (prim/tempid)
                     :input/value value})}
  (material/text-field #js {:label label}
    (material/input #js {:type     type
                         :value    value
                         :onChange (fn [e] (m/set-string! this :input/value :event e))})))

(def ui-input-field (prim/factory InputField))

(defsc LoginForm
  [this {:keys [login-form/nickname-field login-form/password-field]}]
  {:query         [{:login-form/nickname-field (prim/get-query InputField)}
                   {:login-form/password-field (prim/get-query InputField)}]
   :initial-state (fn [{:keys [nickname password]
                        :or   {nickname "" password ""}}]
                    {:login-form/nickname-field (prim/get-initial-state InputField {:value nickname})
                     :login-form/password-field (prim/get-initial-state InputField {:value password})})}
  (dom/form
    (material/grid #js {:align "right"}
      (material/row #js {}
        (material/cell #js {:columns 12}
          (ui-input-field (prim/computed nickname-field
                            {:ui/label "Nickname"
                             :ui/type  "text"})))
        (material/cell #js {:columns 12}
          (ui-input-field (prim/computed password-field
                            {:ui/label "Password"
                             :ui/type  "password"})))
        (material/cell #js {:columns 6 :align "bottom"}
          (material/button #js {:href    "#"
                                :raised  true
                                :onClick #(prim/transact! this `[(ms/login {:nickname ~(:input/value nickname-field)
                                                                            :password ~(:input/value password-field)})])}
            "Login"))))))

(def ui-login-form (prim/factory LoginForm))

(defsc NavDrawerItem [this {:keys [drawer-item/text drawer-item/icon drawer-item/index]} {:keys [ui/onClick]}]
  {:query [:drawer-item/text :drawer-item/icon :drawer-item/index]}
  (material/list-item #js {:onClick          onClick
                           :tag              "a"
                           :tabIndex         index
                           :childrenTabIndex index}
    (material/list-item-graphic #js {:graphic (material/icon #js {:icon icon})})
    (material/list-item-text #js {:primaryText text})))

(def ui-nav-drawer-item (prim/factory NavDrawerItem {:keyfn :drawer-item/index}))

(defsc NavDrawer [this {:keys [db/id drawer/open? dbas/connection]}]
  {:query         [:db/id :drawer/open? [:dbas/connection '_]]
   :ident         [:drawer/by-id :db/id]
   :initial-state (fn [{:keys [id]}]
                    {:db/id        id
                     :drawer/open? false})}
  (let [logged-in? (dbas.client/logged-in? connection)
        close      #(m/set-value! this :drawer/open? false)]
    (material/drawer #js {:modal   true
                          :open    open?
                          :onClose #(m/set-value! this :drawer/open? false)}
      (material/drawer-header #js {}
        (material/drawer-title #js {}
          (if logged-in?
            (:dbas.client/nickname connection)
            (material/button #js
                {:onClick #(do (close)
                               (routing/nav-to! this :login))}
              "Login"))))
      (material/drawer-content #js {}
        (material/mdc-list #js {:tag "nav"}
          (map-indexed (fn [i p] (ui-nav-drawer-item (assoc p :drawer-item/index (inc i))))
            (cond-> [(prim/computed {:drawer-item/text "Discuss"
                                     :drawer-item/icon "forum"}
                       {:ui/onClick #(do (close)
                                         (routing/nav-to! this :discuss))})])))))))

(def ui-nav-drawer (prim/factory NavDrawer))

(defsc IssueList [this {:keys [dbas/issues dbas/connection]}]
  {:query         [[:dbas/connection '_]
                   {[:dbas/issues '_] (prim/get-query discuss/IssueEntry)}]}
  (dom/div
    (material/button #js {:onClick #(loads/load-issues this connection [:ui/root])} "LOAD!")
    (js/console.log issues)
    (map discuss/ui-issue-entry issues)))

(def ui-issue-list (prim/factory IssueList))

(defsc TempRoot [this {:keys [db/id dbas/connection issue-list]}]
  {:query [:db/id
           [:dbas/connection '_]
           {:issue-list (prim/get-query IssueList)}]}
  (dom/div
    (material/button #js {:outlined true
                          :onClick  #(loads/load-issues this connection [:ui/root])} "Load")
    (ui-issue-list issue-list)))

(def ui-temp-root (prim/factory TempRoot))