(ns decide.model.account
  (:require
    [taoensso.timbre :as log]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]))

(defn user-path
  "Normalized path to a user entity or field in Fulcro state-map"
  ([id field] [:account/id id field])
  ([id] [:account/id id]))

(defn insert-user*
  "Insert a user into the correct table of the Fulcro state-map database."
  [state-map {:keys [:account/id] :as user}]
  (assoc-in state-map (user-path id) user))

(defmutation update-display-name [params]
  (action [{:keys [state]}]
    (swap! state insert-user* params))
  (remote [_] true))

(defsc Account [_ _]
  {:query [:account/id :account/display-name :account/firstname :account/lastname :account/email]
   :ident :account/id})