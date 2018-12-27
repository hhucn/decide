(ns decidotron.loads
  (:require [fulcro.client.data-fetch :as df]))

(defn load-issues [component connection]
  (df/load component :dbas/issues nil
    {:remote :dbas
     :params {:connection connection}
     :parallel true}))