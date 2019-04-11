(ns decidotron.ui.components.results
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target]])
  (:require [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.incubator.dynamic-routing :as dr]
            ["chart.js" :as chartjs]
            ["react-chartjs-2" :as react-chartjs]
            [fulcro.client.primitives :as prim]
            [fulcro.client.data-fetch :as df]
            [decidotron.ui.models :as models]))

(defn factory-apply [class]
  (fn [props & children]
    (apply js/React.createElement
      class props children)))

(def bar-chart (factory-apply react-chartjs/Bar))
(def polar-chart (factory-apply react-chartjs/Polar))
(def pie-chart (factory-apply react-chartjs/Pie))


(defsc-route-target ResultScreen [_ {:keys [result/slug result/show? result/positions]}]
  {:query           [:result/slug
                     :result/show?
                     {:result/positions [{:winners (conj (prim/get-query models/Position) :scores)}
                                         {:losers (conj (prim/get-query models/Position) :scores)}]}]
   :ident           [:result/slug :result/slug]
   :route-segment   (fn [] ["preferences" :result/slug "result"])
   :route-cancelled (fn [_])
   :will-enter      (fn [reconciler {:keys [result/slug]}]
                      (dr/route-deferred [:result/slug slug]
                        #(df/load reconciler [:result/slug slug] ResultScreen
                           {:post-mutation        `dr/target-ready
                            :post-mutation-params {:target [:result/slug slug]}})))
   :will-leave      (fn [_] true)}
  (let [all-positions (concat (:winners positions) (:losers positions))
        colors        (take (count all-positions) (cycle ["rgba(255, 99, 132, 0.8)", ; cljs->js doesn't do endless seqs
                                                          "rgba(54, 162, 235, 0.8)",
                                                          "rgba(255, 206, 86, 0.8)",
                                                          "rgba(75, 192, 192, 0.8)",
                                                          "rgba(153, 102, 255, 0.8)",
                                                          "rgba(255, 159, 64, 0.8)"]))]
    (js/console.log (map (comp :borda :scores) all-positions))
    (pie-chart (clj->js {:data {:labels   (map :dbas.position/text all-positions)
                                :datasets [{:data            (map (comp :borda :scores) all-positions)
                                            :label           "Rev in MM"
                                            :backgroundColor colors}]}}))))
                                            