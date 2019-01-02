(ns decidotron.client
  (:require [fulcro.client :as fc]
            [decidotron.ui.root :as root]
            [fulcro.client.network :as net]
            [decidotron.remotes.dbas :refer [dbas-remote]]
            [decidotron.ui.routing :as routing]
            [fulcro.client.primitives :as prim]))

(defonce app (atom nil))

(defn mount []
  (reset! app (fc/mount @app root/Root "app")))

(defn start []
  (mount))

(def secured-request-middleware
  ;; The CSRF token is embedded in the server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))

(defn ^:export init []
  (reset! app (fc/new-fulcro-client
                ;; This ensures your client can talk to a CSRF-protected server.
                ;; See middleware.clj to see how the token is embedded into the HTML
                :networking {:remote (net/fulcro-http-remote
                                       {:url                "/api"
                                        :request-middleware secured-request-middleware})
                             :dbas   dbas-remote}
                :started-callback (fn [{:keys [reconciler] :as app}]
                                    (routing/start-routing (prim/app-root reconciler)))))
  (start))
