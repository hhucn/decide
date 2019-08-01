(ns decidotron.workspaces
  (:require
    [nubank.workspaces.core :as ws]
    [decidotron.demo-ws]
    [decidotron.workspaces.result-status]))

(defonce init (ws/mount))
