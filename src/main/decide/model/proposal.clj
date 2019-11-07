(ns decide.model.proposal
  (:require
    [datahike.api :as d]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s])
  (:import (java.util UUID)))


(defresolver resolve-proposal [{:keys [db]} {:keys [proposal/id]}]
  {::pc/input  #{:proposal/id}
   ::pc/output [:proposal/id :proposal/subtext :proposal/cost]}
  (-> db
    (d/pull [:argument/id
             :proposal/subtext
             :proposal/cost]
      [:argument/id (str id)])
    (update :argument/id #(UUID/fromString %))))


(def resolvers [resolve-proposal])