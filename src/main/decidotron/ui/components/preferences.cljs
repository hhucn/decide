(ns decidotron.ui.components.preferences
  (:require [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.dom :as dom]
            [goog.string :refer [format]]
            [decidotron.api :as ms]
            [decidotron.ui.models :as models]
            [decidotron.ui.utils :refer [close-button price-tag logged-in?]]
            [decidotron.ui.components.pro-con-addon :refer [ui-pro-con-addon]]))

(defn expand-button [collapse-id]
  (dom/button :.expand-btn.btn.btn-sm.collapsed
    {:role        "button"
     :data-toggle "collapse"
     :aria-label  "collapse"
     :data-target (str "#collapse-" collapse-id)}
    (dom/i :.fas.fa-chevron-down)))

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

(defn untouched-positions
  "This subcomponent renders the positions, which the user does not prefer (yet)"
  [this slug position-items]
  (dom/div :.untouched-positions
    (dom/h3 "Weitere Vorschläge")
    (dom/h6 :.text-muted "Wählen Sie die für Sie wichtigen Vorschläge und lassen Sie die unwichtigen hier.")
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
      (dom/div
        (if (empty? positions)
          (dom/div
            (dom/p :.alert.alert-info "Bisher gibt es keine Vorschläge. Fügen Sie welche hinzu!"))
          (dom/div
            (when (not-empty preferences)
              (preferred-positions this slug preferences))

            (when (not-empty position-items)
              (dom/div
                (dom/div :.my-4)
                (untouched-positions this slug position-items)))))

        #_(dom/div :.my-2)
        #_(dom/a :.btn.btn-outline-secondary.btn-block {:href (str js/dbas_host "/discuss/" slug)}
            "Fügen Sie einen Vorschlag hinzu.")))
    (dom/div :.alert.alert-info "Sie müssen sich einloggen, bevor Sie Ihre Stimme abgeben können.")))

(def ui-pref-list (prim/factory PreferenceList))