(ns decidotron.server-components.config
  (:require
    [mount.core :refer [defstate args]]
    [fulcro.server :as server]))

(defstate config
  :start (let [{:keys [config] :or {config "config/dev.edn"}} (args)]
           (server/load-config {:config-path config})))

