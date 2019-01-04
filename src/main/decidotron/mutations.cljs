(ns decidotron.mutations
  (:require
    [fulcro.client.mutations :as m :refer [defmutation]]
    [dbas.client :as dbas]))

(defmutation login [{:keys [nickname password]}]
  (action [{:keys [state]}]
    (js/console.log "Login!"))
  (dbas [{:keys [ast state]}]
    (-> ast
      (assoc :key 'dbas/login)
      (assoc-in [:params :connection] (:dbas/connection @state))
      (m/with-target [:dbas/connection]))))

(defmutation toggle-drawer [{:keys [drawer/id]}]
  (action [{:keys [state]}]
    (swap! state update-in [:drawer/by-id id :drawer/open?] not)))

(defmutation set-page-params [{:keys [handler] :as params}]
  (action [{:keys [state]}]
    (swap! state assoc-in [:root/current-page handler] params)))