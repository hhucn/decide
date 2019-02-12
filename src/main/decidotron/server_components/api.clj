(ns decidotron.server-components.api
  (:require [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [decidotron.server-components.token :as t]
            [decidotron.database.models :refer [positions-for-issue index-by]]))



(defonce state (atom {}))
(reset! state {4
               {"was-sollen-wir-mit-20-000eur-anfangen"
                {:dbas.issue/slug "was-sollen-wir-mit-20-000eur-anfangen"
                 :preferences [{:position [:position/by-id 87]}]}}})
@state
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(pc/defmutation update-preferences [e {:keys [preference-list token] :as ps}]
  {::pc/sym 'update-preferences
   ::pc/params [:preference-list :token]}
  (clojure.pprint/pprint (keys e))
  (clojure.pprint/pprint ps)
  (let [user-id (:id (t/unsign token))
        s (swap! state assoc-in [user-id (:dbas.issue/slug preference-list)] preference-list)]
    (clojure.pprint/pprint s)
    (s user-id)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(pc/defresolver position [{{{:keys [slug id]} :params} :ast} _]
  {::pc/output [:id :text :cost]}
  (get (index-by (positions-for-issue slug) :id) id))


(pc/defresolver positions [{{{:keys [dbas.issue/slug]} :params} :ast} _]
  {::pc/output [{:dbas.issue/positions [:id :text :cost]}]}
  {:dbas.issue/positions (positions-for-issue slug)})


(pc/defresolver preferences [{user-id :dbas.client/id} {slug :preference-list/by-slug}]
  {::pc/input #{:preference-list/by-slug}
   ::pc/output [:dbas.issue/slug {:preferences [{:position [:id :text :cost]}]}]}
  (let [positions (index-by (positions-for-issue slug) :id)
        preference-ids (map (comp second :position) (get-in @state [user-id slug :preferences] []))]
    {:dbas.issue/slug slug
     :preferences (map (comp (partial hash-map :position) positions) preference-ids)}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(def app-registry [position positions preferences update-preferences]) ; DON'T FORGET TO ADD EVERYTHING HERE!
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

