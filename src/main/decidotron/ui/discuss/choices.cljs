(ns decidotron.ui.discuss.choices
  (:require [decidotron.ui.mdc-components :as material]
            [fulcro.client.primitives :as prim :refer [defsc]]))

(defsc DBASChoice
  [this {:keys [choice/text]}]
  {:initial-state (fn [{:keys [choice/text]}] {:choice/text text})
   :query         [:choice/text]}
  (material/list-item #js {:role "radio"}
    (material/list-item-graphic #js
        {:graphic
         (material/radio #js {}
           (material/native-radio #js {:checked  false
                                       :readOnly true}))})
    (material/list-item-text #js {:primaryText text})))

(def ui-choice (prim/factory DBASChoice))

(defsc DBASChoiceList
  [this {:keys [choice-list/choices]}]
  {:initial-state (fn [{:keys [choice-list/choices]}] {:choice-list/choices choices})
   :query         [{:choice-list/choices (prim/get-query DBASChoice)}]}
  (material/mdc-list #js {:role             "radiogroup"
                          :aria-orientation "vertical"}
    (map ui-choice choices)))

(def ui-choice-list (prim/factory DBASChoiceList))