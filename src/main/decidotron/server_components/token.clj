(ns decidotron.server-components.token
  (:require [clj-http.client :as http]
            [buddy.sign.jwt :as jwt]
            [buddy.core.keys :refer [str->public-key]]))

(def dbas-base "http://0.0.0.0:4284")
(def api-token "7de14:d0d329b91bc7f8dcb42889f7f1c879330a73781ae9fe11cd38669d2ec9076132")
(def pkey (str->public-key (slurp (str dbas-base "/api/pubkey"))))

(defn unsign [token]
  (jwt/unsign token pkey {:alg :es256}))

(defn refresh [token]
  (:body (http/post (str dbas-base "/api/refresh-token")
           {:body (format "{\"token\": \"%s\"}" token)
            :headers {"X-Authentication" (format "{\"nickname\": \"Björn\", \"token\":\"%s\"}" api-token)}
            :content-type :json})))