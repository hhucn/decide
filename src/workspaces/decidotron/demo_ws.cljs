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

(ws/defcard root-card
  {::wsm/align {:flex 1}}
  (ct.fulcro/fulcro-card
    {::f.portal/root       root-ui/Root
     ::f.portal/wrap-root? false
     ::f.portal/app        {:networking    {:dbas dbas-remote}
                            :initial-state (prim/get-initial-state root-ui/Root {:connection dbas/connection})}}))

(ws/defcard input-field
  (ct.fulcro/fulcro-card
    {::f.portal/root ui/InputField}))

(ws/defcard login-form
  {::wsm/card-width  4
   ::wsm/card-height 9
   ::wsm/align       {:flex 1}}
  (ct.fulcro/fulcro-card
    {::f.portal/root ui/LoginForm
     ::f.portal/app  {:networking    {:dbas dbas-remote}
                      :initial-state {:dbas/connection dbas/connection
                                      :ui/root         (prim/get-initial-state ui/LoginForm {:id 1 :nickname "bjebb" :password "secret"})}}}))