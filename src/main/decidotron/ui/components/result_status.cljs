(ns decidotron.ui.components.result-status
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target]])
  (:require [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.mutations :as m :refer [defmutation]]
            [clojure.string :as str]
            [fulcro.incubator.dynamic-routing :as dr]
            [fulcro.client.data-fetch :as df]))

(defn- set-status [comp status]
  (fn [e] (m/set-value! comp :status/state status)))

(defmutation save-status [_]
  (remote [{:keys [ast]}]
    (assoc ast :key 'decidotron.api/save-status)))

(defsc StatusBox [this {:keys               [ui/editing?]
                        :status/keys        [content state]
                        :dbas.position/keys [id text]
                        :or                 {editing? false}}]
  {:query         [:status/content :status/state :ui/editing? :dbas.position/text :dbas.position/id
                   {[:dbas/connection '_] [:dbas.client/admin?]}]
   :initial-state (fn [p]
                    (merge p {:ui/editing? false}))
   :ident         [:result-status/position-id :dbas.position/id]}
  (let [shade   (if (= state :status/in-work) "dark" "light") ; dark on yellow, light on green and red
        form-id (random-uuid)]

    (dom/div :.mb-2.card
      (dom/div :.card-header.d-flex
        (dom/div
          (dom/span :.badge
            {:classes [(str "badge-" (case state
                                       :status/done "success"
                                       :status/in-work "warning"
                                       :status/cancelled "danger"
                                       ""))]}
            (case state
              :status/done "Fertig!"
              :status/in-work "In Arbeit"
              :status/cancelled "Abgebrochen"
              ""))
          (str " Der Vorschlag, dass " text "."))
        (when (prim/shared this [:dbas/connection :dbas.client/admin?])
          (dom/div :.ml-auto
            (if editing?
              (dom/button :.btn.btn-sm.btn-primary
                {:type "submit"
                 :form form-id}
                "Speichern")

              (dom/button :.btn.btn-sm
                {:onClick #(m/toggle! this :ui/editing?)
                 :classes [(str "btn-outline-" shade)]}
                "Bearbeiten")))))
      (dom/div :.card-body
        (if editing?
          (dom/form
            {:id       form-id
             :onSubmit (fn submit-status [e]
                         (.preventDefault e)
                         (m/toggle! this :ui/editing?)
                         (prim/transact! this `[(save-status ~{:dbas.position/id id
                                                               :status/content   content
                                                               :status/state     state})]))} ; TODO Mutate on remote
            (dom/div :.form-group
              (dom/div :.btn-group.btn-group-toggle.d-flex
                (dom/label :.btn.btn-success.flex-fill "Fertig"
                  (dom/input {:type    "radio"
                              :onClick (set-status this :status/done)}))
                (dom/label :.btn.btn-warning.flex-fill "In Arbeit"
                  (dom/input {:type    "radio"
                              :onClick (set-status this :status/in-work)}))
                (dom/label :.btn.btn-danger.flex-fill "Abgebrochen"
                  (dom/input {:type    "radio"
                              :onClick (set-status this :status/cancelled)}))))

            (dom/div :.form-group
              (dom/textarea :.form-control
                {:value    content
                 :onChange (partial m/set-string! this :status/content :event)})))
          (dom/div :.card-text
            (if (empty? content)
              (dom/i "Bislang gibt es keinen Status.")
              (map dom/p (str/split-lines content)))))))))

(def ui-status-box (prim/factory StatusBox {:keyfn :dbas.position/id}))

(defsc-route-target ResultStatusScreen [this {:keys [dbas.issue/slug result/show? result/winners]}]
  {:query           [:dbas.issue/slug
                     :result/show?
                     {:result/winners (prim/get-query StatusBox)}]
   :ident           [:dbas.issue/slug :dbas.issue/slug]
   :route-segment   (fn [] ["preferences" :dbas.issue/slug "status"])
   :route-cancelled (fn [_])
   :will-enter      (fn [reconciler {:keys [dbas.issue/slug]}]
                      (js/console.log slug)
                      #_(dr/route-immediate [:screens/ResultStatusScreen slug])
                      (dr/route-deferred [:dbas.issue/slug slug]
                        #(df/load reconciler [:dbas.issue/slug slug] ResultStatusScreen
                           {:post-mutation        `dr/target-ready
                            :post-mutation-params {:target [:dbas.issue/slug slug]}})))
   :will-leave      (fn [_] true)}
  (if show?
    (dom/div (map ui-status-box winners))
    (dom/div "Nothing to see here!")))


;;;;

