(ns decidotron.remotes.dbas
  (:require [cljs.core.async :refer [go <!] :as async]
            [dbas.client :as dbas]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.fulcro.network :as pfn]))

(pc/defresolver issues [{{{connection :connection} :params} :ast} _]
  {::pc/output [{:dbas/issues [:dbas/date :dbas/description :dbas/language :dbas/slug :dbas/summary :dbas/title :dbas/url]}]}
  {:dbas/issues (go (into {} (map #(vector (:slug %) %)) (<! (dbas/issues connection))))})

(pc/defmutation login [_ {:keys [connection nickname password]}]
  {::pc/sym    'dbas/login
   ::pc/params [:connection :nickname :password]
   ::pc/output [::dbas/base ::dbas/nickname ::dbas/id ::dbas/token]}
  (dbas/login connection nickname password))

(def app-registry [login issues])
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