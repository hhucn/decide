(ns decidotron.utils
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