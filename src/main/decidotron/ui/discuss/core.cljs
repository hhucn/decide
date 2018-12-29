(ns decidotron.ui.discuss.core
  (:require [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.routing :as r :refer [defsc-router]]
            [fulcro.client.dom :as dom]
            [decidotron.ui.discuss.bubbles :as bubbles]
            [decidotron.ui.discuss.choices :as choices]
            [decidotron.loads :as loads]
            [decidotron.ui.mdc-components :as material]
            [dbas.client :as dbas]))


(defsc DialogArea
  [this {:keys [dialog-area/bubble-area dialog-area/choice-area]}]
  {:initial-state (fn [{:keys [bubble-area choice-area]}] {:dialog-area/bubble-area bubble-area
                                                           :dialog-area/choice-area choice-area})
   :query         [{:dialog-area/bubble-area (prim/get-query bubbles/DBASBubbleArea)}
                   {:dialog-area/choice-area (prim/get-query choices/DBASChoiceList)}]}
  (dom/div :.dialog-area.mdc-elevation--z2
    (bubbles/ui-bubble-area bubble-area)
    (choices/ui-choice-list choice-area)))

(def ui-dialog-area (prim/factory DialogArea))

(defsc IssueEntry [this {:keys [dbas.issue/title dbas.issue/slug] :as props}]
  {:query [:dbas.issue/title :dbas.issue/slug]
   :ident [:dbas.issue/by-slug :dbas.issue/slug]}
  (dom/li title))

(def ui-issue-entry (prim/factory IssueEntry {:keyfn :dbas.issue/slug}))

(defsc IssueList [this {:keys [db/id router/page issues dbas/connection] :as props}]
  {:query         [:db/id :router/page
                   [:dbas/connection '_]
                   {:issues (prim/get-query loads/Issue)}]
   :ident         (fn [] [page id])
   :initial-state (fn [params]
                    {:db/id               1
                     :router/page         :PAGE/issues})}
  (dom/div
    (material/button #js {:onClick #(loads/load-issues this connection [page id :issues])} "LOAD!")
    (choices/ui-choice-list issues)
    (map ui-issue-entry issues)))

(def ui-issue-list (prim/factory IssueList))

(defsc-router DiscussRouter [this {:keys [db/id router/page]}]
  {:router-id      :discuss/router
   :ident (fn [] [page id])
   :default-route  IssueList
   :router-targets {:PAGE/issues IssueList}}
  (dom/div "Something went wrong. :-("))

(def ui-discuss-router (prim/factory DiscussRouter))

(defsc Discuss [this {:keys [db/id router/page discuss/dialog-area discuss/router]}]
  {:query         [:db/id
                   :router/page
                   {:discuss/router (prim/get-query DiscussRouter)}
                   {:discuss/dialog-area (prim/get-query DialogArea)}]
   :ident         (fn [] [page id])
   :initial-state (fn [params]
                    {:db/id               1
                     :router/page         :PAGE/discuss
                     :discuss/router      (prim/get-initial-state DiscussRouter {})
                     :discuss/dialog-area (prim/get-initial-state DialogArea
                                            {:bubble-area {:bubble-area/bubbles [{:bubble/text "I want to talk about the position that ... "
                                                                                  :bubble/type "system"}
                                                                                 {:bubble/text "Now"
                                                                                  :bubble/type "status"}
                                                                                 {:bubble/text "You said that: Now you say that.."
                                                                                  :bubble/type "user"}]}
                                             :choice-area {:choice-list/choices
                                                           [{:choice/text "Cats are great"}
                                                            {:choice/text "Dogs a great"}
                                                            {:choice/text "Neither of them is great, we should banish both!"}]}})})}
  (dom/div
    (ui-discuss-router router)
    (ui-dialog-area dialog-area)))