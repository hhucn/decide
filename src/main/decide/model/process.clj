(ns decide.model.process
  (:require
    [datahike.api :as d]
    [datahike.core :refer [db? conn?]]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
    [taoensso.timbre :as log]
    [clojure.spec.alpha :as s])
  (:import (java.util Date)))

(s/def :phase/id any?)
(s/def :phase/starts inst?)
(s/def :phase/allowed set?)
(s/def :phase/config (s/keys :req [:phase/id :phase/starts :phase/allowed]))

(>defn now [] [=> inst?] (Date.))

(>defn before? [x y]
  [(? inst?) (? inst?) => boolean?]
  (if (and x y)
    (neg? (compare x y))
    false))

(>defn get-phase [datetime phase-config]
  [inst? (s/coll-of :phase/config) => (? :phase/config)]
  (let [groups (group-by #(-> % :phase/starts (before? datetime)) phase-config)]
    (-> groups (get true) last)))

(>defn get-phase-by-id [phases phase-id]
  [(s/coll-of :phase/config) :phase/id => (? :phase/config)]
  (first (filter (comp #{phase-id} :phase/id) phases)))


(defresolver resolve-phase [{:keys [config]} {:phase/keys [id]}]
  {::pc/input  #{:phase/id}
   ::pc/output [:phase/id :phase/starts :phase/allowed]}
  (get-phase-by-id (get-in config [:process :phases] []) id))


(defresolver resolve-current-phase [{:keys [config]} _]
  {::pc/output [{:current-phase [:phase/id :phase/starts :phase/allowed]}]}
  (let [phase (get-phase (now) (get-in config [:process :phases]))
        {:phase/keys [id starts allowed]
         :or         {allowed #{}}} phase]
    {:current-phase
     (when phase
       #:phase{:id      id
               :starts  starts
               :allowed (or allowed #{})})}))

(defresolver resolve-all-phases [{:keys [config]} _]
  {::pc/output [{:all-phases [:phase/id :phase/starts :phase/allowed]}]}
  {:all-phases (get-in config [:process :phases] [])})

(defresolver resolve-process [{:keys [config]} _]
  {::pc/output [:process/budget :process/currency]}
  (let [{:keys [budget]} (:process config)]
    #:process{:budget   budget
              :currency "€"}))



(def resolvers [resolve-process resolve-current-phase resolve-phase resolve-all-phases])