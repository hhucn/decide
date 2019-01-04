(ns decidotron.ui.discuss.core
  (:require [fulcro.client.primitives :as prim :refer [defsc]]
            [fulcro.client.routing :as r :refer [defsc-router]]
            [fulcro.client.dom :as dom]
            [fulcro.client.data-fetch :as df]
            [decidotron.ui.discuss.bubbles :as bubbles]
            [decidotron.ui.discuss.choices :as choices]
            [decidotron.loads :as loads]
            [decidotron.ui.mdc-components :as material]
            [decidotron.ui.routing :as routing]
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

(defsc IssueEntry [this {:keys [dbas.issue/title dbas.issue/summary dbas.issue/slug]}]
  {:query [:dbas.issue/title :dbas.issue/summary :dbas.issue/slug]
   :ident [:issue-entry/by-slug :dbas.issue/slug]}
  (material/list-item #js {:onClick #(routing/nav-to! this :positions {:slug slug})}
    (material/list-item-text #js {:primaryText   title
                                  :secondaryText summary})))

(def ui-issue-entry (prim/factory IssueEntry {:keyfn :dbas.issue/slug}))

(defsc IssueList [this {:keys [db/id router/page issues dbas/connection]}]
  {:query         [:db/id :router/page
                   [:dbas/connection '_]
                   {:issues (prim/get-query loads/Issue)}]
   :ident         (fn [] [page id])
   :initial-state (fn [_]
                    {:db/id       1
                     :router/page :PAGE.discuss/issues})}
  (when (not (some? issues))
    (loads/load-issues this connection [page id :issues]))
  (if (some? issues)
    (material/mdc-list #js {:twoLine true}
      (map ui-issue-entry (filter (comp not empty?) issues)))
    (dom/p "Issues could not be loaded. :-(")))

(def ui-issue-list (prim/factory IssueList))

(defsc Positions [this {:keys [db/id router/page dbas/connection root/current-page positions]}]
  {:query [:db/id :router/page
           [:root/current-page '_]
           [:dbas/connection '_]
           {:positions (prim/get-query loads/Positions)}]
   :ident          (fn [] [page id])
   :initial-state {:db/id 1
                   :router/page :PAGE.discuss.dialog/positions}}
  (dom/div
    (let [slug (get-in current-page [:positions :route-params :slug])]
      (when-not positions
        (loads/load-positions this connection
          (df/multiple-targets
            (df/replace-at [:PAGE.discuss/dialog 1 :bubbles]) ; TODO get rid of this
            (df/replace-at [:PAGE.discuss.dialog/positions 1 :positions]))
          slug))
      (if (:positions positions)
        (choices/ui-choice-list (prim/computed
                                  {:choice-list/choices (:positions positions)}
                                  {:choices-onClick-fn
                                   (fn [url]
                                     ; /town-has-to-cut-spending/attitude/11?history=^
                                     (let [position (-> url
                                                      (clojure.string/split "?") first
                                                      (clojure.string/split "/") (nth 3)
                                                      js/parseInt)]
                                       (routing/nav-to! this :attitude {:slug slug :position position})))}))
        (dom/p "Still loading!")))))

(defsc Attitude [this {:keys [db/id router/page dbas/connection attitudes root/current-page]}]
  {:query [:db/id :router/page :attitudes [:dbas/connection '_] [:root/current-page '_]]
   :ident          (fn [] [page id])
   :initial-state {:db/id 1
                   :router/page :PAGE.discuss.dialog/attitude}}
  (let [params (get-in current-page [:attitude :route-params])]
    (when-not attitudes
      (loads/load-attitudes this connection (df/multiple-targets
                                              (df/replace-at [:PAGE.discuss/dialog 1 :bubbles])
                                              (df/replace-at [:PAGE.discuss.dialog/attitude 1 :attitudes]))
        (:slug params) (:position params)))

    (let [halves #js {:columns 2}]
      (material/grid #js {}
        (material/row #js {}
          (material/cell halves (material/button #js {:raised true} "I agree"))
          (material/cell halves (material/button #js {} "I disagree")))))))

(defsc-router DialogRouter [this {:keys [db/id router/page]}]
  {:router-id :discuss.dialog/router
   :default-route Positions
   :ident          (fn [] [page id])
   :router-targets {:PAGE.discuss.dialog/positions Positions
                    :PAGE.discuss.dialog/attitude Attitude
                    :PAGE.discuss.dialog/justify Positions
                    :PAGE.discuss.dialog/react Positions}}
  (dom/p "Oh no!"))

(def ui-dialog-router (prim/factory DialogRouter))

(defsc DialogScreen [this {:keys [db/id router/page dbas/connection dialog-router slug bubbles] :as props}]
  {:query         [:db/id :router/page [:dbas/connection '_]
                   {:bubbles (prim/get-query loads/Positions)}
                   {:dialog-router (prim/get-query DialogRouter)}
                   :slug]
   :ident         (fn [] [page id])
   :initial-state (fn [params]
                    {:db/id 1
                     :router/page :PAGE.discuss/dialog
                     :dialog-router (prim/get-initial-state DialogRouter {})})}
  (dom/div :.dialog-area.mdc-elevation--z2
    (bubbles/ui-bubble-area {:bubble-area/bubbles (:bubbles bubbles)})
    (ui-dialog-router dialog-router)))

(defsc-router DiscussRouter [this {:keys [db/id router/page]}]
  {:router-id      :discuss/router
   :ident          (fn [] [page id])
   :default-route  IssueList
   :router-targets {:PAGE.discuss/issues    IssueList
                    :PAGE.discuss/dialog DialogScreen}}
  (dom/div "Something went wrong. :-(")) ; TODO Replace with something more useful for the user

(def ui-discuss-router (prim/factory DiscussRouter))

(defsc Discuss [this {:keys [db/id router/page discuss/router]}]
  {:query         [:db/id
                   :router/page
                   {:discuss/router (prim/get-query DiscussRouter)}]
   :ident         (fn [] [page id])
   :initial-state (fn [params]
                    {:db/id               1
                     :router/page         :PAGE/discuss
                     :discuss/router      (prim/get-initial-state DiscussRouter {})})}
  (ui-discuss-router router))