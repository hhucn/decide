(ns dbas.client
  (:require
    [cljs-http.client :as http]
    [cljs.core.async :refer [go <!]]
    [cljs.spec.alpha :as s]
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
   {:pre [(some? conn)]}
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

(s/def ::slug (s/and string? #(re-matches #"^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$" %)))
(s/def ::date string?) ; for now
(s/def ::description string?)
(s/def ::title string?)
(s/def ::url string?)
(s/def ::summary string?)
(s/def ::language (s/conformer keyword str))
(s/def ::issue (s/keys :req-un [::slug ::date ::title ::description ::url ::summary ::language]))

(defn issues [conn]
  (go (->> (<! (api-get conn "/issues"))
           (map #(s/conform ::issue %))
           (remove #{:cljs.spec.alpha/invalid}))))

(defn positions [conn slug]
  (api-get conn (gstring/format "/%s" slug)))