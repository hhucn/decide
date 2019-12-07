(ns decide.model.process
  (:require
    [datahike.api :as d]
    [datahike.core :refer [db? conn?]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s]
    [decide.util :as util]))

(defresolver resolve-process [{:keys [config]} _]
  {::pc/input  #{}
   ::pc/output [:process/budget]}
  (let [{:keys [budget]} (:process config)]
    #:process{:budget budget}))

(def resolvers [resolve-process])