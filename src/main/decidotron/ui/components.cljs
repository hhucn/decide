(ns decidotron.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.dom :as dom]
    [fulcro.client.mutations :as m :refer [defmutation]]
    [decidotron.api :as ms]
    [decidotron.ui.mdc-components :as material]
    [fulcro.client.routing :as r]
    [decidotron.loads :as loads]
    [decidotron.ui.routing :as routing]
    [fulcro.client.data-fetch :as df]
    [decidotron.ui.models :as models]
    [dbas.client :as dbas]
    [goog.string :refer [format]]
    [fulcro.incubator.dynamic-routing :as dr])
  (:require-macros [fulcro.incubator.dynamic-routing :refer [defsc-route-target defrouter]]))

(defsc InputField
  [this {:keys [db/id input/value] :as props} {:keys [ui/label ui/type] :as computed}]
  {:query         [:db/id :input/value]
   :ident         [:input/by-id :db/id]
   :initial-state (fn [{:keys [value] :or {value ""}}]
                    {:db/id       (prim/tempid)
                     :input/value value})}
  (dom/div :.form-group
    (dom/label {:htmlFor (str "id" label)} label)
    (dom/input :.form-control
      {:id          (str "id" label)
       :type        type
       :value       value
       :placeholder label
       :aria-label  label
       :onChange    (fn [e] (m/set-string! this :input/value :event e))})))

(def ui-input-field (prim/factory InputField))

(defmutation redirect-from-login [{:keys [where]}]
  (action [{:keys [state component]}]
    (let [login-status (get-in @state [:dbas/connection ::dbas/login-status])]
      (when (= ::dbas/logged-in login-status)
        (routing/change-route! component where)))))

