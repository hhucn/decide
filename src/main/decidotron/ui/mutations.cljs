(ns decidotron.ui.mutations
  (:require
    [cljs.core.async :refer [go <!] :as async]
    [fulcro.client.mutations :as m :refer [defmutation]]))

(defmutation login [{:keys [nickname password] :as params}]
  (action [{:keys [state]}]
    (go
      (let [s @state]
        (if-let [new-conn (<! (dbas.client/login (:dbas/connection s) nickname password))]
          (reset! state (assoc s :dbas/connection new-conn))
          (js/console.error "Pech gehabt"))))))