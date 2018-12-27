(ns decidotron.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc get-query]]
    [fulcro.client.dom :as dom]
    [fulcro.client.mutations :as m :refer [defmutation]]
    [decidotron.mutations :as ms]
    [decidotron.ui.mdc-components :as material]
    [decidotron.remotes.dbas :as remote-dbas]
    [fulcro.client.routing :as r]
    [fulcro.client.data-fetch :as df]))

(defsc DBASChoice
  [this {:keys [choice/text] :as props}]
  {:initial-state (fn [{:keys [choice/text]}] {:choice/text text})
   :query         [:choice/text]}
  (material/list-item #js {:role "radio"}
    (material/list-item-graphic #js
        {:graphic
         (material/radio #js {}
           (material/native-radio #js {:checked  false
                                       :readOnly true}))})
    (material/list-item-text #js {:primaryText text})))

(def ui-choice (prim/factory DBASChoice))

(defsc DBASChoiceList
  [this {:keys [choice-list/choices] :as props}]
  {:initial-state (fn [{:keys [choice-list/choices]}] {:choice-list/choices choices})
   :query         [{:choice-list/choices (get-query DBASChoice)}]}
  (material/mdc-list #js {:role             "radiogroup"
                          :aria-orientation "vertical"}
    (map ui-choice choices)))

(def ui-choice-list (prim/factory DBASChoiceList))


(def bubble-types {"system" [:.bubble-system]
                   "status" [:.bubble-status]
                   "user"   [:.bubble-user]})

(defsc DBASBubble
  [this {:keys [bubble/text bubble/type]}]
  {:initial-state (fn [{:keys [bubble/text bubble/type]}] {:bubble/text text :bubble/type type})
   :query         [:bubble/text :bubble/type]}
  (material/list-item #js {}
    (dom/div :.bubble.mdc-elevation--z2 {:classes (bubble-types type)}
      (dom/span text))))

(def ui-bubble (prim/factory DBASBubble))

(defsc DBASBubbleArea
  [this {:keys [bubble-area/bubbles]}]
  {:initial-state (fn [{:keys [bubble-area/bubbles]}] {:bubble-area/bubbles bubbles})
   :query         [{:bubble-area/bubbles (get-query DBASBubble)}]}
  (dom/div :.bubble-area
    (material/mdc-list #js {:nonInteractive true}
      (map ui-bubble bubbles))))

(def ui-bubble-area (prim/factory DBASBubbleArea))

(defsc DBASDialogArea
  [this {:keys [dialog-area/bubble-area dialog-area/choice-area]}]
  {:initial-state (fn [{:keys [bubble-area choice-area]}] {:dialog-area/bubble-area bubble-area
                                                           :dialog-area/choice-area choice-area})
   :query         [{:dialog-area/bubble-area (get-query DBASBubbleArea)}
                   {:dialog-area/choice-area (get-query DBASChoiceList)}]}
  (dom/div :.dialog-area.mdc-elevation--z2
    (ui-bubble-area bubble-area)
    (ui-choice-list choice-area)))

(def ui-dialog-area (prim/factory DBASDialogArea))

(defsc DBASIssueEntry [this {:keys [dbas/title dbas/slug]}]
  {:query [:dbas/title :dbas/slug]
   :ident [:dbas-issue-entry/by-slug :dbas/slug]}
  (dom/li title))

(def ui-issue-entry (prim/factory DBASIssueEntry {:keyfn :dbas/slug}))

(defsc DBASIssueList [this {:keys [dbas/issues]}]
  {:query [{:dbas/issues [(prim/get-query DBASIssueEntry)]}]
   :initial-state {:dbas/issues []}}
  (dom/div
    (map ui-issue-entry issues)))

(def ui-issue-list (prim/factory DBASIssueList))


; ========================================

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
        close #(m/set-value! this :drawer/open? false)]
    (material/drawer #js {:modal   true
                          :open    open?
                          :onClose #(m/set-value! this :drawer/open? false)}
      (material/drawer-header #js {}
        (material/drawer-title #js {}
          (if logged-in?
            (:dbas.client/nickname connection)
            (material/button #js
                {:onClick  #(do (close)
                              (prim/transact! this
                                `[(r/set-route {:router :root/router
                                                :target [:PAGE/login 1]})
                                  (ms/toggle-drawer {:drawer/id :main-drawer})]))}
              "Login"))))
      (material/drawer-content #js {}
        (material/mdc-list #js {:tag "nav"}
          (map-indexed (fn [i p] (ui-nav-drawer-item (assoc p :drawer-item/index (inc i))))
            (cond-> [(prim/computed {:drawer-item/text "Discuss"
                                     :drawer-item/icon "forum"}
                       {:ui/onClick #(do (close)
                                         (prim/transact! this
                                           `[(r/set-route {:router :root/router
                                                           :target [:PAGE/discuss 1]})]))})])))))))


(def ui-nav-drawer (prim/factory NavDrawer))

(defsc TempRoot [this {:keys [dbas/issues dbas/connection]}]
  {:query [[:dbas/issues '_]
           [:dbas/connection '_]]}
  (dom/div
    (material/button #js {:outlined true
                          :onClick #(df/load this :dbas/issues nil
                                      {:remote :dbas
                                       :params {:connection connection}
                                       :parallel true})} "Load")
    (ui-issue-list issues)))

(def ui-temp-root (prim/factory TempRoot))