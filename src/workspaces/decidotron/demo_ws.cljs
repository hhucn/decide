(ns decidotron.demo-ws
  (:require [fulcro.client.primitives :as fp]
            [fulcro.client.localized-dom :as dom]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.fulcro :as ct.fulcro]
            [nubank.workspaces.lib.fulcro-portal :as f.portal]
            [fulcro.client.mutations :as fm]
            [decidotron.ui.components :as ui :refer [DBASChoice DBASChoiceList ui-choice-list]]))

(fp/defsc FulcroDemo
  [this {:keys [counter]}]
  {:initial-state (fn [_] {:counter 0})
   :ident         (fn [] [::id "singleton"])
   :query         [:counter]}
  (dom/div
    (str "Fulcro counter demo [" counter "]")
    (dom/button {:onClick #(fm/set-value! this :counter (inc counter))} "+")))

(ws/defcard fulcro-demo-card
  (ct.fulcro/fulcro-card
    {::f.portal/root FulcroDemo}))


(ws/defcard choice-card
   (ct.fulcro/fulcro-card
     {::f.portal/root          DBASChoice
      ::f.portal/initial-state {:choice/text "Cats are bad!"}}))

(ws/defcard choices-card
   (ct.fulcro/fulcro-card
     {::f.portal/root          DBASChoiceList
      ::f.portal/initial-state {:choice-list/choices
                                [{:choice/text "Cats are great"}
                                 {:choice/text "Dogs a great"}]}}))

(ws/defcard bubble
   (ct.fulcro/fulcro-card
     {::f.portal/root ui/DBASBubble
      ::f.portal/initial-state {:bubble/text "I want to talk about the position that ... "
                                :bubble/type "system"}}))

(ws/defcard bubble-area
   (ct.fulcro/fulcro-card
     {::f.portal/root ui/DBASBubbleArea
      ::f.portal/wrap-root? false
      ::f.portal/initial-state {:bubble-area/bubbles [{:bubble/text "I want to talk about the position that ... "
                                                       :bubble/type "system"}
                                                      {:bubble/text "Now"
                                                       :bubble/type "status"}
                                                      {:bubble/text "You said that: Now you say that.."
                                                       :bubble/type "user"}]}}))

(ws/defcard dialog-area
   (ct.fulcro/fulcro-card
     {::f.portal/root ui/DBASDialogArea
      ::f.portal/initial-state {:bubble-area {:bubble-area/bubbles [{:bubble/text "I want to talk about the position that ... "
                                                                     :bubble/type "system"}
                                                                    {:bubble/text "Now"
                                                                     :bubble/type "status"}
                                                                    {:bubble/text "You said that: Now you say that.."
                                                                     :bubble/type "user"}]}
                                :choice-area {:choice-list/choices
                                              [{:choice/text "Cats are great"}
                                               {:choice/text "Dogs a great"}
                                               {:choice/text "Neither of them is great, we should banish both!"}]}}}))
