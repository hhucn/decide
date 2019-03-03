(ns decidotron.ui.routing
  (:require
    [fulcro.client.mutations :as m :refer [defmutation]]
    [pushy.core :as pushy]
    [bidi.verbose :refer [branch leaf param]]
    [bidi.bidi :as bidi]
    [fulcro.client.primitives :as prim]
    [fulcro.incubator.dynamic-routing :as dr]))

;; To keep track of the global HTML5 pushy routing object
(def history (atom nil))

;; To indicate when we should turn on URI mapping. This is so you can use with devcards (by turning it off)
(defonce use-html5-routing (atom true))

(def app-routes
  "The bidi routing map for the application. The leaf keywords are the route names. Parameters
  in the route are available for use in the routing algorithm as :param/param-name."
  (branch "/"
    (leaf "" :main)
    (leaf "login" :login)
    (branch "preferences/"
      (param :slug)
      (leaf "" :preferences))))

(defn change-route! [this new-route]
  (if (and @history @use-html5-routing)
    (let [path (str \/ (clojure.string/join \/ new-route))]
      (js/console.log ["Setting path to" path])
      (pushy/set-token! @history path))
    (dr/change-route (prim/any->reconciler this) new-route)))

(defn- match->path [{:keys [handler route-params]}]
  (rest (clojure.string/split (apply bidi/path-for app-routes handler (flatten (seq route-params))) #"/")))

(defn start-routing [reconciler]
  (when (and @use-html5-routing (not @history))
    (reset! history (pushy/pushy #(do (js/console.log ["URL CHANGE DETECTED" %])
                                      (dr/change-route reconciler %))
                      (partial bidi/match-route app-routes)
                      :identity-fn match->path))
    (pushy/start! @history)))