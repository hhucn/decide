(ns decidotron.api
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [mount.core :refer [defstate]]
            [decidotron.server-components.token :as t]
            [decidotron.server-components.database :as db]
            [decidotron.server-components.budgets :as b]
            [decidotron.server-components.config :refer [config]]
            [konserve.filestore :as kfs]
            [konserve.core :as k]
            [clojure.core.async :refer [go <! <!!]]
            [fulcro.logging :as log]))

(defstate storage
  :start (<!! (kfs/new-fs-store (:storage-dir config))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- ident->map [[a b]] {a b})

(defn- validate-preference-list [slug {:keys [preferences]}]
  (distinct? (map :dbas.position/id (db/filter-disabled-positions slug preferences))))

(pc/defmutation update-preferences [_ {:keys [preference-list dbas.client/token]}]
  {::pc/params [:preference-list :token]}
  (if-let [user-id (:id (t/unsign token))]
    (let [slug            (:preference-list/slug preference-list)
          preference-list (update preference-list :preferences (partial map ident->map))] ; idents to maps
      (if (validate-preference-list slug preference-list)
        (when (db/allow-voting? slug)
          (-> (if (empty? (:preferences preference-list))
                (k/update-in storage [slug] #(dissoc % user-id))
                (k/assoc-in storage [slug user-id] preference-list))
            <! second go))
        :decidotron.error/duplicated-position))
    :decidotron.error/invalid-token))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(pc/defresolver position [_ input]
  {::pc/input  #{:dbas.position/id}
   ::pc/output [:dbas.position/id :dbas.position/text :dbas.position/cost :dbas.position/disabled?]}
  ;   ::pc/batch? true}
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
  (go
    (merge {:dbas/issue {:dbas.issue/slug slug}}
      (update (<! (k/get-in storage [slug user-id]))
        :preferences #(db/filter-disabled-positions slug %)))))

(pc/defresolver preferences [_ {slug :preferences/slug}]
  {::pc/input  #{:preferences/slug}
   ::pc/output [{:preferences/list [:preference-list/slug]}
                {:dbas/issue [:dbas.issue/slug]}
                {:preferences/result-list [:result/slug]}]}
  {:preferences/list        {:preference-list/slug slug}
   :dbas/issue              {:dbas.issue/slug slug}
   :preferences/result-list {:dbas.issue/slug slug}})

(defn- proposal->dbas [{:keys [proposal scores]}]
  #:dbas.position{:id     proposal
                  :scores scores})

(defn- votes [slug]
  (let [disabled-filter (db/filter-disabled-positions-fn slug)]
    (->> (<!! (k/get-in storage [slug]))
      vals
      (map #(->> % :preferences disabled-filter (map :dbas.position/id))))))

(pc/defresolver result [_ {slug :dbas.issue/slug}]
  {::pc/input  #{:dbas.issue/slug}
   ::pc/output [{:result/positions [{:winners [:dbas.position/id :dbas.position/scores]}
                                    {:losers [:dbas.position/id :dbas.position/scores]}]}]}
  (if (db/show-results? slug)
    (let [issue       (db/get-issue slug)
          {:keys [winners losers] :as result}
          (b/borda-budget
            (votes slug)
            (:dbas.issue/budget issue)
            (db/get-costs issue))]
      (log/trace "Result is:" result)
      {:result/positions {:winners         (map proposal->dbas winners)
                          :losers (map proposal->dbas losers)}})
    {:result/positions {:winners []
                        :losers  []}}))

(pc/defresolver show-result [_ {slug :dbas.issue/slug}]
  {::pc/input  #{:dbas.issue/slug}
   ::pc/output [:result/show?]}
  {:result/show? (db/show-results? slug)})

(pc/defresolver result-no-of-participants [_ {slug :dbas.issue/slug}]
  {::pc/input  #{:dbas.issue/slug}
   ::pc/output [:result/no-of-participants]}
  {:result/no-of-participants (count (<!! (k/get-in storage [slug])))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn preprocess-parser-plugin
  "Helper to create a plugin that can view/modify the env/tx of a top-level request.
  f - (fn [{:keys [env tx]}] {:env new-env :tx new-tx})
  If the function returns no env or tx, then the parser will not be called (aborts the parse)"
  [f]
  {::p/wrap-parser
   (fn transform-parser-out-plugin-external [parser]
     (fn transform-parser-out-plugin-internal [env tx]
       (let [{:keys [env tx] :as req} (f {:env env :tx tx})]
         (if (and (map? env) (seq tx))
           (parser env tx)
           {}))))})

(pc/defresolver index-explorer [env _]
  {::pc/input  #{:com.wsscode.pathom.viz.index-explorer/id}
   ::pc/output [:com.wsscode.pathom.viz.index-explorer/index]}
  {:com.wsscode.pathom.viz.index-explorer/index (get env :pc/indexes)})

; DON'T FORGET TO ADD EVERYTHING HERE!
(def app-registry [position issue preferences preference-list position-pros-cons update-preferences result show-result result-no-of-participants index-explorer])
(def index (atom {}))

(def token-param-plugin
  {::p/wrap-read
   (fn [reader]
     (fn [env]
       (let [token (get-in env [:ast :params :dbas.client/token])]
         (reader (cond-> env token (assoc :dbas.client/id (:id (t/unsign token))))))))})

(defn log-requests [{:keys [env tx] :as req}]
  (log/debug "Pathom transaction:" (pr-str tx))
  req)

(def server-parser
  (let [real-parser (p/parallel-parser
                      {::p/env     {::p/reader               [p/map-reader
                                                              pc/parallel-reader
                                                              pc/open-ident-reader
                                                              p/env-placeholder-reader]
                                    ::p/placeholder-prefixes #{">"}}
                       ::p/mutate  pc/mutate-async
                       ::p/plugins [(pc/connect-plugin {::pc/register app-registry})
                                    (p/env-wrap-plugin (fn [env]
                                                         ;; Here is where you can dynamically add things to the resolver/mutation
                                                         ;; environment, like the server config, database connections, etc.
                                                         (assoc env :config config)))
                                    (preprocess-parser-plugin log-requests)
                                    #_(p/post-process-parser-plugin p/elide-not-found)
                                    p/error-handler-plugin
                                    p/request-cache-plugin
                                    p/trace-plugin
                                    token-param-plugin]})]
    (fn wrapped-parser [env tx]
      (real-parser env (if (:trace? config) (conj tx :com.wsscode.pathom/trace) tx)))))

