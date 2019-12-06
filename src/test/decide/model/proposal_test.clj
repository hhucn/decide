(ns decide.model.proposal-test
  (:require
    [decide.server-components.pathom :refer [build-parser]]
    [decide.model.proposal :as proposal]
    [decide.util :refer [uuid]]
    [clojure.test :refer [deftest is]]
    [fulcro-spec.core :refer [specification provided behavior assertions component provided! =>]]
    [decide.server-components.database :as db]
    [datahike.api :as d]
    [taoensso.timbre :as log]))

(defn seeded-setup []
  (let [conn (db/new-database "datahike:mem://test-db")]
    (d/transact conn [#:account{:id "2"}
                      #:process{:slug      "example"
                                :budget    100000
                                :proposals ["proposal-1"]}])
    {:conn conn
     :db   @conn}))

(deftest parser-integration-test
  (component "The pathom parser for the server"
    (let [{:keys [conn]} (seeded-setup)
          parser (build-parser conn)]
      (assertions
        "Can add a new proposal with an account"
        (parser {:AUTH/account-id "2"}
          [{'(decide.model.proposal/new-proposal
               {:proposal/id      "c01f9b68-c47b-46df-b74d-161a04e65b7e"
                :proposal/cost    "1234"
                :proposal/details "Ein Wasserspender sorgt dafür, dass alle Studenten und Mitarbeiter mehr trinken. Dies sorgt für ein gesünderes Leben."
                :argument/text    "Es sollte ein Wasserspender im Flur aufgestellt werden."})
            [:proposal/cost :proposal/details :argument/text {:argument/author [:account/id]}]}])
        => {'decide.model.proposal/new-proposal
            {:proposal/cost    1234
             :proposal/details "Ein Wasserspender sorgt dafür, dass alle Studenten und Mitarbeiter mehr trinken. Dies sorgt für ein gesünderes Leben."
             :argument/text    "Es sollte ein Wasserspender im Flur aufgestellt werden."
             :argument/author  {:account/id "2"}}}

        ))))