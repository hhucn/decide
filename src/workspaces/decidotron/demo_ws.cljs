(ns decidotron.demo-ws
  (:require [fulcro.client.primitives :as fp]
            [fulcro.client.localized-dom :as dom]
            [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.fulcro :as ct.fulcro]
            [nubank.workspaces.lib.fulcro-portal :as f.portal]
            [fulcro.client.mutations :as fm]
            [decidotron.ui.components :as ui]
            [decidotron.ui.root :as root-ui]
            [dbas.client :as dbas]
            [fulcro.client.primitives :as prim]
            [decidotron.ui.components.preferences :as pref-ui]
            [decidotron.remotes.dbas :refer [dbas-remote]]))

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

(ws/defcard unselected-preference-card
  (ct.fulcro/fulcro-card
    {::f.portal/root          pref-ui/PreferenceListItem
     ::f.portal/initial-state (fn [] #:dbas.position{:id   1
                                                     :text "wir eine Katze holen sollten."
                                                     :cost 50
                                                     :pros [#:dbas.statement{:id            42 :text "das eine gute Idee ist"
                                                                             :is-supportive true
                                                                             :argument-id   3}]
                                                     :cons []})}))