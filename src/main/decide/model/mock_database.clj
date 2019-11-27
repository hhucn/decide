(ns decide.model.mock-database
  "This is a mock database implemented via Datascript, which runs completely in memory, has few deps, and requires
  less setup than Datomic itself.  Its API is very close to Datomics, and for a demo app makes it possible to have the
  *look* of a real back-end without having quite the amount of setup to understand for a beginner."
  (:require
    ;[datascript.core :as d]
    [datahike.core :as d]
    [datahike.api :as datahike]
    [mount.core :refer [defstate]]))

;; In datascript just about the only thing that needs schema
;; is lookup refs and entity refs.  You can just wing it on
;; everything else.
(def schema {:account/id {:db/cardinality :db.cardinality/one
                          :db/unique      :db.unique/identity}
             :argument/id {:db/cardinality :db.cardinality/one
                           :db/unique      :db.unique/identity
                           ;:db/type        :db.type/uuid
                           :db/type        :db.type/string}
             :argument/pros {:db/cardinality :db.cardinality/many
                             :db/type        :db.type/ref}
             :argument/cons {:db/cardinality :db.cardinality/many
                             :db/type        :db.type/ref}})

(defn new-database [] (d/create-conn schema))

(defn initialize-example! [conn]
  (d/transact! conn [#:argument{:db/id   "my-new-argument"
                                :id      (str (java.util.UUID/nameUUIDFromBytes (.getBytes "example-con")))
                                :type    :con
                                :subtype :undermine
                                :text    "Jeder kann sein eigenes Wasser mitbringen"}
                     #:argument{:id            (str (java.util.UUID/nameUUIDFromBytes (.getBytes "example-position")))
                                :text          "Sollten wir einen Wasserspender kaufen?"
                                :type          :position
                                :proposal/details
                                               "Hier steht eine genaue Beschreibung des Vorschlags. Mit seinen Einschränkungen und Bedingungen. \n\nVielleicht auch Anmerkungen von der Moderation. \nVielleicht zusammen, vielleicht alleine stehend.\n\nLorem ipsum dolor sit amet und soweiter und mehr Text, denn man gar nicht lesen braucht, weil er nur den Platz füllen soll. Jetzt solltest du aufhören zu lesen!"
                                :proposal/cost 5000
                                :argument/pros []
                                :argument/cons ["my-new-argument"]}])
  conn)

(defstate conn
  :start {}
  #_(-> (new-database) initialize-example!))
