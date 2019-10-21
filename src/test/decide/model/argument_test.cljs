(ns decide.model.argument-test
  (:require
    [decide.model.argument :as argument]
    [clojure.test :refer [deftest]]
    [fulcro-spec.core :refer [specification provided behavior assertions]]))

(def =>)

(defn argumentation [current upstream]
  #:argumentation{:upstream         upstream
                  :current-argument current})


(specification "Navigation of the argumentation stack"
  (let [empty-upstream (argumentation [:argument/id 1] [])]
    (assertions
      "should work for jumping backwards"
      (argument/*jump-backwards empty-upstream 0) => empty-upstream
      (argument/*jump-backwards empty-upstream 1) => empty-upstream

      (argument/*jump-backwards (argumentation [:argument/id 2] [[:argument/id 1]]) 0) => empty-upstream
      (argument/*jump-backwards (argumentation [:argument/id 3] [[:argument/id 1] [:argument/id 2]]) 1) => (argumentation [:argument/id 2] [[:argument/id 1]]))
    (assertions
      "should work for navigating forwards"
      (argument/*navigate-forward empty-upstream [:argument/id 2]) => (argumentation [:argument/id 2] [[:argument/id 1]]))))