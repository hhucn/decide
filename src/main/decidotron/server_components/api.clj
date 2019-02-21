(ns decidotron.server-components.api
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [decidotron.server-components.token :as t]
            [decidotron.database.models :as db :refer [positions-for-issue index-by]]))



(defonce state (atom {}))
(reset! state {4
               {"was-sollen-wir-mit-20-000eur-anfangen"
                {:preferences [{:dbas.position/id 83}]}}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(pc/defmutation update-preferences [_ {:keys [preference-list dbas.client/token]}]
  {::pc/sym 'update-preferences
   ::pc/params [:preference-list :token]}
  (let [user-id (:id (t/unsign token))]
    (get (swap! state assoc-in [user-id (:preference-list/slug preference-list)] preference-list) user-id)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(pc/defresolver position [_ input]
  {::pc/input  #{:dbas.position/id}
   ::pc/output [:dbas.position/id :dbas.position/text :dbas.position/cost]
   ::pc/batch? true}
  (if (sequential? input)
    (pc/batch-restore-sort {::pc/inputs input
                            ::pc/key    :dbas.position/id}
      (db/positions-by-ids (map :dbas.position/id input)))
    (db/position-by-id (:dbas.position/id input))))

(pc/defresolver issue [_ {slug :dbas.issue/slug}]
  {::pc/input  #{:dbas.issue/slug}
   ::pc/output [:dbas.issue/slug
                {:dbas.issue/positions [:dbas.position/id :dbas.position/text :dbas.position/cost]}]}
  #:dbas.issue{:slug      slug
               :positions (db/positions-for-issue slug)})

(pc/defresolver statements [_ {id :dbas.position/id}]
  {::pc/input  #{:dbas.position/id}
   ::pc/output [{:dbas/statements [:dbas.statement/id
                                   :dbas.statement/text
                                   :dbas.statement/is-supportive]}]}
  {:dbas/statements
   (for [{:keys [uid text is_supportive]} (db/pro-con-for-position id)]
     #:dbas.statement{:id            uid
                      :text          text
                      :is-supportive is_supportive})})

(pc/defresolver preferences [{user-id :dbas.client/id} {slug :preference-list/slug}]
  {::pc/input  #{:preference-list/slug}
   ::pc/output [:preference-list/slug
                {:dbas/issue [:dbas.issue/slug]}
                {:preferences [:dbas.position/id]}]}
  (merge {:dbas/issue {:dbas.issue/slug slug}}
    (get-in @state [user-id slug])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def app-registry [position issue preferences statements update-preferences]) ; DON'T FORGET TO ADD EVERYTHING HERE!
(def index (atom {}))

(def token-param-plugin
  {::p/wrap-read
   (fn [reader]
     (fn [env]
       (let [token (get-in env [:ast :params :dbas.client/token])]
         (reader (cond-> env token (assoc :dbas.client/id (:id (t/unsign token))))))))})

(def server-parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register app-registry
                                      ::pc/indexes  index})
                  p/error-handler-plugin
                  p/request-cache-plugin
                  p/trace-plugin
                  token-param-plugin]}))

