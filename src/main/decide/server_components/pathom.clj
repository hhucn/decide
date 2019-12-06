(ns decide.server-components.pathom
  (:require
    [mount.core :refer [defstate]]
    [taoensso.timbre :as log]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.common.async-clj :refer [let-chan]]
    [clojure.core.async :as async]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [decide.model.account :as acct]
    [decide.model.session :as session]
    [decide.model.argument :as arg]
    [decide.model.proposal :as proposal]
    [decide.model.process :as process]
    [decide.server-components.config :refer [config]]
    [decide.server-components.database :as db]
    [clojure.spec.alpha :as s]
    [datahike.api :as d]
    [datahike.core :refer [conn? db?]]))

(pc/defresolver index-explorer [env _]
  {::pc/input  #{:com.wsscode.pathom.viz.index-explorer/id}
   ::pc/output [:com.wsscode.pathom.viz.index-explorer/index]}
  {:com.wsscode.pathom.viz.index-explorer/index
   (-> (get env ::pc/indexes)
     ; this is necessary for now, because the index contains functions which can not be serialized by transit.
     (update ::pc/index-resolvers #(into {} (map (fn [[k v]] [k (dissoc v ::pc/resolve)])) %))
     (update ::pc/index-mutations #(into {} (map (fn [[k v]] [k (dissoc v ::pc/mutate)])) %))
     ; to minimize clutter in the Index Explorer
     #_(update ::pc/index-resolvers (fn [rs] (apply dissoc rs (filter #(clojure.string/starts-with? (namespace %) "com.wsscode.pathom") (keys rs)))))
     #_(update ::pc/index-mutations (fn [rs] (apply dissoc rs (filter #(clojure.string/starts-with? (namespace %) "com.wsscode.pathom") (keys rs))))))})

(def all-resolvers [acct/resolvers session/resolvers index-explorer arg/resolvers proposal/resolvers
                    process/resolvers
                    (pc/alias-resolver :proposal/id :argument/id)])

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

(def spec-plugin
  {::p/wrap-mutate
   (fn [mutate]
     (fn [env sym params]
       (if-let [spec (get-in env [::pc/indexes ::pc/index-mutations sym ::s/params])]
         (if (s/valid? spec params)
           (mutate env sym params)
           (throw (ex-info "Failed validation!" (s/explain-data spec params))))
         (mutate env sym params))))})


(defn log-requests [{:keys [env tx] :as req}]
  (log/debug "Pathom transaction:" (pr-str tx))
  req)

(>defn build-parser [db-connection]
  [conn? => any?]
  (let [real-parser (p/parallel-parser
                      {::p/mutate  pc/mutate-async
                       ::p/env     {::p/reader               [p/map-reader pc/parallel-reader
                                                              pc/open-ident-reader p/env-placeholder-reader]
                                    ::p/placeholder-prefixes #{">"}}
                       ::p/plugins [(pc/connect-plugin {::pc/register all-resolvers})
                                    (p/env-wrap-plugin (fn [env]
                                                         ;; Here is where you can dynamically add things to the resolver/mutation
                                                         ;; environment, like the server config, database connections, etc.
                                                         (let [{req-id :account/id valid? :session/valid?} (get-in env [:ring/request :session])]
                                                           (merge
                                                             {:AUTH/account-id (when valid? req-id)
                                                              :db              (d/db db-connection)
                                                              :connection      db-connection
                                                              :config          config}
                                                             env))))
                                    spec-plugin
                                    (preprocess-parser-plugin log-requests)
                                    p/error-handler-plugin
                                    (p/post-process-parser-plugin p/elide-not-found)
                                    p/trace-plugin]})
        ;; NOTE: Add -Dtrace to the server JVM to enable Fulcro Inspect query performance traces to the network tab.
        ;; Understand that this makes the network responses much larger and should not be used in production.
        trace?      (not (nil? (System/getProperty "trace")))]
    (fn wrapped-parser [env tx]
      (async/<!! (real-parser env (if trace?
                                    (conj tx :com.wsscode.pathom/trace)
                                    tx))))))

(defstate parser
  :start (build-parser db/conn))

