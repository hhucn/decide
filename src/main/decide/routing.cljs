(ns decide.routing
  (:require
    [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
    [taoensso.timbre :as log]
    [goog.events :as events]
    [clojure.string :refer [split]])
  (:import [goog.history Html5History EventType]))


(defn- make-history []
  (doto (Html5History.)
    (.setPathPrefix (str js/window.location.protocol
                      "//"
                      js/window.location.host))
    (.setUseFragment false)))


(defn- get-token []
  (str js/window.location.pathname js/window.location.search))

(defn- handle-url-change [e]
  ;; log the event object to console for inspection
  ;; and let's see the token
  (log/debug (str "Navigating: " (get-token) " Nav: " (.-isNavigation e)))
  ;; we are checking if this event is due to user action,
  ;; such as click a link, a back button, etc.
  ;; as opposed to programmatically setting the URL with the API
  (when-not (.-isNavigation e)
    ;; in this case, we're setting it
    (log/debug "Token set programmatically")
    ;; let's scroll to the top to simulate a navigation
    (js/window.scrollTo 0 0)))


(defonce history
  (doto
    (make-history)
    (goog.events/listen EventType.NAVIGATE handle-url-change)
    (.setEnabled true)))

(defn update-url [new-token]
  (if (= new-token (.getToken history))
    (.replaceToken history new-token)
    (.setToken history new-token)))

(defn start-history [app]
  (events/listen js/window
    goog.events.EventType.POPSTATE
    (fn [e]
      (.preventDefault e)
      (let [new-path (get-token)]
        (log/debug "onpopstate!" e)
        (log/debug :new-path new-path)
        (.setToken history new-path)
        (dr/change-route app (rest (split new-path "/")))))))