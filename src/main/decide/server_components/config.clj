(ns decide.server-components.config
  (:require
    [mount.core :refer [defstate args]]
    [com.fulcrologic.fulcro.server.config :refer [load-config!]]
    [clojure.pprint :refer [pprint]]
    [taoensso.timbre :as log]))


(defn configure-logging! [config]
  (let [{:keys [taoensso.timbre/logging-config]} config]
    (log/info "Configuring Timbre with " logging-config)
    (log/merge-config! logging-config)
    (log/merge-config!
      {:middleware [(fn [data]
                      (update data :vargs (partial mapv #(if (string? %)
                                                           %
                                                           (with-out-str (pprint %))))))]})))


(defstate config
  :start (let [{:keys [config] :or {config "config/dev.edn"}} (args)
               configuration (load-config! {:config-path config})]
           (log/info "Loaded config" config)
           (configure-logging! configuration)
           configuration))

