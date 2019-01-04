(ns decidotron.ui.discuss.choices
  (:require [decidotron.ui.mdc-components :as material]
            [fulcro.client.primitives :as prim :refer [defsc]]))

(defsc DBASChoice
  [this {:keys [htmls texts url]} {:keys [onClick-fn]}]
  {:initial-state (fn [{:keys [texts]}] {:text texts})
   :query         [:texts :htmls :url]}
  (material/list-item #js {:role "radio"
                           :onClick (if onClick-fn #(onClick-fn url) #())}
    (material/list-item-graphic #js
        {:graphic
         (material/radio #js {}
           (material/native-radio #js {:checked  false
                                       :readOnly true}))})
    (material/list-item-text #js {:primaryText (apply str htmls)})))

(def ui-choice (prim/factory DBASChoice))

(defsc DBASChoiceList
  [this {:keys [choice-list/choices]} {:keys [choices-onClick-fn]}]
  {:initial-state (fn [{:keys [choice-list/choices]}] {:choice-list/choices choices})
   :query         [{:choice-list/choices (prim/get-query DBASChoice)}]}
  (material/mdc-list #js {:role             "radiogroup"
                          :aria-orientation "vertical"}
    (map #(-> (prim/computed % {:onClick-fn choices-onClick-fn})
            ui-choice)
      choices)))

(def ui-choice-list (prim/factory DBASChoiceList))