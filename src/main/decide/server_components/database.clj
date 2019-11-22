(ns decide.server-components.database
  (:require
    [decide.server-components.config :refer [config]]
    [datahike.api :as d]
    [datahike.core :as datahike]
    [datahike.db :as datahike-db]
    [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
    [mount.core :refer [defstate args]]
    [clojure.spec.alpha :as s]
    [taoensso.timbre :as log])
  (:import (java.util UUID)))

(def default-uri "datahike:mem://example")


(def argument-schema [{:db/ident       :argument/id
                       :db/cardinality :db.cardinality/one
                       :db/unique      :db.unique/identity
                       ;:db/type        :db.type/uuid
                       :db/valueType   :db.type/string}
                      {:db/ident       :argument/type
                       :db/cardinality :db.cardinality/one
                       :db/valueType   :db.type/keyword}
                      {:db/ident       :argument/subtype
                       :db/cardinality :db.cardinality/one
                       :db/valueType   :db.type/keyword}

                      {:db/ident       :argument/text
                       :db/cardinality :db.cardinality/one
                       :db/valueType   :db.type/string}

                      {:db/ident       :argument/pros
                       :db/cardinality :db.cardinality/many
                       :db/valueType   :db.type/ref}
                      {:db/ident       :argument/cons
                       :db/cardinality :db.cardinality/many
                       :db/valueType   :db.type/ref}])

(def proposal-schema [{:db/ident       :proposal/subtext
                       :db/cardinality :db.cardinality/one
                       :db/valueType   :db.type/string}
                      {:db/ident       :proposal/cost
                       :db/cardinality :db.cardinality/one
                       :db/valueType   :db.type/long}])

(def account-schema [#:db{:ident       :account/id
                          :valueType   :db.type/string
                          :cardinality :db.cardinality/one
                          :unique      :db.unique/identity}
                     #:db{:ident       :account/display-name
                          :valueType   :db.type/string
                          :cardinality :db.cardinality/one}
                     #:db{:ident       :account/firstname
                          :valueType   :db.type/string
                          :cardinality :db.cardinality/one}
                     #:db{:ident       :account/lastname
                          :valueType   :db.type/string
                          :cardinality :db.cardinality/one}
                     #:db{:ident       :account/mail
                          :valueType   :db.type/string
                          :cardinality :db.cardinality/one}
                     #:db{:ident       :account/active?
                          :valueType   :db.type/boolean
                          :cardinality :db.cardinality/one}])



(def schema (into [] cat [argument-schema
                          proposal-schema
                          account-schema]))

(>defn named-uuid [s]
  [string? => uuid?]
  (UUID/nameUUIDFromBytes (.getBytes s)))

(defn initialize-example! [conn]
  [datahike/conn? => datahike/conn?]
  (d/transact! conn [#:argument{:db/id   "my-new-argument"
                                :id      (str (named-uuid "example-pro"))
                                :type    :pro
                                :subtype :support
                                :text    "Die Umweltspur reduziert die Feinstaubbelastung."}
                     #:argument{:id            (str (named-uuid "example-position"))
                                :text          "Die Umweltspur in Düsseldorf sollte beibehalten werden"
                                :type          :position
                                :subtype       :position
                                :proposal/subtext
                                               "Hier steht eine genaue Beschreibung des Vorschlags. Mit seinen Einschränkungen und Bedingungen. \n\nVielleicht auch Anmerkungen von der Moderation. \nVielleicht zusammen, vielleicht alleine stehend.\n\nLorem ipsum dolor sit amet und soweiter und mehr Text, denn man gar nicht lesen braucht, weil er nur den Platz füllen soll. Jetzt solltest du aufhören zu lesen!"
                                :proposal/cost 10000
                                :argument/pros ["my-new-argument"]
                                :argument/cons []}])
  conn)

(defn new-database []
  (d/create-database default-uri
    :initial-tx schema
    :schema-on-read true)
  (d/connect default-uri))

(defstate conn
  :start
  (let [{{:db/keys [uri example? reset?]
          :or      {uri      default-uri
                    example? false
                    reset?   false}} :db} config
        _          (when reset?
                     (log/info "Reset database...")
                     (d/delete-database uri))
        db-exists? (d/database-exists? uri)]
    (log/info "Create database connection with URI:" uri)
    (log/info "Example requested?" example?)

    (when-not db-exists?
      (log/info "Database does not exist! Creating...")
      (d/create-database uri :initial-tx schema))

    (log/info "Database exists. Connecting...")
    (let [conn (d/connect uri)]
      (when example?
        (log/info "Example DB requested. Hydrating...")
        (initialize-example! conn))

      conn)))