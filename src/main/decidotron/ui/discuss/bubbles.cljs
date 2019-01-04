(ns decidotron.ui.discuss.bubbles
  (:require [decidotron.ui.mdc-components :as material]
            [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]))

(def bubble-types {"system" [:.bubble-system]
                   "status" [:.bubble-status]
                   "user"   [:.bubble-user]})

(defsc Bubble
  [this {:keys [html text type url]}]
  {:initial-state (fn [{:keys [text type]}] {:text text :type type})
   :query         [:html :text :type :url]}
  (material/list-item #js {}
    (dom/div :.bubble.mdc-elevation--z2 {:classes (bubble-types type)}
      (dom/span (or html text)))))

(def ui-bubble (prim/factory Bubble))

(defsc DBASBubbleArea
  [this {:keys [bubble-area/bubbles]}]
  {:initial-state (fn [{:keys [bubble-area/bubbles]}] {:bubble-area/bubbles bubbles})
   :query         [{:bubble-area/bubbles (prim/get-query Bubble)}]}
  (dom/div :.bubble-area
    (material/mdc-list #js {:nonInteractive true}
      (map ui-bubble bubbles))))

(def ui-bubble-area (prim/factory DBASBubbleArea))