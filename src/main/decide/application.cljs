(ns decide.application
  (:require [com.fulcrologic.fulcro.networking.http-remote :as net]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [goog.events :as events])
  (:import [goog.history Html5History EventType]))

(def secured-request-middleware
  ;; The CSRF token is embedded via server_components/html.clj
  (->
    (net/wrap-csrf-token (or js/fulcro_network_csrf_token "TOKEN-NOT-IN-HTML!"))
    (net/wrap-fulcro-request)))

#_(js/setTimeout #(.setToken history "/bla"), 10000)

(defonce SPA (app/fulcro-app
               {;; This ensures your client can talk to a CSRF-protected server.
                ;; See middleware.clj to see how the token is embedded into the HTML
                :remotes {:remote (net/fulcro-http-remote
                                    {:url                "/api"
                                     :request-middleware secured-request-middleware})}}))

(defn make-history []
  (doto (Html5History.)
    (.setPathPrefix (str js/window.location.protocol
                      "//"
                      js/window.location.host))
    (.setUseFragment false)))

(defn get-token []
  (str js/window.location.pathname js/window.location.search))

(defn handle-url-change [e]
  ;; log the event object to console for inspection
  (js/console.log e)
  ;; and let's see the token
  (js/console.log (str "Navigating: " (get-token) " Nav: "(.-isNavigation e)))
  ;; we are checking if this event is due to user action,
  ;; such as click a link, a back button, etc.
  ;; as opposed to programmatically setting the URL with the API
  (when-not (.-isNavigation e)
    ;; in this case, we're setting it
    (js/console.log "Token set programmatically")
    ;; let's scroll to the top to simulate a navigation
    (js/window.scrollTo 0 0)))


(defonce history
  (doto
    (make-history)
    (goog.events/listen EventType.NAVIGATE #(handle-url-change %))
    (.setEnabled true)))

(events/listen js/window
  goog.events.EventType.POPSTATE
  (fn [e]
    (.preventDefault e)
    (let [new-path (get-token)]
      (js/console.log "onpopstate!" e)
      (js/console.log :new-path new-path)
      (.setToken history new-path)
      (dr/change-route SPA (rest (clojure.string/split new-path "/"))))))


(comment
  (-> SPA (::app/runtime-atom) deref ::app/indexes))
