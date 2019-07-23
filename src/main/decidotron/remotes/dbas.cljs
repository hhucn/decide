(ns decidotron.remotes.dbas
  (:require [cljs.core.async :refer [go <!] :as async]
            [dbas.client :as dbas]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.fulcro.network :as pfn]
            [decidotron.token :as token-util]))

(pc/defresolver issues [{{{connection :connection} :params} :ast} _]
  {::pc/output [{:dbas/issues [:dbas.issue/date
                               :dbas.issue/description
                               :dbas.issue/language
                               :dbas.issue/slug
                               :dbas.issue/summary
                               :dbas.issue/title
                               :dbas.issue/url]}]}
  {:dbas/issues (go vec (<! (dbas/issues connection)))})

(pc/defresolver positions [{{{:keys [connection slug] } :params} :ast} _]
  {::pc/output [{:dbas.issue/positions [:dbas.position/url]}]}
  {:dbas.issue/positions (dbas/positions connection slug)})

(pc/defresolver attitudes [{{{:keys [connection slug position] } :params} :ast} _]
  {::pc/output [{:dbas.issue.position/attitudes [:bubbles :attitudes]}]}
  {:dbas.issue.position/attitudes (dbas/attitude connection slug position)})

(pc/defresolver login [{{{:keys [connection nickname password]} :params} :ast :as ast} _]
  {::pc/output [{:dbas/connection [::dbas/base ::dbas/nickname ::dbas/id ::dbas/token ::dbas/login-status
                                   ::dbas/admin?]}]}
  (go (let [con (<! (dbas/login connection nickname password))]
        (if (= ::dbas/logged-in (::dbas/login-status con))
          (let [payload (token-util/payload-from-jwt (::dbas/token con))]
            {:dbas/connection
             (-> con
               (assoc ::dbas/nickname (:nickname payload))
               (assoc ::dbas/id (:id payload))
               (assoc ::dbas/admin? (= "admins" (:group payload))))})
          con))))

(def app-registry [login issues positions attitudes]) ; DON'T FORGET TO ADD EVERYTHING HERE!
(def index (atom {}))

(def parser
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
                  p/trace-plugin]}))

(def dbas-remote (pfn/pathom-remote parser))