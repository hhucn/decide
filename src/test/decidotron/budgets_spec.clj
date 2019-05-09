(ns decidotron.budgets-spec
  (:require [clojure.test :refer :all]
            [fulcro-spec.core :refer [specification provided behavior assertions]]
            [decidotron.server-components.budgets :as b]))

(specification "Testing borda score" :unit
  (assertions
    "with empty votes"
    (b/borda-score 5 []) => {}

    "with content"
    (b/borda-score 5 [:a :b :c]) => {:a 5 :b 4 :c 3}))

(specification "Test max-n" :unit
  (assertions
    "Max n of an empty vote set is 0"
    (b/find-max-n []) => 0))

(specification "Calculating the borda result" :unit
  (let [budget 10000
        costs  {:wasserspender 2000
                :bällebad      7000
                :schokobrunnen 4000}
        votes  [[:wasserspender :schokobrunnen]             ; Christian
                [:bällebad :schokobrunnen :wasserspender]   ; Alexander
                [:schokobrunnen :bällebad]]]                ; Markus
    (assertions
      "without a draw"
      (b/borda-budget votes budget costs)
      => {:winners [{:proposal :schokobrunnen
                     :scores   {:borda 7 :approval 3 :id :schokobrunnen}}
                    {:proposal :wasserspender
                     :scores   {:borda 4 :approval 2 :id :wasserspender}}]
          :losers  [{:proposal :bällebad
                     :scores   {:borda 5 :approval 2 :id :bällebad}}]}

      "On draw the proposal with the most approval wins"
      (b/borda-budget
        (conj votes [:bällebad :wasserspender :schokobrunnen]) ; Martin
        budget costs)
      => {:winners [{:proposal :schokobrunnen
                     :scores   {:borda 8 :approval 4 :id :schokobrunnen}}
                    {:proposal :wasserspender
                     :scores   {:borda 6 :approval 3 :id :wasserspender}}]
          :losers  [{:proposal :bällebad
                     :scores   {:borda 8 :approval 3 :id :bällebad}}]}

      "A proposal with no votes can not win"
      (b/borda-budget [[]] 1000 {:a 100})
      => {:winners []
          :losers  [{:proposal :a
                     :scores   {:borda 0 :approval 0 :id :a}}]})))
