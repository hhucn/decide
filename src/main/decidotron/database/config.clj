(ns decidotron.database.config
  (:require [toucan.db :as db]
            [toucan.models :as models]))

(db/set-default-db-connection!
  {:classname   "org.postgresql.Driver"
   :subprotocol "postgresql"
   :subname     "//localhost:5432/postgres"
   :user        "postgres"
   :password    "password123"})

(db/set-default-automatically-convert-dashes-and-underscores! true)

(models/set-root-namespace! 'decidotron.database.config)