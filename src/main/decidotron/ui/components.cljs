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

(defsc PreferenceListItem [_this {:dbas.position/keys [text id cost]} {:keys [prefer-fn]}]
  {:query [{:dbas/position (prim/get-query models/Position)}]}
  (dom/li :.list-group-item.d-flex.container
    (dom/div :.row
      (dom/button :.btn.btn-outline-success {:onClick #(prefer-fn id)}
        (dom/i :.far.fa-thumbs-up))
      (dom/p :.col (str "Ich bin dafür, dass " text "."))   ; TODO translate
      (dom/span :.price.text-muted.float-right (format-cost cost)))))

(def ui-pref-list-item (prim/factory PreferenceListItem {:keyfn :dbas.position/id}))

(defsc UpDownButton [_this {:keys [level last?] :or {last? false}} {:keys [up-fn down-fn]}]
  (let [chevron-button
        (fn [up_or_down props]
          (dom/button (merge props {:className "btn btn-lg" :type "button"})
            (dom/i {:className (str "fas " (case up_or_down :up "fa-chevron-up"
                                                            :down "fa-chevron-down"))})))]
    (dom/div {:className "up-down"}
      (chevron-button :up {:disabled (zero? level)
                           :onClick  #(up-fn level)})
      (chevron-button :down {:disabled last?
                             :onClick  #(down-fn level)}))))

(def ui-updown-button (prim/factory UpDownButton))

(defsc Statement [this {:keys [uid position_uid text is_supportive]}]
  {:query [:uid :position_uid :text :is_supportive]
   :ident [:statement/by-id :uid]})

(defsc ProConAddon [this {:keys [position/pro position/con]}]
  {:query [{:position/pro (prim/get-query Statement)}
           {:position/con (prim/get-query Statement)}]}
  (dom/div {:className "pro-con-addon"}
    (dom/p (dom/span :.text-success "Pro:") " " pro)
    (dom/p (dom/span :.text-danger "Con:") " " con)))

(def ui-pro-con-addon (prim/factory ProConAddon))

(defsc PreferredItem [this {:keys [ui/preferred-level
                                   dbas.position/text
                                   dbas.position/id
                                   dbas.position/cost
                                   ui/last?]
                            :or   {last? false}}
                      {:keys [un-prefer-fn
                              dbas-argument-link] :as computed}]
  {:query [:ui/preferred-level
           :dbas.position/text
           :dbas.position/id
           :dbas.position/cost
           :ui/last?]}
  (dom/li {:data-position-id id
           :className        "list-group-item"}
    (dom/div #js {:className "preferred-item card-body"}
      (ui-updown-button (prim/computed {:level preferred-level
                                        :last? last?} computed))
      (dom/div :.container
        (dom/p :.row
          (dom/div {:className "content card-text col"} (str "Ich bin dafür, dass " (or text "") "."))
          (dom/div {:className "price text-muted float-right"} (format-cost cost)))
        (dom/div :.row
          (dom/div :.col
            (dom/div :.btn-toolbar
              (dom/div :.btn-group.mr-2
                (dom/a :.btn.btn-outline-secondary
                  {:href (gstring/format "%s/discuss/%s/justify/%d/agree" js/dbas-host
                           "was-sollen-wir-mit-20-000eur-anfangen" id)}
                  (dom/i :.fas.fa-plus) " Argument hinzufügen!")

                (dom/button {:className   "btn btn-outline-secondary"
                             :data-toggle "collapse"
                             :data-target (str "#prefered-item-collapse-" preferred-level)}
                  (dom/i :.fas.fa-eye) " Zeige mir Argumente"))

              (dom/div :.btn-group
                (dom/button {:className "icon-btn btn btn-outline-danger"
                             :onClick   #(un-prefer-fn id)}
                  (dom/i :.far.fa-thumbs-down))))))))

    (dom/div :.collapse {:id (str "prefered-item-collapse-" preferred-level)}
      (ui-pro-con-addon {:position/pro "Wasser ist gesund für die Menschen!"
                         :position/con "Ein Wasserspender ist aufwändig zu warten"}))))


(def ui-preferred-item (prim/factory PreferredItem {:keyfn (comp :id :position)}))

(defsc PreferenceList [this {:keys [preference-list/slug preferences dbas/issue]}]
  {:query         [:preference-list/slug
                   {:preferences (prim/get-query models/Position)}
                   {:dbas/issue (prim/get-query models/Issue)}]
   :ident         [:preference-list/slug :preference-list/slug]
   :initial-state (fn [{:keys [slug preferences]}]
                    {:preference-list/slug slug
                     :preferences          preferences})}
  (when-not preferences
    (df/load this [:preference-list/slug slug] PreferenceList))
  (let [positions      (:dbas.issue/positions issue)
        preferred-ids  (set (map :dbas.position/id preferences))
        position-items (->> positions
                         (remove #(preferred-ids (:dbas.position/id %))))]
    (material/list-group #js {}
      (material/button
        #js {:onClick #(df/load this [:preference-list/slug slug] PreferenceList)}
        "Refresh!")
      (dom/div
        (when (not-empty preferences)
          (dom/div (dom/h2 "Deine Prioritätsliste")
            (dom/h6 :.text-muted "Sortiere sie deinen Wünschen entsprechend.")))
        (dom/ol {:className "list-group list-group-flush"}
          (->> preferences
            (map-indexed (fn [i v] (assoc v
                                     :ui/preferred-level i
                                     :ui/last? (= i (dec (count preferences))))))
            (map #(prim/computed % {:dbas-argument-link (gstring/format "%s/discuss/%s/justify" js/dbas-host slug)
                                    :up-fn              (fn [level] (prim/transact! this `[(ms/preference-up {:level ~level})]))
                                    :down-fn (fn [level] (prim/transact! this `[(ms/preference-down {:level ~level})]))
                                    :un-prefer-fn (fn [position-id] (prim/transact! this `[(ms/un-prefer {:position/id ~position-id})]))}))
            (map ui-preferred-item))))
      (dom/hr)
      (dom/div
        (when (not-empty position-items)
          (dom/div
            (dom/h3 "Weitere Positionen")
            (dom/h6 :.text-muted "Wähle die für dich wichtige Positionen.")))
        (dom/ol :.list-group.list-group-flush
          (map #(ui-pref-list-item
                  (prim/computed %
                    {:prefer-fn (fn [position-id] (prim/transact! this `[(ms/prefer {:position/id ~position-id})]))}))
            position-items))))))

(def ui-pref-list (prim/factory PreferenceList))

(defsc PreferenceScreen [_this {:keys [db/id router/page pref-list]}]
  {:query         [:db/id :router/page
                   {:pref-list (prim/get-query PreferenceList)}]
   :ident         (fn [] [page id])
   :initial-state (fn [_] {:db/id       1
                           :router/page :PAGE/preferences
                           :pref-list   (prim/get-initial-state PreferenceList {:slug "was-sollen-wir-mit-20-000eur-anfangen"})})}
  (dom/div
    (ui-pref-list pref-list)))
