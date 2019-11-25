(ns decide.model.process
  (:require
    [datahike.api :as d]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [decide.util :as util]))


(defresolver resolve-process [{:keys [db]} {:process/keys [slug]}]
  {::pc/input #{:process/slug}
   ::pc/output [{:process/proposals [:proposal/id]}]}
  {:process/proposals
   (vec (for [{:argument/keys [id]} (:process/proposals (d/pull db [{:process/proposals [:argument/id]}] [:process/slug slug]))]
          {:proposal/id (util/str->uuid id)}))})

(defresolver all-processes [{:keys [db]} _]
  {::pc/output [{:all-processes [:process/slug]}]}
  {:all-processes
   (vec (for [slug (d/q '[:find [?slug ...]
                          :where [_ :process/slug ?slug]] db)]
          {:process/slug slug}))})

(def resolvers [resolve-process all-processes])