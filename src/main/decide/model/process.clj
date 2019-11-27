(ns decide.model.process
  (:require
    [datahike.api :as d]
    [datahike.core :refer [db? conn?]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [decide.util :as util]))

(s/def :process/slug string?)
(s/def ::process-ident (s/tuple #{:process/slug} :process/slug))

(>defn get-process [db slug subquery]
  [db? :process/slug vector? => any?]
  (d/pull db subquery [:process/slug slug]))

(defresolver resolve-process [{:keys [db]} {:process/keys [slug]}]
  {::pc/input  #{:process/slug}
   ::pc/output [:process/budget {:process/proposals [:proposal/id]}]}
  (-> (get-process db slug
        [:process/budget {:process/proposals [:argument/id]}])
    (update :process/proposals #(map (comp util/arg-id->prop-id util/str-id->uuid-id) %))))

(defresolver all-processes [{:keys [db]} _]
  {::pc/output [{:all-processes [:process/slug]}]}
  {:all-processes
   (vec (for [slug (d/q '[:find [?slug ...]
                          :where [_ :process/slug ?slug]] db)]
          {:process/slug slug}))})

(def resolvers [resolve-process all-processes])