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
    [hiccup.page :refer [html5 include-js include-css]]
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
  (fn api-middleware [request]
    (if (= uri (:uri request))
      (server/handle-api-request
        ;; Sub out a pathom parser here if you want to use pathom.
        (fn phantom-parser [env query]
          (async/<!! (server-parser env query)))
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
    [:html {:lang "de"}
     [:head {:lang "de"}
      [:title "Decide"]
      [:meta {:charset "utf-8"}]
      [:link
       {:href  "/favicons/apple-touch-icon.png",
        :sizes "180x180",
        :rel   "apple-touch-icon"}]
      [:link
       {:href  "/favicons/favicon-32x32.png",
        :sizes "32x32",
        :type  "image/png",
        :rel   "icon"}]
      [:link
       {:href  "/favicons/favicon-16x16.png",
        :sizes "16x16",
        :type  "image/png",
        :rel   "icon"}]
      [:link
       {:color "#006abf",
        :href  "/favicons/safari-pinned-tab.svg",
        :rel   "mask-icon"}]
      [:link {:href "/favicons/favicon.ico", :rel "shortcut icon"}]
      [:meta {:content "#FFFFFF", :name "msapplication-TileColor"}]
      [:meta {:name "viewport" :content "width=device-width, initial-scale=1, maximum-scale=1, user-scalable=no"}]
      [:link {:rel "shortcut icon" :href "data:image/x-icon;," :type "image/x-icon"}]
      (include-css
        "/distributions/bootstrap-4.3.1-dist/css/bootstrap.css"
        "/distributions/fontawesome-free-5.7.2-web/css/all.min.css"
        "/css/main.css")
      [:script (str "var fulcro_network_csrf_token = '" csrf-token "';")]
      [:script (str "var dbas_host ='" (get-in config [:dbas :base]) "';")]]
     [:body
      [:div#app]
      (include-js
        "/distributions/jQuery/jquery-3.3.1.slim.min.js"
        "/distributions/bootstrap-4.3.1-dist/js/bootstrap.bundle.js"
        "/js/main/main.js")
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
  (fn html-route-middleware [{:keys [uri anti-forgery-token] :as req}]
    (cond
      ;; fetch the correct token from the user and redirect to the site without the token parameter.
      (get-in req [:params :token])
      (let [response (resp/redirect uri)]
        (println "Get user token and redirect")
        (if-let [t (ensure-token req)]
          (resp/set-cookie response "decidotron-token" t {:path "/"})
          response))

      (#{"/"} uri)
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

      ; This is just an additional layer, to protect if the CSRF Token is lost.
      #_(server/wrap-protect-origins {:allow-when-origin-missing? false
                                      :legal-origins              legal-origins})
      (wrap-html-routes)
      ;; If you want to set something like session store, you'd do it against
      ;; the defaults-config here (which comes from an EDN file, so it can't have
      ;; code initialized).
      ;; E.g. (wrap-defaults (assoc-in defaults-config [:session :store] (my-store)))
      (wrap-defaults defaults-config)
      wrap-gzip)))
