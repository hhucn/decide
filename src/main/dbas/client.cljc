(ns dbas.client
  (:require
    [org.httpkit.client :as http]
    [clojure.data.json :as json]))

(defn new-connection [] {::base "https://dbas.cs.uni-duesseldorf.de/api"})
(def connection (new-connection))

(defn logged-in? [conn]
  (and (contains? conn ::token)
       (contains? conn ::nickname)))

(comment
  (logged-in? {::base "https://dbas.cs.uni-duesseldorf.de/api"})
  (logged-in? {::base "https://dbas.cs.uni-duesseldorf.de/api"
               ::nickname "Björn"
               ::token "abcTOKEN123"}))

(defn- api-call
  ([method-fn conn path] (api-call method-fn conn path nil))
  ([method-fn conn path body]
   (let [response @(-> {:content-type :json
                        :headers {}}
                       (cond-> (some? body)
                               (assoc :body (json/write-str body)))
                       (cond-> (logged-in? conn)
                               (assoc-in [:headers "X-Authentication"] (json/write-str (select-keys conn [::nickname ::token]))))
                       (->> (method-fn (str (::base conn) path))))]
     (println response)
     (if (< (:status response) 300)
       (json/read-str (get response :body "{}") :key-fn keyword)
       (throw (SecurityException. "Login required for this function"))))))

(def api-get (partial api-call http/get))
(def api-post (partial api-call http/post))

(defn login "Login the user with his credentials."
  [conn username password]
  (let [body (api-post conn "/login" {:nickname username
                                      :password password})]
    (cond-> conn
            (body :token) (assoc ::nickname (body :nickname)
                                 ::token (body :token)))))

(defn logout [conn]
  #_(api-post conn "/logout") ; this is broken in D-BAS
  (-> conn
      (dissoc ::token)
      (dissoc ::nickname)))

(defn issues [conn]
  (api-get conn "/issues"))

(defn positions [conn slug]
  (api-get conn (format "/%s" slug)))