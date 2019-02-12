(ns decidotron.database.config
  (:require [mount.core :refer [defstate]]
            [korma.db :as kdb]))

(kdb/defdb kdatabase
  (kdb/postgres {:db       "discussion"
                 :user     "postgres"
                 :password "DXxCNtfnt!MOo!f8LY1!P%sw3KGzt@s!"}))