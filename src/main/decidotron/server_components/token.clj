(ns decidotron.server-components.token
  (:require [clj-http.client :as http]
            [buddy.sign.jwt :as jwt]
            [buddy.core.keys :refer [str->public-key]]
            [mount.core :refer [defstate]]
            [decidotron.server-components.config :refer [config]]))

(defstate pubkey
  :start (or (str->public-key (get-in config [:dbas :pubkey]))
           (slurp (str (get-in config [:dbas :api]) "/pubkey"))))

(defn unsign [token]
  (jwt/unsign token pubkey {:alg :es256}))

(defn refresh [token]
  (let [api       (get-in config [:dbas :api])
        api-token (get-in config [:dbas :api-token])]
    (:body (http/post (str api "/refresh-token")
             {:body         (format "{\"token\": \"%s\"}" token)
              :headers      {"X-Authentication" (format "{\"nickname\": \"Björn\", \"token\":\"%s\"}" api-token)}
              :content-type :json}))))