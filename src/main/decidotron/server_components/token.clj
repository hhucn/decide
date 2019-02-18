(ns decidotron.server-components.token
  (:require [clj-http.client :as http]
            [buddy.sign.jwt :as jwt]
            [buddy.core.keys :refer [str->public-key]]
            [decidotron.server-components.config :refer [config]]))

(defn unsign [token]
  (jwt/unsign token (str->public-key (get-in config [:dbas :pubkey])) {:alg :es256}))

(defn refresh [token]
  (let [dbas-base (get-in config [:dbas :base])
        api-token (get-in config [:dbas :api-token])]
    (:body (http/post (str dbas-base "/api/refresh-token")
             {:body         (format "{\"token\": \"%s\"}" token)
              :headers      {"X-Authentication" (format "{\"nickname\": \"Björn\", \"token\":\"%s\"}" api-token)}
              :content-type :json}))))