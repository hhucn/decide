(ns decidotron.api
  (:require
    [fulcro.client.mutations :as m :refer [defmutation]]
    [dbas.client :as dbas]
    [decidotron.cookies :as cookie]
    [fulcro.client.primitives :as prim]))

(defmutation login [{:keys [nickname password]}]
  (action [{:keys [state]}]
    (js/console.log "Login!"))
  (dbas [{:keys [ast state]}]
    (-> ast
      (assoc :key 'dbas/login)
      (assoc-in [:params :connection] (:dbas/connection @state))
      (m/with-target [:dbas/connection]))))


(defn logout* [state]
  (js/console.log state)
  (-> state
    (assoc :dbas/connection
           {::dbas/login-status ::dbas/logged-out
            ::dbas/base         (str js/dbas_host "/api")})
    (dissoc :preference-list/slug)
    (dissoc :preferences/slug)))

(defmutation logout [_]
  (action [{:keys [state]}]
    (cookie/remove! cookie/decidotron-token)
    (swap! state logout*)))

(defmutation set-dbas-connection [{:keys [dbas-state]}]
  (action [{:keys [state]}]
    (swap! state assoc :dbas/connection dbas-state)))

(defmutation toggle-drawer [{:keys [drawer/id]}]
  (action [{:keys [state]}]
    (swap! state update-in [:drawer/by-id id :drawer/open?] not)))

;;;;

(defn- update-preferences* [{:keys [state ast ref]}]
  (let [pref-list (get-in @state ref)]
    (-> (assoc ast :key `update-preferences)
      (m/with-params {:preference-list (select-keys pref-list [:preferences :preference-list/slug])}))))

(defmutation prefer [{:keys [position/id]}]
  (action [{:keys [state ref]}]
    (swap! state m/integrate-ident* [:dbas.position/id id] :append (concat ref [:preferences])))
  (remote [env]
    (js/console.log env)
    (update-preferences* env)))

(defmutation un-prefer [{:keys [position/id]}]
  (action [{:keys [state ref]}]
    (swap! state m/remove-ident* [:dbas.position/id id] (concat ref [:preferences])))
  (remote [env]
    (update-preferences* env)))

;;;;

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