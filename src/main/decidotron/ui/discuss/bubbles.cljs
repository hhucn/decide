(ns decidotron.ui.discuss.bubbles
  (:require [decidotron.ui.mdc-components :as material]
            [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]))

(def bubble-types {"system" [:.bubble-system]
                   "status" [:.bubble-status]
                   "user"   [:.bubble-user]})

(defsc DBASBubble
  [this {:keys [bubble/text bubble/type]}]
  {:initial-state (fn [{:keys [bubble/text bubble/type]}] {:bubble/text text :bubble/type type})
   :query         [:bubble/text :bubble/type]}
  (material/list-item #js {}
    (dom/div :.bubble.mdc-elevation--z2 {:classes (bubble-types type)}
      (dom/span text))))

(def ui-bubble (prim/factory DBASBubble))

(defsc DBASBubbleArea
  [this {:keys [bubble-area/bubbles]}]
  {:initial-state (fn [{:keys [bubble-area/bubbles]}] {:bubble-area/bubbles bubbles})
   :query         [{:bubble-area/bubbles (prim/get-query DBASBubble)}]}
  (dom/div :.bubble-area
    (material/mdc-list #js {:nonInteractive true}
      (map ui-bubble bubbles))))

(def ui-bubble-area (prim/factory DBASBubbleArea))