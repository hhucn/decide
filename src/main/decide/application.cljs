(ns decide.application
  (:require [com.fulcrologic.fulcro.networking.http-remote :as net]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.fulcro.rendering.keyframe-render2 :as keyframe-render2]
            [taoensso.timbre :as log]
            [clojure.string :refer [split]]
            [decide.routing :as routing]))

(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))


(defonce SPA (app/fulcro-app
               {;; This ensures your client can talk to a CSRF-protected server.
                ;; See middleware.clj to see how the token is embedded into the HTML
                :optimized-render! keyframe-render2/render!
                :client-did-mount  #(dr/change-route SPA (rest (split js/document.location.pathname #"/")))
                :remotes           {:remote (net/fulcro-http-remote
                                              {:url                "/api"
                                               :request-middleware secured-request-middleware})}}))

(routing/start-history SPA)

(comment
  (-> SPA (::app/runtime-atom) deref ::app/indexes))
