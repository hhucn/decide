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
    [decidotron.ui.routing :as routing]
    [fulcro.client.data-fetch :as df]
    [decidotron.ui.models :as models]
    [goog.string :as gstring]
    [dbas.client :as dbas]
    [goog.string :as gstring]))

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
  (let [logged-in? (dbas/logged-in? connection)
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
                                         (routing/nav-to! this :issues))})])))))))

(def ui-nav-drawer (prim/factory NavDrawer))

(defsc IssueList [this {:keys [dbas/issues dbas/connection]}]
  {:query [[:dbas/connection '_]
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

(defn format-cost [cost]
  (gstring/format "€ %.2f" (/ cost 100)))

(defsc PreferenceListItem [this {:keys [position]} {:keys [prefer-fn]}]
  {:query [{:position (prim/get-query models/Position)}]}
  (material/list-item #js {}
    (material/list-item-graphic
      #js {:graphic (material/icon-button
                      #js {:onClick #(prefer-fn (:id position))}
                      (material/icon #js {:icon "check_box_outline_blank" :className "material-icon prefer-icon"}))})
    (material/list-item-text #js {:className   "content"
                                  :primaryText (str "Ich bin dafür, dass " (:text position) ".")}) ; TODO translate
    (material/list-item-meta #js {:className "price"
                                  :meta      (format-cost (:cost position))})))

(def ui-pref-list-item (prim/factory PreferenceListItem {:keyfn :position}))

(defsc UpDownButton [this {:keys [level last?] :or {last? false}} {:keys [up-fn down-fn]}]
  (dom/div {:className "up-down"}
    (material/icon-button #js {:className "up-down-button up-down-button__up"
                               :dense     "true"
                               :onClick   #(up-fn level)
                               :disabled  (zero? level)}
      (material/icon #js {:icon "keyboard_arrow_up"}))
    (material/icon-button #js {:className "up-down-button up-down-button__down"
                               :dense     "true"
                               :onClick   #(down-fn level)
                               :disabled  last?}
      (material/icon #js {:icon "keyboard_arrow_down"}))))

(def ui-updown-button (prim/factory UpDownButton))

(defsc PreferredItem [this {:keys [ui/preferred-level position ui/last?] :or {last? false}} {:keys [un-prefer-fn] :as computed}]
  {:query [:ui/preferred-level {:position (prim/get-query models/Position)} :ui/last?]}
  (dom/li {:data-position-id (:id position)}
    (material/card #js {:outlined true}
      (material/card-primary-content #js {:className "preferred-item"}
        (ui-updown-button (prim/computed {:level preferred-level
                                          :last? last?} computed))
        (dom/p {:className "content"}
          (str "Ich bin dafür, dass " (or (:text position) "") "."))
        (material/list-item-meta #js {:className "price"
                                      :meta      (format-cost (:cost position))})
        (material/card-actions #js {:fullBleed false}
          (material/card-action-buttons #js {}
            (material/button #js {:href (gstring/format "http://0.0.0.0:4284/discuss/%s/justify/%d/agree"
                                          "was-sollen-wir-mit-20-000eur-anfangen" (:id position))}
              "Füg ein Argument hinzu!"))
          (material/card-action-icons #js {}
            (dom/i {:onClick #(un-prefer-fn (:id position))}
              (material/icon #js {:icon "not_interested"}))))))))


(def ui-preferred-item (prim/factory PreferredItem {:keyfn (comp :id :position)}))

(defsc PreferenceList [this {:keys [dbas.issue/slug preferences dbas.issue/positions dbas/connection]}]
  {:query [:dbas.issue/slug
           {:preferences (prim/get-query PreferredItem)}
           {[:dbas.issue/positions '_] (prim/get-query models/Position)}
           [:dbas/connection '_]]
   :ident [:preference-list/by-slug :dbas.issue/slug]}
  (when-not preferences
    (df/load this [:preference-list/by-slug slug] PreferenceList {:params  {:dbas.client/token (:dbas.client/token connection)}
                                                                  :without #{[:dbas.issue/positions '_] [:dbas/connection '_]}}))
  (let [preferred-ids  (set (map #(get-in % [:position :id]) preferences))
        position-items (->> positions
                         (remove #(preferred-ids (:id %)))
                         (map #(hash-map :position %)))]
    (material/list-group #js {}
      (material/button
        #js {:onClick #(df/load this [:preference-list/by-slug slug] PreferenceList {:params  {:dbas.client/token (:dbas.client/token connection)}
                                                                                     :without #{[:dbas.issue/positions '_] [:dbas/connection '_]}})}
        "Refresh!")
      (material/list-group #js {}
        (when (not-empty preferences)
          (material/list-group-subheader #js {} "Diesen Positionen stimmst du zu."))
        (material/mdc-list #js {:tag "ol"}
          (->> preferences
            (map-indexed (fn [i v] (assoc v :ui/preferred-level i :ui/last? (= i (dec (count preferences))))))
            (map #(prim/computed % {:up-fn   (fn [level] (prim/transact! this `[(ms/preference-up {:level ~level})]))
                                    :down-fn (fn [level] (prim/transact! this `[(ms/preference-down {:level ~level})]))
                                    :un-prefer-fn (fn [position-id] (prim/transact! this `[(ms/un-prefer {:position/id ~position-id})]))}))
            (map ui-preferred-item))))
      (material/list-divider #js {})
      (material/list-group #js {}
        (when (not-empty position-items)
          (material/list-group-subheader #js {} "Diesen Positionen kannst du zustimmen."))
        (material/mdc-list #js {}
          (map #(ui-pref-list-item
                  (prim/computed %
                    {:prefer-fn (fn [position-id] (prim/transact! this `[(ms/prefer {:position/id ~position-id})]))}))
            position-items))))))

(def ui-pref-list (prim/factory PreferenceList))

(defsc Position [this {:keys [id text cost]}]
  {:query [:id :text :cost]
   :ident [:position/by-id :id]})

(defsc PreferenceScreen [this {:keys [db/id router/page pref-list]}]
  {:query         [:db/id :router/page
                   {:pref-list (prim/get-query PreferenceList)}]
   :ident         (fn [] [page id])
   :initial-state (fn [_] {:db/id       1
                           :router/page :PAGE/preferences
                           :pref-list   {:dbas.issue/slug "was-sollen-wir-mit-20-000eur-anfangen"}})}
  (dom/div
    (ui-pref-list pref-list)))
