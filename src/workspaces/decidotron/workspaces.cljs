(ns decidotron.workspaces
  (:require
    [nubank.workspaces.core :as ws]
    [decidotron.demo-ws]))

(defonce init (ws/mount))
