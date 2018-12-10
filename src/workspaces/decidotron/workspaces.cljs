(ns decidotron.workspaces
  (:require
    [nubank.workspaces.core :as ws]
    [decidotron.demo-ws]
    [decidotron.material-ws]))

(defonce init (ws/mount))
