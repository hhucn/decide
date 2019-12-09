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
                      #:argument{:id "42"}])
    {:conn conn
     :db   @conn}))

(deftest parser-integration-test
  (component "The pathom parser for the server"
    (let [{:keys [conn]} (seeded-setup)
          parser (build-parser conn)]
      (assertions
        "can add a new proposal with an account"
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

        "can add a vote to a proposal and pull the utility"
        (parser {:AUTH/account-id "2"}
          [{'(decide.model.proposal/set-vote
               {:proposal/id  "42"
                :account/id   "2"
                :vote/utility 1})
            [:proposal/id :vote/utility]}])
        => {'decide.model.proposal/set-vote
            {:proposal/id  "42"
             :vote/utility 1}}

        "can pull the utility afterwards"
        (parser {:AUTH/account-id "2"}
          [{[:proposal/id "42"]
            [:proposal/id :vote/utility]}])
        => {[:proposal/id "42"]
            {:proposal/id "42", :vote/utility 1}}

        "can't pull the utility without account"
        (get (parser {}
               [{[:proposal/id "42"]
                 [:proposal/id :vote/utility]}])
          [:proposal/id "42"])
        => {:proposal/id  "42",
            :vote/utility :com.wsscode.pathom.core/reader-error}))))


