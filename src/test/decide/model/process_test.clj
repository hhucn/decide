(ns decide.model.process-test
  (:require
    [clojure.test :refer [deftest is]]
    [fulcro-spec.core :refer [specification provided behavior assertions component provided! =>]]
    [decide.model.process :as process]))


(specification "The `before?` function"
  (behavior "returns true"
    (assertions
      "when the first parameter is before the second in time"
      (process/before? #inst "2020" #inst "3333") => true))
  (behavior "returns false"
    (assertions
      "when the first and second parameters are equal"
      (process/before? #inst "2020" #inst "2020") => false
      "when the second parameter is before the first in time"
      (process/before? #inst "3333" #inst "2020") => false)
    (behavior "when there is nil"
      (assertions
        "for the first parameter"
        (process/before? nil #inst "2020") => false
        "for the second parameter"
        (process/before? #inst "2020" nil) => false
        "for both parameters"
        (process/before? nil #inst "2020") => false))))

(specification "When getting the phase"
  (let [phase-config [#:phase{:id   :proposal
                              :starts  #inst "2020"
                              :allowed #{:proposing :moderation :argumentation}}
                      #:phase{:id   :moderation
                              :starts  #inst "2020-02-01"
                              :allowed #{:moderation :argumentation}}
                      #:phase{:id   :vote
                              :starts  #inst "2020-03-01"
                              :allowed #{:voting :argumentation}}
                      #:phase{:id   :over
                              :starts   #inst "2020-04-01"
                              :allowed #{:result}}]]
    (behavior "return nil"
      (assertions
        "when date is before every phase"
        (process/get-phase #inst "1999" phase-config) => nil

        "when there are no phases"
        (process/get-phase #inst "2020" {}) => nil))
    (assertions

      "return the correct one"
      (process/get-phase #inst "2020-02-15" phase-config) => #:phase{:id   :moderation
                                                                     :starts  #inst "2020-02-01"
                                                                     :allowed #{:moderation :argumentation}})))