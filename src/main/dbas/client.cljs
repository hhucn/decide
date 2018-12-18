(ns dbas.client
  (:require
    [cljs-http.client :as http]
    [cljs.core.async :refer [go <!]]
    [goog.string :as gstring]))

(defn new-connection [] {::base "https://dbas.cs.uni-duesseldorf.de/api"})
(def connection (new-connection))

(defn logged-in? [conn]
  (and (contains? conn ::token)
    (contains? conn ::nickname)))

(comment
  (logged-in? {::base "https://dbas.cs.uni-duesseldorf.de/api"})
  (logged-in? {::base     "https://dbas.cs.uni-duesseldorf.de/api"
               ::nickname "Björn"
               ::token    "abcTOKEN123"}))

(defn- api-call
  ([method-fn conn path] (api-call method-fn conn path nil))
  ([method-fn conn path body]
   (go
     (let [response (<! (-> {:headers {}}
                            (cond-> (some? body)
                                    (assoc :json-params body))
                            (cond-> (logged-in? conn)
                                    (assoc-in [:headers "X-Authentication"] (select-keys conn [::nickname ::token])))
                            (->> (method-fn (str (::base conn) path)))))]
       (if (:success response)
         (get response :body {})
         (throw (js/Error. "Wrong / Missing credentials!")))))))

(def api-get (partial api-call http/get))
(def api-post (partial api-call http/post))

(defn login "Login the user with his credentials."
  [conn username password]
  (go
    (let [body (<! (api-post conn "/login" {:nickname username
                                            :password password}))]
      (cond-> conn
        (:token body) (assoc ::id (:id body)
                             ::nickname (:nickname body)
                             ::token (:token body))))))

(defn logout [conn]
  #_(api-post conn "/logout")                               ; this is broken in D-BAS
  (go (dissoc conn ::id ::token ::nickname)))

(defn issues [conn]
  (api-get conn "/issues"))

(defn positions [conn slug]
  (api-get conn (gstring/format "/%s" slug)))