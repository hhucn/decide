(ns decidotron.client
  (:require [fulcro.client :as fc]
            [decidotron.ui.root :as root]
            [fulcro.client.network :as net]
            [decidotron.remotes.dbas :refer [dbas-remote]]
            [decidotron.ui.routing :as routing]
            [decidotron.ui.models :as models]
            [decidotron.mutations :as ms]
            [fulcro.client.primitives :as prim]
            [fulcro.client.data-fetch :as df]
            [goog.crypt.base64 :as b64])
  (:import goog.net.Cookies))

(defonce app (atom nil))

(defn mount []
  (reset! app (fc/mount @app root/Root "app")))

(defn start []
  (mount))

(defn wrap-api->root-middleware [handler]
  (fn [request]
    (handler request)))

(def secured-request-middleware
  ;; The CSRF token is embedded in the server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)
    (wrap-api->root-middleware)))

(defn- parse-json [json]
  (js->clj (.parse js/JSON json) :keywordize-keys true))

(defn- ->initial-dbas-data [token]
  (let [{:keys [id nickname]} (-> token (clojure.string/split #"\.") second b64/decodeString parse-json)]
    {:dbas.client/base "http://0.0.0.0/api"
     :dbas.client/id id
     :dbas.client/nickname nickname
     :dbas.client/login-status :dbas.client/logged-in
     :dbas.client/token token}))

(defn get-user-state-from-cookie [app-root]
  (when-let [token (.get (Cookies. js/document) "decidotron-token" nil)]
    (prim/transact! app-root `[(ms/set-dbas-connection {:dbas-state ~(->initial-dbas-data token)})])))

(defn ^:export init []
  (reset! app (fc/new-fulcro-client
                ;; This ensures your client can talk to a CSRF-protected server.
                ;; See middleware.clj to see how the token is embedded into the HTML
                :networking {:remote (net/fulcro-http-remote
                                       {:url                "/api"
                                        :request-middleware secured-request-middleware})
                             :dbas   dbas-remote}
                :started-callback (fn [{:keys [reconciler] :as app}]
                                    (get-user-state-from-cookie (prim/app-root reconciler))
                                    (routing/start-routing (prim/app-root reconciler))
                                    (df/load reconciler :dbas.issue/positions models/Position {:params {:dbas.issue/slug "was-sollen-wir-mit-20-000eur-anfangen"}}))))
  (start))
