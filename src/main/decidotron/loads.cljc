(ns decidotron.loads
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.data-fetch :as df]))

(defsc Issue [_ _]
  {:query [:dbas.issue/slug
           :dbas.issue/title
           :dbas.issue/summary
           :dbas.issue/description
           :dbas.issue/date
           :dbas.issue/url
           :dbas.issue/language]
   :ident [:dbas.issue/by-slug :dbas.issue/slug]})

(defn load-issues [component connection where]
  (df/load component :dbas/issues Issue
    {:remote :dbas
     :params {:connection connection}
     :parallel true
     :target where}))