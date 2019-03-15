(ns decidotron.api
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [decidotron.server-components.token :as t]
            [decidotron.database.models :as db :refer [positions-for-issue index-by]]
            [decidotron.server-components.budgets :as b]))

(defonce state (atom {}))
(reset! state {4
               {"was-sollen-wir-mit-20-000eur-anfangen"
                {:preferences [{:dbas.position/id 83}]}}})
@state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ident->map [[a b]] {a b})

(pc/defmutation update-preferences [_ {:keys [preference-list dbas.client/token]}]
  {::pc/params [:preference-list :token]}
  (let [user-id         (:id (t/unsign token))
        slug            (:preference-list/slug preference-list)
        with-map-idents (update preference-list :preferences (partial map ident->map))] ; idents to maps
    (when (db/allow-voting? slug)
      (get (swap! state assoc-in [user-id slug] with-map-idents) user-id))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(pc/defresolver position [_ input]
  {::pc/input  #{:dbas.position/id}
   ::pc/output [:dbas.position/id :dbas.position/text :dbas.position/cost :dbas.position/disabled?]
   ::pc/batch? true}
  (if (sequential? input)
    (pc/batch-restore-sort {::pc/inputs input
                            ::pc/key    :dbas.position/id}
      (db/positions-by-ids (map :dbas.position/id input)))
    (db/position-by-id (:dbas.position/id input))))

(pc/defresolver issue [_ {slug :dbas.issue/slug}]
  {::pc/input  #{:dbas.issue/slug}
   ::pc/output [:dbas.issue/id
                :dbas.issue/slug
                :dbas.issue/budget
                :dbas.issue/title
                :dbas.issue/info
                :dbas.issue/long-info
                :dbas.issue/votes-end
                :dbas.issue/votes-start
                :dbas.issue/currency-symbol
                {:dbas.issue/positions [:dbas.position/id :dbas.position/text :dbas.position/cost]}]}
  (db/get-issue slug))

(pc/defresolver statement [_ {id :dbas.statement/id}]
  {::pc/input  #{:dbas.statement/id}
   ::pc/output [:dbas.statement/id :dbas.statement/text :dbas.statement/is-supportive
                :dbas.statement/argument-id]}
  (for [{:keys [uid text is_supportive arg_uid]} (db/pro-con-for-position id)]
    #:dbas.statement{:id            uid
                     :text          text
                     :is-supportive is_supportive
                     :argument-id   arg_uid}))

(pc/defresolver position-pros-cons [_ {id :dbas.position/id}]
  {::pc/input  #{:dbas.position/id}
   ::pc/output [{:dbas.position/pros [:dbas.statement/id
                                      :dbas.statement/text
                                      :dbas.statement/is-supportive
                                      :dbas.statement/argument-id]}
                {:dbas.position/cons [:dbas.statement/id
                                      :dbas.statement/text
                                      :dbas.statement/is-supportive
                                      :dbas.statement/argument-id]}]}
  (let [{pros true
         cons false
         :or  {pros [] cons []}}
        (group-by :dbas.statement/is-supportive
          (for [{:keys [uid text is_supportive arg_uid]} (db/pro-con-for-position id)]
            #:dbas.statement{:id            uid
                             :text          text
                             :is-supportive is_supportive
                             :argument-id   arg_uid}))]
    {:dbas.position/pros pros
     :dbas.position/cons cons}))

(pc/defresolver preference-list [{user-id :dbas.client/id} {slug :preference-list/slug}]
  {::pc/input  #{:preference-list/slug}
   ::pc/output [:preference-list/slug
                {:dbas/issue [:dbas.issue/slug]}
                {:preferences [:dbas.position/id]}]}
  (let [preferences (get-in @state [user-id slug])]
    (merge {:dbas/issue {:dbas.issue/slug slug}}
      (update preferences :preferences db/filter-disabled-positions))))

(pc/defresolver preferences [_ {slug :preferences/slug}]
  {::pc/input  #{:preferences/slug}
   ::pc/output [{:preferences/list [:preference-list/slug]}
                {:dbas/issue [:dbas.issue/slug]}
                {:preferences/result-list [:result/slug]}]}
  {:preferences/list        {:preference-list/slug slug}
   :dbas/issue              {:dbas.issue/slug slug}
   :preferences/result-list {:result/slug slug}})

(pc/defresolver result [_ {slug :result/slug}]
  {::pc/input  #{:result/slug}
   ::pc/output [:result/show?
                {:result/positions [{:winners [:dbas.position/id :score]} ; make score publicly available?
                                    {:losers [:dbas.position/id :score]}]}]}
  (if (db/show-results? slug)
    (let [issue       (db/get-issue slug)
          budget      (:dbas.issue/budget issue)
          costs       (db/get-costs issue)
          preferences (->> @state vals (map #(->> (get-in % [slug :preferences])
                                               db/filter-disabled-positions
                                               (map :dbas.position/id))))
          {:keys [winners losers]} (b/borda-budget preferences budget costs)]
      {:result/show? true
       :result/positions
                     {:winners winners
                      :losers  losers}})
    {:result/show?     false
     :result/positions {:winners []
                        :losers  []}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; DON'T FORGET TO ADD EVERYTHING HERE!
(def app-registry [position issue preferences preference-list position-pros-cons update-preferences result])
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

