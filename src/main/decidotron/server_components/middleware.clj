(ns decidotron.server-components.middleware
  (:require
    [clojure.core.async :as async]
    [decidotron.server-components.config :refer [config]]
    [decidotron.api :refer [server-parser]]
    [decidotron.server-components.token :as token]
    [mount.core :refer [defstate]]
    [fulcro.server :as server]
    [ring.middleware.defaults :refer [wrap-defaults]]
    [ring.middleware.gzip :refer [wrap-gzip]]
    [ring.util.response :refer [response file-response resource-response]]
    [ring.util.response :as resp]
    [hiccup.page :refer [html5]]
    [fulcro.logging :as log]
    [buddy.core.keys :refer [str->public-key]]))

(def ^:private not-found-handler
  (fn [req]
    {:status  404
     :headers {"Content-Type" "text/plain"}
     :body    "NOPE"}))

;; ================================================================================
;; Replace this with a pathom Parser once you get past the beginner stage.
;; This one supports the defquery-root, defquery-entity, and defmutation as
;; defined in the book, but you'll have a much better time parsing queries with
;; Pathom.
;; ================================================================================
(log/set-level! :all)
(defn wrap-api [handler uri]
  (fn [request]
    (if (= uri (:uri request))
      (server/handle-api-request
        ;; Sub out a pathom parser here if you want to use pathom.
        (fn [env query] (let [result (async/<!! (server-parser env query))] result))
        ;; this map is `env`. Put other defstate things in this map and they'll be
        ;; in the mutations/query env on server.
        {:config config}
        (:transit-params request))
      (handler request))))

;; ================================================================================
;; Dynamically generated HTML. We do this so we can safely embed the CSRF token
;; in a js var for use by the client.
;; ================================================================================
(defn index [csrf-token]
  (html5
    [:html {:lang "en"}
     [:head {:lang "en"}
      [:title "Decidotron"]
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"}]
      [:link {:rel "shortcut icon" :href "data:image/x-icon;," :type "image/x-icon"}]
      [:link {:rel       "stylesheet" :href "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"
              :integrity "sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" :crossorigin "anonymous"}]
      [:link {:rel         "stylesheet" :href "https://use.fontawesome.com/releases/v5.7.2/css/all.css"
              :integrity   "sha384-fnmOCqbTlWIlj8LyTjo7mOUStjsKC4pOpQbqyi7RrhN7udi9RwhKkMHpvLbHG9Sr"
              :crossorigin "anonymous"}]
      [:link {:rel "stylesheet" :href "/css/main.css"}]
      [:script (str "var fulcro_network_csrf_token = '" csrf-token "';")]
      [:script (str "var dbas_host ='" (get-in config [:dbas :base]) "';")]]
     [:body
      [:div#app]
      [:script {:src       "https://code.jquery.com/jquery-3.3.1.slim.min.js"
                :integrity "sha384-q8i/X+965DzO0rT7abK41JStQIAqVgRVzpbzo5smXKp4YfRvH+8abtTE1Pi6jizo" :crossorigin "anonymous"}]
      [:script {:src       "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js"
                :integrity "sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM" :crossorigin "anonymous"}]
      [:script {:src "/js/main/main.js"}]
      [:script "decidotron.client.init();"]]]))

;; ================================================================================
;; Workspaces can be accessed via shadow's http server on http://localhost:8023/workspaces.html
;; but that will not allow full-stack fulcro cards to talk to your server. This
;; page embeds the CSRF token, and is at `/wslive.html` on your server (i.e. port 3000).
;; ================================================================================
(defn wslive [csrf-token]
  (html5
    [:html {:lang "en"}
     [:head {:lang "en"}
      [:title "devcards"]
      [:meta {:charset "utf-8"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"}]
      [:link {:href "https://cdnjs.cloudflare.com/ajax/libs/semantic-ui/2.4.1/semantic.min.css"
              :rel  "stylesheet"}]
      [:link {:rel "shortcut icon" :href "data:image/x-icon;," :type "image/x-icon"}]
      [:script (str "var fulcro_network_csrf_token = '" csrf-token "';")]]
     [:body
      [:div#app]
      [:script {:src "js/workspaces/main.js"}]]]))

(defn ensure-token [req]
  (let [query-token (get-in req [:params :token])]
    (try
      (when-let [payload (token/unsign query-token)]
        (if (= "tmp" (:sub payload))
            (token/refresh query-token)
            query-token))
      (catch Exception e
        (println "Token could not be verified" e)))))

(defn wrap-html-routes [ring-handler]
  (fn [{:keys [uri anti-forgery-token] :as req}]
    (cond
      ;; fetch the correct token from the user and redirect to the site without the token parameter.
      (get-in req [:params :token])
      (let [response (resp/redirect uri)]
        (println "Get user token and redirect")
        (if-let [t (ensure-token req)]
          (resp/set-cookie response "decidotron-token" t {:path "/"})
          response))

      (#{"/" "/index.html"} uri)
      (-> (resp/response (index anti-forgery-token))
        (resp/content-type "text/html"))

      ;; See note above on the `wslive` function.
      (#{"/wslive.html"} uri)
      (-> (resp/response (wslive anti-forgery-token))
        (resp/content-type "text/html"))

      (#{"/api"} uri)
      (ring-handler req)

      :else
      (-> (resp/response (index anti-forgery-token))
        (resp/content-type "text/html"))
      #_(ring-handler req))))                               ; Default -> 403

(defstate middleware
  :start
  (let [defaults-config (:ring.middleware/defaults-config config)
        legal-origins   (get config :legal-origins #{"localhost"})]
    (-> not-found-handler
      (wrap-api "/api")
      server/wrap-transit-params
      server/wrap-transit-response
      (server/wrap-protect-origins {:allow-when-origin-missing? false
                                    :legal-origins              legal-origins})
      (wrap-html-routes)
      ;; If you want to set something like session store, you'd do it against
      ;; the defaults-config here (which comes from an EDN file, so it can't have
      ;; code initialized).
      ;; E.g. (wrap-defaults (assoc-in defaults-config [:session :store] (my-store)))
      (wrap-defaults defaults-config)
      wrap-gzip)))
