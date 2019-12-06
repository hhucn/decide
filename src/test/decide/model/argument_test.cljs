(ns decide.model.argument-test
  (:require
    [decide.model.argument :as argument]
    [clojure.test :refer [deftest]]
    [fulcro-spec.core :refer [specification provided behavior assertions component =>]]))

(defn argumentation [current upstream]
  {:argumentation/upstream upstream
   :>/current-argument     current})


(specification "navigation-argumentation-stack"
  (let [empty-upstream (argumentation [:argument/id 1] [])]
    (behavior "should work for jumping backwards"
      (assertions
        "when the upstream is empty"
        (argument/*jump-backwards empty-upstream 0) => empty-upstream
        "when trying to jump out of the stack"
        (argument/*jump-backwards empty-upstream 1) => empty-upstream

        "when jumping to the root argument"
        (argument/*jump-backwards (argumentation [:argument/id 2] [[:argument/id 1]]) 0) => empty-upstream

        "when jumping back to a specific position below the root"
        (argument/*jump-backwards (argumentation [:argument/id 3] [[:argument/id 1] [:argument/id 2]]) 1) => (argumentation [:argument/id 2] [[:argument/id 1]])))
    (behavior "should work for navigating forwards"
      (assertions
        "When adding a new argument to an empty upstream"
        (argument/*navigate-forward empty-upstream [:argument/id 2]) => (argumentation [:argument/id 2] [[:argument/id 1]])))))