(defsc LoginForm
  [this {:keys [login-form/nickname-field login-form/password-field dbas/connection]}]
  {:query         [{:login-form/nickname-field (prim/get-query InputField)}
                   {:login-form/password-field (prim/get-query InputField)}
                   {[:dbas/connection '_] (prim/get-query models/Connection)}]
   :initial-state (fn [{:keys [nickname password]
                        :or   {nickname "" password ""}}]
                    {:login-form/nickname-field (prim/get-initial-state InputField {:value nickname})
                     :login-form/password-field (prim/get-initial-state InputField {:value password})})}
  (dom/div :.mt-3
    (dom/p :.lead "Log dich bitte mit deiner Uni Kennung ein.")
    (case (::dbas/login-status connection)
      ::dbas/failed (dom/div :.alert.alert-danger {:role :alert} "Login fehlgeschlagen")
      ::dbas/logged-in (dom/div :.alert.alert-success {:role :alert} "Login erfolgreich")
      nil)
    (dom/form
      (ui-input-field (prim/computed nickname-field
                        {:ui/label "Nickname"
                         :ui/type  "text"}))
      (ui-input-field (prim/computed password-field
                        {:ui/label "Passwort"
                         :ui/type  "password"}))
      (dom/button :.btn.btn-primary
        {:type    "button"
         :onClick (fn login []
                    (df/load this :dbas/connection models/Connection
                      {:remote               :dbas
                       :params               {:nickname   (:input/value nickname-field)
                                              :password   (:input/value password-field)
                                              :connection connection}
                       :post-mutation        `redirect-from-login
                       :post-mutation-params {:where ["preferences" "was-sollen-wir-mit-20-000eur-anfangen"]}}))}
        "Login"))))

(def ui-login-form (prim/factory LoginForm))

(defn format-cost [cost]
  (format "%.2f €" (/ cost 100)))

(defsc UpDownButton [_this {:keys [level last?] :or {last? false}} {:keys [up-fn down-fn]}]
  (let [chevron-button
        (fn [up_or_down props]
          (dom/button (merge props {:className "btn" :type "button"})
            (dom/i {:className (str "fas " (case up_or_down :up "fa-chevron-up"
                                                            :down "fa-chevron-down"))})))]
    (dom/div {:className "up-down"}
      (chevron-button :up {:disabled (zero? level)
                           :onClick  #(up-fn level)})
      (chevron-button :down {:disabled last?
                             :onClick  #(down-fn level)}))))

(def ui-updown-button (prim/factory UpDownButton))

(defn- format-pro-con [text]
  (format "… %s." text))

(defn discuss-button [prefix id]
  (dom/a :.btn.btn-sm.btn-outline-primary
    {:href (format "%s/jump/%d" prefix id)}
    (dom/i :.far.fa-comments) " Diskutieren"))

(defsc ProConAddon [_this
                    {:dbas.position/keys [pros cons]}
                    {:keys [collapse-id dbas-argument-link]}]
  {:query [{:dbas.position/pros (prim/get-query models/Statement)}
           {:dbas.position/cons (prim/get-query models/Statement)}]}
  (let [pros (take 3 (shuffle pros))
        cons (take 3 (shuffle cons))]
    (dom/div :.list-group-item.collapse.pro-con-addon {:id collapse-id}
      (when (not-empty pros)
        (dom/div :.pro-con-addon__pros
          (dom/p
            (dom/span :.text-success "Dafür") " spricht, dass …")
          (dom/ul :.list-group.list-group-flush
            (for [{:dbas.statement/keys [text id argument-id]} pros]
              (dom/li :.list-group-item.d-flex.justify-content-between.align-items-center
                {:key (str collapse-id "-" id)}
                (format-pro-con text)
                (discuss-button dbas-argument-link argument-id))))))

      (when (not-empty cons)
        (dom/div :.pro-con-addon__cons.mt-3
          (dom/p
            (dom/span :.text-danger "Dagegen") " spricht, dass …")
          (dom/ul :.list-group.list-group-flush
            (for [{:dbas.statement/keys [id text argument-id]} cons]
              (dom/li :.list-group-item.d-flex.justify-content-between.align-items-center
                {:key (str collapse-id "-" id)}
                (format-pro-con text)
                (discuss-button dbas-argument-link argument-id)))))))))

(def ui-pro-con-addon (prim/factory ProConAddon))

(defn expand-button [collapse-id]
  (dom/button :.expand-btn.btn.btn-sm.collapsed
    {:role        "button"
     :data-toggle "collapse"
     :aria-label  "collapse"
     :data-target (str "#collapse-" collapse-id)}
    (dom/i :.fas.fa-chevron-down)))


(defsc PreferenceListItem [_this {:dbas.position/keys [text id cost pros cons]}
                           {:keys [prefer-fn] :as computed}]
  {:query [{:dbas/position (prim/get-query models/Position)}]}
  (let [collapse-id (random-uuid)]
    (dom/li :.mb-1
      (dom/div :.list-group-item.container
        (dom/div :.row
          (dom/button :.btn.btn-outline-success {:onClick #(prefer-fn id)}
            (dom/i :.far.fa-thumbs-up))
          (dom/div :.col.d-flex.justify-content-between
            (dom/p (str "Ich bin dafür, dass " text "."))   ; TODO translate
            (dom/span :.price.text-muted (format-cost cost))))
        (expand-button collapse-id))
      (ui-pro-con-addon (->> computed
                          (merge {:collapse-id (str "collapse-" collapse-id)})
                          (prim/computed {:dbas.position/pros pros
                                          :dbas.position/cons cons}))))))

(def ui-pref-list-item (prim/factory PreferenceListItem {:keyfn :dbas.position/id}))

(defn close-button [on-click-fn]
  (dom/button :.close {:type       "button"
                       :aria-label "Close"
                       :onClick    on-click-fn}
    (dom/span {:aria-hidden true} (str "×"))))

(defsc PreferredItem [this {:keys [ui/preferred-level
                                   dbas.position/text
                                   dbas.position/id
                                   dbas.position/cost
                                   dbas.position/pros
                                   dbas.position/cons
                                   ui/last?]
                            :or   {last? false}}
                      {:keys [un-prefer-fn] :as computed}]
  {:query [:ui/preferred-level
           :dbas.position/text
           :dbas.position/id
           :dbas.position/cost
           {:dbas.position/pros (prim/get-query models/Statement)}
           {:dbas.position/cons (prim/get-query models/Statement)}
           :ui/last?]
   :ident [:dbas.position/id :dbas.position/id]}
  (let [collapse-id (random-uuid)
        unprefer    (partial un-prefer-fn id)]
    (dom/li :.mb-1
      (dom/div :.list-group-item
        (dom/div {:data-position-id id}
          (dom/span :.unprefer-position (close-button unprefer))
          (expand-button collapse-id)
          (dom/div :.container
            (dom/div :.row
              (ui-updown-button (prim/computed {:level preferred-level
                                                :last? last?} computed))
              (dom/div {:className "align-center content card-text col"} (str "Ich bin dafür, dass " (or text "") ".")))
            (dom/div :.row.d-flex.justify-content-between
              (dom/div)
              (dom/div :.price.text-muted.float-right
                (format-cost cost))))))


      (ui-pro-con-addon (->> computed
                          (merge {:collapse-id (str "collapse-" collapse-id)})
                          (prim/computed {:dbas.position/pros pros
                                          :dbas.position/cons cons}))))))


(def ui-preferred-item (prim/factory PreferredItem {:keyfn :dbas.position/id}))

(declare PreferenceList)
(defsc-route-target PreferenceList [this {:keys [preference-list/slug preferences dbas/issue]}]
  {:query           [:preference-list/slug
                     {:preferences (prim/get-query PreferredItem)}
                     {:dbas/issue (prim/get-query models/Issue)}]
   :ident           [:preference-list/slug :preference-list/slug]
   :route-segment   (fn [] ["preferences" :preference-list/slug])
   :route-cancelled (fn [_])
   :will-enter      (fn [reconciler {:keys [preference-list/slug]}]
                      (js/console.log "Enter Preference Screen")
                      (dr/route-deferred [:preference-list/slug slug]
                        #(df/load reconciler [:preference-list/slug slug] PreferenceList
                           {:post-mutation        `dr/target-ready
                            :post-mutation-params {:target [:preference-list/slug slug]}})))
   :will-leave      (fn [_]
                      (js/console.log "Leaving Preference Screen")
                      true)}
  (let [positions      (:dbas.issue/positions issue)
        preferred-ids  (set (map :dbas.position/id preferences))
        position-items (->> positions
                         (remove #(preferred-ids (:dbas.position/id %))))]
    (dom/div
      (dom/button :.btn.btn-dark
        {:onClick #(df/load this [:preference-list/slug slug] PreferenceList)}
        "Refresh!")
      (dom/div
        (when (not-empty preferences)
          (dom/div (dom/h2 "Deine Prioritätsliste")
            (dom/h6 :.text-muted "Sortiere sie deinen Wünschen entsprechend.")))
        (dom/ol :.list-group
          (->> preferences
            (map-indexed (fn [i v] (assoc v
                                     :ui/preferred-level i
                                     :ui/last? (= i (dec (count preferences))))))
            (map #(prim/computed % {:dbas-argument-link (format "%s/discuss/%s" js/dbas-host slug)
                                    :up-fn              (fn [level] (prim/transact! this `[(ms/preference-up {:level ~level})]))
                                    :down-fn            (fn [level] (prim/transact! this `[(ms/preference-down {:level ~level})]))
                                    :un-prefer-fn       (fn [position-id] (prim/transact! this `[(ms/un-prefer {:position/id ~position-id})]))}))
            (map ui-preferred-item))))
      (dom/hr)
      (dom/div
        (when (not-empty position-items)
          (dom/div
            (dom/h3 "Weitere Positionen")
            (dom/h6 :.text-muted "Wähle die für dich wichtige Positionen.")))
        (dom/ul :.list-group
          (map #(ui-pref-list-item
                  (prim/computed %
                    {:prefer-fn          (fn [position-id] (prim/transact! this `[(ms/prefer {:position/id ~position-id})]))
                     :dbas-argument-link (format "%s/discuss/%s" js/dbas-host slug)}))
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
