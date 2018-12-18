(ns decidotron.mutations
  (:require
    [fulcro.client.mutations :as m :refer [defmutation]]
    [dbas.client :as dbas]))

(defmutation dbas/login [{:keys [nickname password]}]
  (action [{:keys [state]}]
    (js/console.log "Login!"))
  (dbas [{:keys [ast state]}]
    (-> ast
      (assoc-in [:params :connection] (:dbas/connection @state))
      (m/with-target [:dbas/connection]))))