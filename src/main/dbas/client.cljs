(ns dbas.client
  (:require
    [cljs-http.client :as http]
    [cljs.core.async :refer [go <!]]
    [cljs.spec.alpha :as s]
    [goog.string :as gstring]))

(defn new-connection [] {::base         "https://dbas.cs.uni-duesseldorf.de/api"
                         ::login-status ::logged-out})
(def connection (new-connection))

; from https://stackoverflow.com/a/43722784/3616102
(defn- map->nsmap
  [n m]
  (reduce-kv (fn [acc k v]
               (let [new-kw (if (and (keyword? k)
                                  (not (qualified-keyword? k)))
                              (keyword (str n) (name k))
                              k)]
                 (assoc acc new-kw v)))
    {} m))

(defn logged-in? [conn]
  (and (contains? conn ::token)
    (contains? conn ::nickname)
    (= (conn ::login-status) ::logged-in)))

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
         (js/Error. "Wrong / Missing credentials!"))))))

(def api-get (partial api-call http/get))
(def api-post (partial api-call http/post))

(defn login "Login the user with his credentials."
  [conn username password]
  (go
    (let [body (<! (api-post conn "/login" {:nickname username
                                            :password password}))]
      (if (instance? js/Error body)
        (assoc conn ::login-status ::failed)
        (cond-> conn
          (:token body) (assoc ::id (:id body)
                               ::nickname (:nickname body)
                               ::token (:token body)
                               ::login-status ::logged-in))))))

(defn logout [conn]
  #_(api-post conn "/logout")                               ; this is broken in D-BAS
  (go (-> conn
        (dissoc ::id ::token ::nickname)
        (assoc ::login-status ::logged-out))))

(s/def :dbas.issue/slug (s/and string? #(re-matches #"^[A-Za-z0-9]+(?:-[A-Za-z0-9]+)*$" %)))
(s/def :dbas.issue/date string?)                            ; for now
(s/def :dbas.issue/description string?)
(s/def :dbas.issue/title string?)
(s/def :dbas.issue/url string?)
(s/def :dbas.issue/summary string?)
(s/def :dbas.issue/language (s/conformer keyword str))
(s/def :dbas/issue (s/keys :req [:dbas.issue/slug
                                 :dbas.issue/date
                                 :dbas.issue/title
                                 :dbas.issue/description
                                 :dbas.issue/url
                                 :dbas.issue/summary
                                 :dbas.issue/language]))
(s/def :dbas/issues (s/coll-of :dbas/issue :kind vector?))

(defn issues [conn]
  (go (->>
        (<! (api-get conn "/issues"))
        (map (partial map->nsmap "dbas.issue"))
        (map #(s/conform :dbas/issue %))
        (remove #{:cljs.spec.alpha/invalid})
        vec)))

(s/fdef issues
  :ret :dbas/issues)

(defn positions [conn slug]
  (api-get conn (gstring/format "/%s" slug)))

(defn attitude [conn slug position]
  (api-get conn (gstring/format "/%s/attitude/%s" slug (str position))))