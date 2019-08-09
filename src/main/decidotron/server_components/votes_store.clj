(ns decidotron.server-components.votes-store
  (:require [mount.core :refer [defstate]]
            [decidotron.server-components.config :refer [config]]
            [konserve.filestore :as kfs]
            [konserve.core :as k]
            [clojure.core.async :refer [<!!]]))

(defstate storage
  :start (<!! (kfs/new-fs-store (:storage-dir config))))

(defn get-votes [slug user-id]
  (<!! (k/get-in storage [slug user-id])))

(defn remove-votes [slug user-id]
  (second (<!! (k/update-in storage [slug] #(dissoc % user-id)))))

(defn remove-votes-from-all-issues [user-id]
  (Exception. "Not implemented yet"))

(defn all-votes [slug]
  (vals (<!! (k/get-in storage [slug]))))

(defn update-votes [slug user-id votes]
  (second (<!! (k/assoc-in storage [slug user-id] votes))))

(defn no-of-voters [slug]
  (count (<!! (k/get-in storage [slug]))))