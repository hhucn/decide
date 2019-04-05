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
    [cljs-time.core :as t]
    [cljs-time.format :as tf]
    [cljs-time.coerce :refer [from-date]])
  (:require-macros [fulcro.incubator.dynamic-routing :refer [defsc-route-target defrouter]]))

(defn logged-in?
  "Returns true if the"
  [this]
  (= (prim/shared this [:dbas/connection ::dbas/login-status])
    ::dbas/logged-in))


(defn format-cost [cost]
  (format "%.0f €" (/ cost 100)))

(defn price-tag [cost]
  (dom/span :.price.text-muted (format-cost cost)))

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
            (dom/p (str "Es wird vorgeschlagen, dass " text ".")) ; TODO translate
            (price-tag cost)))
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
              (price-tag cost)))))


      (ui-pro-con-addon (->> computed
                          (merge {:collapse-id (str "collapse-" collapse-id)})
                          (prim/computed {:dbas.position/pros pros
                                          :dbas.position/cons cons}))))))


(def ui-preferred-item (prim/factory PreferredItem {:keyfn :dbas.position/id}))

(defn preferred-positions
  "This subcomponent renders the positions, which the user does prefer"
  [this slug preferences]
  (dom/div :.preferred-positions
    (dom/h2 "Ihre Prioritätsliste")
    (dom/h6 :.text-muted "Sortieren Sie sie absteigend Ihren Wünschen entsprechend. Sie dürfen dabei gerne über das Budget hinausgehen.")
    (dom/ol :.list-group
      (letfn [(assoc-levels [i v]
                (assoc v
                  :ui/preferred-level i
                  :ui/last? (-> preferences count dec (= i))))

              (add-computed [props]
                (prim/computed props
                  {:dbas-argument-link (format "%s/discuss/%s" js/dbas-host slug)
                   :up-fn              (fn up-fn [level]
                                         (prim/transact! this `[(ms/preference-up {:level ~level})]))
                   :down-fn            (fn down-fn [level]
                                         (prim/transact! this `[(ms/preference-down {:level ~level})]))
                   :un-prefer-fn       (fn un-prefer-fn [position-id]
                                         (prim/transact! this `[(ms/un-prefer {:position/id ~position-id})]))}))]
        (->> preferences
          (map-indexed assoc-levels)
          (map add-computed)
          (map ui-preferred-item))))))

(defn untouched-positions
  "This subcomponent renders the positions, which the user does not prefer (yet)"
  [this slug position-items]
  (dom/div :.untouched-positions
    (dom/h3 "Weitere Positionen")
    (dom/h6 :.text-muted "Wählen Sie die für Sie wichtigen Positionen und lassen Sie die unwichtigen hier.")
    (dom/ul :.list-group
      (map (fn [position-item]
             (ui-pref-list-item
               (prim/computed position-item
                 {:prefer-fn          (fn [position-id] (prim/transact! this `[(ms/prefer {:position/id ~position-id})]))
                  :dbas-argument-link (format "%s/discuss/%s" js/dbas-host slug)})))
        position-items))))

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
      [(if (empty? positions)
         (dom/div
           (dom/p :.alert.alert-info "Bisher gibt es keine Vorschläge. Fügen Sie welche hinzu!"))
         (dom/div
           (when (not-empty preferences)
             (preferred-positions this slug preferences))

           (when (not-empty position-items)
             [(dom/div :.my-4)
              (untouched-positions this slug position-items)])))

       (dom/div :.my-2)
       (dom/a :.btn.btn-outline-secondary.btn-block {:href (str js/dbas_host "/discuss/" slug)}
         "Füge einen Vorschlag hinzu.")])
    (dom/div :.alert.alert-info "Sie müssen sich einloggen, bevor Sie Ihre Stimme abgeben können.")))

(def ui-pref-list (prim/factory PreferenceList))

(defn format-votes-date [votes-date]
  (tf/unparse (tf/formatter "dd.MM.yyyy' um 'HH:mm' Uhr'") (t/to-default-time-zone (from-date votes-date))))

