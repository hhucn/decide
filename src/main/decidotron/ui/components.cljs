(ns decidotron.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc get-query]]
    [fulcro.client.dom :as dom]))

(defsc PlaceholderImage
  "Generates an SVG image placeholder of the given size and with the given label
  (defaults to showing 'w x h'.

  ```
  (ui-placeholder {:w 50 :h 50 :label \"avatar\"})
  ```
  "
  [this {:keys [w h label]}]
  (let [label (or label (str w "x" h))]
    (dom/svg #js {:width w :height h}
      (dom/rect #js {:width w :height h :style #js {:fill        "rgb(200,200,200)"
                                                    :strokeWidth 2
                                                    :stroke      "black"}})
      (dom/text #js {:textAnchor "middle" :x (/ w 2) :y (/ h 2)} label))))

(def ui-placeholder (prim/factory PlaceholderImage))

; ============================

(defsc DBASChoice
  [this {:keys [choice/text] :as props}]
  {:initial-state (fn [{:keys [choice/text]}] {:choice/text text})
   :query [:choice/text]}
  (dom/li :.mdc-list-item
    (dom/a {:onClick #()})
    (dom/input :.mdc-list-item__graphic.mdc-radio {:type "radio" :checked false})
    (dom/label :.mdc-list-item__text text)))

(def ui-choice (prim/factory DBASChoice))

(defsc DBASChoiceList
  [this {:keys [choice-list/choices] :as props}]
  {:initial-state (fn [{:keys [choice-list/choices]}]{:choice-list/choices choices})
   :query [{:choice-list/choices (get-query DBASChoice)}]}
  (dom/ul :.mdc-list {:aria-orientation "vertical"} (map ui-choice choices)))

(def ui-choice-list (prim/factory DBASChoiceList))


(def bubble-types {"system" [:.bubble-system]
                   "status" [:.bubble-status]
                   "user" [:.bubble-user]})

(defsc DBASBubble
  [this {:keys [bubble/text bubble/type]}]
  {:initial-state (fn [{:keys [bubble/text bubble/type]}] {:bubble/text text :bubble/type type})
   :query [:bubble/text :bubble/type]}
  (dom/li :.mdc-list-item
    (dom/div :.bubble.mdc-elevation--z2 {:classes (bubble-types type)}
            (dom/span text))))

(def ui-bubble (prim/factory DBASBubble))

(defsc DBASBubbleArea
  [this {:keys [bubble-area/bubbles]}]
  {:initial-state (fn [{:keys [bubble-area/bubbles]}] {:bubble-area/bubbles bubbles})
   :query [{:bubble-area/bubbles (get-query DBASBubble)}]}
  (dom/div :.bubble-area
    (dom/ol :.mdc-list.mdc-list--non-interactive (map ui-bubble bubbles))))

(def ui-bubble-area (prim/factory DBASBubbleArea))

(defsc DBASDialogArea
  [this {:keys [dialog-area/bubble-area dialog-area/choice-area]}]
  {:initial-state (fn [{:keys [bubble-area choice-area]}] {:dialog-area/bubble-area bubble-area
                                                           :dialog-area/choice-area choice-area})
   :query [{:dialog-area/bubble-area (get-query DBASBubbleArea)}
           {:dialog-area/choice-area (get-query DBASChoiceList)}]}
  (dom/div :.dialog-area.mdc-elevation--z2
      (ui-bubble-area bubble-area)
      (ui-choice-list choice-area)))

(def ui-dialog-area (prim/factory DBASDialogArea))
