(ns decide.model.argument-test
  (:require
    [decide.server-components.pathom :refer [build-parser]]
    [decide.util :refer [uuid]]
    [clojure.test :refer [deftest is]]
    [fulcro-spec.core :refer [specification provided behavior assertions component provided! =>]]
    [decide.server-components.database :as db]
    [datahike.api :as d]))

(defn seeded-setup []
  (let [conn (db/new-database "datahike:mem://test-db")]
    (d/transact conn [#:account{:id "42"}
                      #:argument{:id   "eeeeeeee-c47b-46df-b74d-161a04e65b7e"
                                 :text "Example"}])
    {:conn conn
     :db   @conn}))

(deftest parser-integration-test
  (component "The pathom parser for the server"
    (let [{:keys [conn]} (seeded-setup)
          parser (build-parser conn)]
      (assertions
        "can mutate in a new argument and query for it afterwards"
        (parser {:AUTH/account-id "42"}
          [{'(decide.model.argument/new-argument
               {:argument/id      #uuid "c01f9b68-c47b-46df-b74d-161a04e65b7e"
                :argument/text    "This is fine"
                :argument/type    :pro
                :argument/subtype :support
                :argument/author  [:account/id "42"]
                :argument/parent  [:argument/id #uuid "eeeeeeee-c47b-46df-b74d-161a04e65b7e"]})
            [:argument/text {:argument/author [:account/id]}]}])
        => {'decide.model.argument/new-argument
            #:argument{:text   "This is fine"
                       :author #:account{:id "42"}}})

      (assertions
        "can query for arguments"
        (parser {}
          [{[:argument/id #uuid "eeeeeeee-c47b-46df-b74d-161a04e65b7e"]
            [:argument/id :argument/text]}])
        =>
        {[:argument/id #uuid "eeeeeeee-c47b-46df-b74d-161a04e65b7e"]
         {:argument/id   #uuid "eeeeeeee-c47b-46df-b74d-161a04e65b7e"
          :argument/text "Example"}}))))

