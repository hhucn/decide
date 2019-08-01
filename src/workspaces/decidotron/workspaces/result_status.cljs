(ns decidotron.workspaces.result-status
  (:require
    [nubank.workspaces.core :as ws :refer [defcard]]
    [nubank.workspaces.card-types.fulcro :as ct.fulcro]
    [nubank.workspaces.card-types.react :as ct.react]
    [nubank.workspaces.lib.fulcro-portal :as f.portal]
    [decidotron.ui.components.result-status :as result-status-ui]))

(def status-box-state {:dbas.position/id     42
                       :dbas.position/text   "Steckerleisten gekauft werden sollten"
                       :status/state         :status/in-work
                       :status/content       "#Test\n\nEs wird geguckt, was gebraucht wird.\n<script>alert(\"I'm evil!\");</script>"
                       :status/last-modified #inst "2019-07-26"})

(defcard status-box-admin-molecule
  (ct.fulcro/fulcro-card
    {::f.portal/root          result-status-ui/StatusBox
     ::f.portal/initial-state status-box-state
     ::f.portal/computed {:admin? true}}))

(defcard status-box-non-admin-molecule
  (ct.fulcro/fulcro-card
    {::f.portal/root          result-status-ui/StatusBox
     ::f.portal/initial-state status-box-state}))

(defcard status-badge-atoms
  (ct.react/react-card
    [(result-status-ui/status-badge :status/done)
     (result-status-ui/status-badge :status/in-work)
     (result-status-ui/status-badge :status/cancelled)]))

(defcard markdown-render-atom
  (ct.react/react-card
    (result-status-ui/markdown-render "#h1\n##h2\nTest\n\n<script>alert(\"I'm evil!\");</script>")))

(defcard last-modified-label-atom
  (ct.react/react-card
    (result-status-ui/last-modified-label #inst "2019-07-26")))
