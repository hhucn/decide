(ns decidotron.ui.routing
  (:require
    [pushy.core :as pushy]
    [fulcro.client.primitives :as prim]
    [fulcro.incubator.dynamic-routing :as dr]
    [clojure.string :as str]))

;; To keep track of the global HTML5 pushy routing object
(def history (atom nil))

;; To indicate when we should turn on URI mapping. This is so you can use with devcards (by turning it off)
(defonce use-html5-routing (atom true))

(defn- url->route [url]
  (rest (str/split url #"/")))

(defn- route->url [path]
  (str \/ (str/join \/ path)))

(defn change-route! [this new-route]
  (if (and @history @use-html5-routing)
    (let [url (route->url new-route)]
      (js/console.log "Setting url to" url)
      (pushy/set-token! @history url))
    (dr/change-route (prim/any->reconciler this) new-route)))

(defn start-routing [reconciler]
  (when (and @use-html5-routing (not @history))
    (reset! history (pushy/pushy #(do (js/console.log "URL changed to:" %)
                                      (dr/change-route reconciler %))
                      url->route))
    (pushy/start! @history)))