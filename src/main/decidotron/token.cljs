(ns decidotron.token
  (:require [goog.crypt.base64 :as b64]
            [clojure.string :as str]))

(defn- parse-json
  "Parse a JSON string to edn. Use only for small amounts of data"
  [json-str]
  (js->clj (js/JSON.parse json-str) :keywordize-keys true))

(defn payload-from-jwt
  [token]
  ;; Anatomy of a JWT: Header.Payload.Signature each segment json encoded in b64 on its own
  (-> token (str/split #"\.") second b64/decodeString parse-json))

(defn- ->connection
  "Receives a D-BAS JWT and returns in the map containing all login data needed for decidotron"
  [token]
  (let [{:keys [id nickname group]} (payload-from-jwt token)]
    {:dbas.client/base         (str js/dbas_host "/api")
     :dbas.client/id           id
     :dbas.client/admin?       (= "admins" group)
     :dbas.client/nickname     nickname
     :dbas.client/login-status :dbas.client/logged-in
     :dbas.client/token        token}))