(defsc VoteHeader [_ {:dbas.issue/keys [title info votes-end]}]
  (dom/div :.vote-header.mb-5
    (dom/h1 {:style {:word-wrap     "break-word"
                     :overflow-wrap "break-word"}}          ; it is recommended to use both
      title)
    (dom/p :.text-muted info)
    (dom/p
      "Es werden für 20.000€ Vorschläge gewählt.
      Dafür können Sie die Vorschläge auswählen, von welchen Sie möchten, dass diese umgesetzt werden.")
    (dom/p
      "Die Vorschläge lassen sich sortieren, wobei Ihr Favorit das meiste Gewicht bei der Abstimmung hat, Ihr zweitliebster Vorschlag etwas weniger usw.
      Vorschläge, die Sie nicht mögen, wählen Sie einfach nicht aus und lassen sie wo sie sind.")
    (when votes-end
      (dom/p (format "Die Stimmabgabe ist möglich bis zum %s. Danach werden die Ergebnise hier angezeigt." (format-votes-date votes-end))))))

(def ui-vote-header (prim/factory VoteHeader))

(defsc ResultEntry [_ {:dbas.position/keys [text id cost pros cons]}
                    {:keys [winner?] :as computed}]
  {:query [{:dbas/position (prim/get-query models/Position)}]}
  (let [collapse-id (random-uuid)]
    (dom/li :.mb-2.mdc-card
      (dom/div :.list-group-item.container
        (dom/div :.row
          (dom/div :.col.d-flex.justify-content-between
            (dom/p {:className (when-not winner? "text-muted")}
              (format (if winner? "Es wurde erfolgreich darüber abgestimmt, dass %s."
                                  "Nicht erfolgreich war der Vorschlag, dass %s.") text)) ; TODO translate
            (price-tag cost)))
        (expand-button collapse-id))
      (ui-pro-con-addon (->> computed
                          (merge {:collapse-id (str "collapse-" collapse-id)})
                          (prim/computed {:dbas.position/pros pros
                                          :dbas.position/cons cons}))))))

(def ui-result-entry (prim/factory ResultEntry))
(defn ui-result-entry-winner [props computed] (->> {:winner? true} (merge computed) (prim/computed props) ui-result-entry))
(defn ui-result-entry-loser [props computed] (->> {:winner? false} (merge computed) (prim/computed props) ui-result-entry))

(defsc ResultList [_this {:result/keys [show? positions]} computed]
  {:query [:result/show?
           {:result/positions [{:winners (prim/get-query models/Position)}
                               {:losers (prim/get-query models/Position)}]}]}
  (let [overall-cost (reduce + (map :dbas.position/cost (:winners positions)))]
    (if show?
      (dom/div
        (dom/p (str "Diese Vorschläge wurden von Ihnen als die wichtigsten auserkoren. "
                 (format "Verteilt werden dadurch %d €." (format-cost overall-cost))))
        (dom/ol :.list-group.winners (map #(ui-result-entry-winner % computed) (:winners positions)))
        (dom/div :.mb-4)
        (when-not (empty? (:losers positions))
          (dom/p "Diese Vorschläge waren nicht erfolgreich.")
          (dom/ol :.list-group.losers (map #(ui-result-entry-loser % computed) (:losers positions)))))
      (dom/p "Die Ergebnisse werden nach der Wahl angezeigt."))))

(def ui-result-list (prim/factory ResultList))

(defn refresh-button [on-click-fn]
  (dom/button :.btn.btn-sm.btn-link
    {:onClick on-click-fn}
    (dom/i :.fas.fa-redo) " Aktualisieren"))

(defn result-area [result-list]
  (dom/div :.result-area
    (dom/hr :.my-5)
    (dom/h3 "Ergebnisse")
    (ui-result-list result-list)))

(defsc-route-target PreferenceScreen [this {:preferences/keys [slug list result-list]
                                            :dbas/keys        [issue]}]
  {:query           [:preferences/slug
                     {:preferences/list (prim/get-query PreferenceList)}
                     {:dbas/issue (prim/get-query models/Issue)}
                     {:preferences/result-list (prim/get-query ResultList)}]
   :ident           [:preferences/slug :preferences/slug]
   :route-segment   (fn [] ["preferences" :preferences/slug])
   :route-cancelled (fn [_])
   :will-enter      (fn [reconciler {:keys [preferences/slug]}]
                      (dr/route-deferred [:preferences/slug slug]
                        #(df/load reconciler [:preferences/slug slug] PreferenceScreen
                           {:post-mutation        `dr/target-ready
                            :post-mutation-params {:target [:preferences/slug slug]}})))
   :will-leave      (fn [_] true)}
  (let [{:dbas.issue/keys [budget votes-start votes-end]} issue
        supported-issue? budget
        voting-started?  (or (not votes-start) (t/after? (t/now) (from-date votes-start)))
        voting-ended?    (and votes-end (t/after? (t/now) (from-date votes-end)))
        show-results?    (:result/show? result-list)]
    (if supported-issue?
      (dom/div :.preference-screen
        (ui-vote-header issue)
        (if voting-started?
          (when (not voting-ended?)
            (dom/div :.voting-area
              (ui-pref-list list)))

          (dom/p :.alert.alert-info
            (format "Die Stimmabgabe ist möglich ab dem %s. Sie werden darüber informiert!" (format-votes-date votes-start))))

        ; show results, if voting has begun and ended (or no end is defined) and told so by the backend.
        (when (and voting-started? show-results? (or voting-ended? (nil? voting-ended?)))
          (result-area (prim/computed result-list {:dbas-argument-link (format "%s/discuss/%s" js/dbas-host slug)})))

        (refresh-button #(df/load this [:preferences/slug slug] PreferenceScreen)))
      (dom/div
        (dom/p :.alert.alert-danger "Für dieses Thema wird keine Entscheidung getroffen!")))))
