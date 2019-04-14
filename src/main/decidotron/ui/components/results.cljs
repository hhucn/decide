(ns decidotron.ui.components.results
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target]])
  (:require [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.incubator.dynamic-routing :as dr]
            [goog.string :refer [format]]
            ["chart.js" :as chartjs]
            ["react-chartjs-2" :as react-chartjs]
            [fulcro.client.primitives :as prim]
            [fulcro.client.data-fetch :as df]
            [decidotron.ui.models :as models]
            [decidotron.ui.components.pro-con-addon :refer [ui-pro-con-addon]]
            [decidotron.ui.components.preferences :refer [expand-button]]
            [decidotron.ui.utils :refer [logged-in? format-cost price-tag format-votes-date]]))

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

(defn- position->sentence [{:dbas.position/keys [text]}]
  (str "Der Vorschlag, dass " (.trim text) "."))

(defn factory-apply [class]
  (fn [props & children]
    (apply js/React.createElement
      class props children)))

(def horizontal-bar-chart (factory-apply react-chartjs/HorizontalBar))

(defsc HorizontalBarChart [_ {:keys [winners losers]}]
  {:query [{:winners (prim/get-query models/Position)}
           {:losers (prim/get-query models/Position)}]}
  (let [tagged-winners (map #(assoc % :winner? true) winners)
        tagged-losers  (map #(assoc % :winner? false) losers)
        all-positions  (reverse (sort-by (comp (juxt :borda :approval) :dbas.position/scores) (concat tagged-winners tagged-losers)))]
    (dom/div :.score-chart
      (horizontal-bar-chart (clj->js {:data    {:labels   (map position->sentence all-positions)
                                                :datasets [{:label           "Punkte"
                                                            :xAxisID         "scores"
                                                            :data            (map (comp :borda :dbas.position/scores) all-positions)
                                                            :backgroundColor (map #(if (:winner? %) "rgba(255, 99, 132, 0.8)" "rgba(255, 99, 132, 0.2)") all-positions)}
                                                           {:label           "Zustimmungen"
                                                            :xAxisID         "scores"
                                                            :data            (map (comp :approval :dbas.position/scores) all-positions)
                                                            :backgroundColor (map #(if (:winner? %) "rgba(54, 162, 235, 0.8)" "rgba(54, 162, 235, 0.2)") all-positions)}]}
                                      :options {:scales
                                                {:xAxes [{:id       "scores"
                                                          :position "top"
                                                          :type     "linear"
                                                          :ticks    {:beginAtZero true
                                                                     :stepSize    1}}]}}})))))

(def h-bar-chart (prim/factory HorizontalBarChart))

(defsc-route-target ResultScreen [_ {:result/keys     [show? no-of-participants positions]
                                     :dbas.issue/keys [slug budget votes-end] :as props}]
  {:query           [:dbas.issue/slug
                     :dbas.issue/budget
                     :dbas.issue/votes-end
                     :result/show?
                     :result/no-of-participants
                     {:result/positions [{:winners (prim/get-query models/Position)}
                                         {:losers (prim/get-query models/Position)}]}]
   :ident           [:dbas.issue/slug :dbas.issue/slug]
   :route-segment   (fn [] ["preferences" :dbas.issue/slug "result"])
   :route-cancelled (fn [_])
   :will-enter      (fn [reconciler {:keys [dbas.issue/slug]}]
                      (dr/route-deferred [:dbas.issue/slug slug]
                        #(df/load reconciler [:dbas.issue/slug slug] ResultScreen
                           {:post-mutation        `dr/target-ready
                            :post-mutation-params {:target [:dbas.issue/slug slug]}})))
   :will-leave      (fn [_] true)}
  (dom/div
    (dom/h1 "Ergebnis")
    (if show?
      (dom/div
        (dom/p (str "Es gab insgesamt " no-of-participants " Teilnehmer, welche abgestimmt haben."))
        (ui-result-list props)
        (dom/hr :.mx-3)
        (dom/div
          (dom/h3 "Punkteverteilung")
          (dom/p
            "Wenn Vorschläge dieselbe Punktzahl haben, wird der Vorschlag mit den meisten Zustimmungen, ungeachtet seiner Priorität, gewählt.
          Die blassen Vorschläge haben verloren, da sie nicht in das Budget gepasst haben.")
          (dom/p "Weiter Beispiele finden Sie auf der " (dom/a {:href "/algorithm"} "Erklärungsseite") ".")
          (h-bar-chart positions)))

      (dom/div
        (dom/div :.alert.alert-info (str "Das Ergebnis wird erst ab " (format-votes-date votes-end) " angezeigt"))))))
                                            