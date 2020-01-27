(ns decide.application
  (:require [com.fulcrologic.fulcro.networking.http-remote :as net]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.fulcro.rendering.keyframe-render2 :as keyframe-render2]
            [com.fulcrologic.fulcro.components :refer [defsc]]
            [taoensso.timbre :as log]
            [clojure.string :refer [split]]
            [decide.routing :as routing]
            [com.fulcrologic.fulcro.data-fetch :as df]))

(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))

(defsc Phase [_ _]
  {:query [:phase/id :phase/allowed :phase/starts]
   :ident :phase/id})


(defonce SPA (app/fulcro-app
               {;; This ensures your client can talk to a CSRF-protected server.
                ;; See middleware.clj to see how the token is embedded into the HTML
                ;:optimized-render! keyframe-render2/render!
                :client-did-mount (fn client-did-mount [app]
                                    (routing/initial-route! app)
                                    (df/load! app :all-phases Phase)
                                    (df/load! app :current-phase Phase))
                :remotes          {:remote (net/fulcro-http-remote
                                             {:url                "/api"
                                              :request-middleware secured-request-middleware})}}))

(routing/start-history SPA)

(comment
  (-> SPA (::app/runtime-atom) deref ::app/indexes))
