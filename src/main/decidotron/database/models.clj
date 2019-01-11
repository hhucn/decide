(ns decidotron.database.models
  [:require [toucan.models :refer [defmodel]]
            [toucan.db :as db]])

(defmodel User :users)
(defmodel Position :positions)
(defmodel Preference :preferences)


(comment
  (db/insert! User {:id 999 :nickname "MrOerni"})
  (db/delete! User {:id 999})
  (User))
