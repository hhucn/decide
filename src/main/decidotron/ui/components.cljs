(ns decidotron.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.dom :as dom]
    [fulcro.client.mutations :refer [defmutation]]
    [decidotron.api :as ms]
    [fulcro.client.data-fetch :as df]
    [decidotron.ui.models :as models]
    [dbas.client :as dbas]
    [goog.string :refer [format]]
    [fulcro.incubator.dynamic-routing :as dr]
    [cljs-time.format :as tf]
    [cljs-time.coerce :refer [from-date]])
  (:require-macros [fulcro.incubator.dynamic-routing :refer [defsc-route-target defrouter]]))

(defn logged-in?
  "Returns true if the"
  [this]
  (= (prim/shared this [:dbas/connection ::dbas/login-status])
    ::dbas/logged-in))


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
    (dom/div :.list-group-item.collapse.pro-con-addon.pr-0 {:id collapse-id}
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
    (dom/li :.mb-2.mdc-card
      (dom/div :.list-group-item.container
        (dom/div :.row.ml-0
          (dom/button :.btn.btn-outline-success {:onClick #(prefer-fn id)
                                                 :style   {:width  "2.5em"
                                                           :height "2.5em"}}
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
    (dom/li :.mb-2.mdc-card
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
(defsc PreferenceList [this {:keys [preference-list/slug preferences dbas/issue]}]
  {:query [:preference-list/slug
           {:preferences (prim/get-query PreferredItem)}
           {:dbas/issue (prim/get-query models/Issue)}]
   :ident [:preference-list/slug :preference-list/slug]}
  (if (logged-in? this)
    (let [positions      (:dbas.issue/positions issue)
          preferred-ids  (set (map :dbas.position/id preferences))
          position-items (->> positions
                           (remove #(preferred-ids (:dbas.position/id %))))]
      (dom/div
        (dom/div
          (when (not-empty preferences)
            (dom/div (dom/h2 "Deine Prioritätsliste")
              (dom/h6 :.text-muted "Sortiere sie absteigend deinen Wünschen entsprechend. Du darfst dabei gerne über das Budget hinausgehen.")))
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
              (dom/h6 :.text-muted "Wähle die für dich wichtige Positionen und lasse die unwichtigen hier.")))
          (dom/ul :.list-group
            (map #(ui-pref-list-item
                    (prim/computed %
                      {:prefer-fn          (fn [position-id] (prim/transact! this `[(ms/prefer {:position/id ~position-id})]))
                       :dbas-argument-link (format "%s/discuss/%s" js/dbas-host slug)}))
              position-items)))))
    (dom/div :.alert.alert-danger "Du musst dich einloggen, bevor du deine Stimme abgeben kannst.")))

(def ui-pref-list (prim/factory PreferenceList))

(defn format-votes-end [votes-end]
  (tf/unparse-local (tf/formatter "dd.MM.yyyy HH:mm") (from-date votes-end)))

(defn vote-header [{:dbas.issue/keys [title info votes-end]}]
  (dom/div
    (dom/h1 title)
    (dom/h6 :.text-muted info)
    (dom/p "Lorem ipsum dolor sit Lorem ipsum dolor sit Lorem ipsum dolor sit Lorem ipsum dolor sit Lorem ipsum dolor sit Lorem ipsum dolor sit Lorem ipsum dolor sit Lorem ipsum dolor sit Lorem ipsum dolor sit Lorem ipsum dolor sit.")
    (when votes-end
      (dom/p :.text-muted (format "Die Stimmabgabe ist möglich bis zum %s Uhr." (format-votes-end votes-end))))))

(defsc ResultList [_this {:keys [result/positions]}]
  {:query [{:result/positions [{:winners (prim/get-query models/Position)}
                               {:losers (prim/get-query models/Position)}]}]}
  (dom/div
    (dom/h5 "Dies sind die Ergebnisse")
    (dom/ol :.list-group
      (for [{:dbas.position/keys [text cost]} (:winners positions)]
        (dom/li :.list-group-item (dom/p (format "Es wurde dafür gestimmt, dass %s. %s" text (format-cost cost)))))
      (for [{:dbas.position/keys [text cost]} (:losers positions)]
        (dom/li :.list-group-item (dom/p :.text-muted (format "Es wurde nicht dafür gestimmt, dass %s. %s" text (format-cost cost))))))))

(def ui-result-list (prim/factory ResultList))

(defsc-route-target PreferenceScreen [this {:keys [preferences/slug preferences/list dbas/issue preferences/result-list]}]
  {:query           [:preferences/slug
                     {:preferences/list (prim/get-query PreferenceList)}
                     {:dbas/issue (prim/get-query models/Issue)}
                     {:preferences/result-list (prim/get-query ResultList)}]
   :ident           [:preferences/slug :preferences/slug]
   :route-segment   (fn [] ["preferences" :preferences/slug])
   :route-cancelled (fn [_])
   :will-enter      (fn [reconciler {:keys [preferences/slug]}]
                      (js/console.log "Enter Preference Screen" slug)
                      (dr/route-deferred [:preferences/slug slug]
                        #(df/load reconciler [:preferences/slug slug] PreferenceScreen
                           {:post-mutation        `dr/target-ready
                            :post-mutation-params {:target [:preferences/slug slug]}})))
   :will-leave      (fn [_]
                      (js/console.log "Leaving Preference Screen")
                      true)}
  (dom/div :.preference-screen
    (vote-header issue)
    (ui-pref-list list)
    (dom/hr :.mt-5)
    (when-not (empty? (:result/positions result-list))
      (ui-result-list result-list))
    (dom/button :.btn.btn-sm.btn-link
      {:onClick #(df/load this [:preferences/slug slug] PreferenceScreen)}
      "Refresh!")))
