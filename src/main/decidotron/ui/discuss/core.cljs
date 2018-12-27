(ns decidotron.ui.discuss.core
  (:require [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.dom :as dom]
            [decidotron.ui.discuss.bubbles :as bubbles]
            [decidotron.ui.discuss.choices :as choices]))


(defsc DBASDialogArea
  [this {:keys [dialog-area/bubble-area dialog-area/choice-area]}]
  {:initial-state (fn [{:keys [bubble-area choice-area]}] {:dialog-area/bubble-area bubble-area
                                                           :dialog-area/choice-area choice-area})
   :query         [{:dialog-area/bubble-area (prim/get-query bubbles/DBASBubbleArea)}
                   {:dialog-area/choice-area (prim/get-query choices/DBASChoiceList)}]}
  (dom/div :.dialog-area.mdc-elevation--z2
    (bubbles/ui-bubble-area bubble-area)
    (choices/ui-choice-list choice-area)))

(def ui-dialog-area (prim/factory DBASDialogArea))