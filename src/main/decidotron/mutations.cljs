(ns decidotron.mutations
  (:require
    [fulcro.client.mutations :as m :refer [defmutation]]
    [dbas.client :as dbas]
    [fulcro.client.primitives :as prim]))

(defmutation login [{:keys [nickname password]}]
  (action [{:keys [state]}]
    (js/console.log "Login!"))
  (dbas [{:keys [ast state]}]
    (-> ast
      (assoc :key 'dbas/login)
      (assoc-in [:params :connection] (:dbas/connection @state))
      (m/with-target [:dbas/connection]))))

(defmutation set-dbas-connection [{:keys [dbas-state]}]
  (action [{:keys [state]}]
    (swap! state assoc :dbas/connection dbas-state)))

(defmutation toggle-drawer [{:keys [drawer/id]}]
  (action [{:keys [state]}]
    (swap! state update-in [:drawer/by-id id :drawer/open?] not)))

;;;;

(defn- update-preferences* [{:keys [state ast]}]
  (let [s         @state
        issue-key (get-in s [:root/current-page :preferences :route-params :slug])
        pref-list (get-in s [:preference-list/slug issue-key])]
    (-> (assoc ast :key 'update-preferences)
      (m/with-params {:preference-list (select-keys pref-list [:preferences :preference-list/slug])}))))

(defmutation prefer [{:keys [position/id]}]
  (action [{:keys [state component]}]
    (swap! state m/integrate-ident* [:dbas.position/id id] :append (concat (prim/get-ident component) [:preferences])))
  (remote [env]
    (update-preferences* env)))

(defmutation un-prefer [{:keys [position/id]}]
  (action [{:keys [state component]}]
    (swap! state m/remove-ident* [:dbas.position/id id] (concat (prim/get-ident component) [:preferences])))
  (remote [env]
    (update-preferences* env)))

(defn- swap-levels
  "Swaps to levels l1 and l2. They have to be in preferences!"
  [preferences l1 l2]
  (assoc preferences
    l2 (get preferences l1)
    l1 (get preferences l2)))

(defn- preference-up* [preferences level]
  (if (< level 1)
    preferences
    (swap-levels preferences level (dec level))))

(defn- preference-down* [preferences level]
  (if (>= level (dec (count preferences)))
    preferences
    (swap-levels preferences level (inc level))))

(defmutation preference-up [{:keys [level]}]
  (action [{:keys [state component]}]
    (swap! state update-in (conj (prim/get-ident component) :preferences) preference-up* level))
  (remote [env]
    (update-preferences* env)))

(defmutation preference-down [{:keys [level]}]
  (action [{:keys [state component]}]
    (swap! state update-in (conj (prim/get-ident component) :preferences) preference-down* level))
  (remote [env]
    (update-preferences* env)))