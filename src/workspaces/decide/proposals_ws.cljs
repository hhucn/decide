(ns decide.proposals-ws
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.model :as wsm]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [nubank.workspaces.card-types.react :as ct.react]
            [decide.model.proposal :as proposal]
            [decide.model.argument :as arg]
            [com.fulcrologic.fulcro.components :as comp]))

(ws/defcard proposal-card-card
  {::wsm/card-width 5 ::wsm/card-height 8}
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root proposal/ProposalCard
     ::ct.fulcro/initial-state
                      (fn [] #:proposal{:argument/id 1
                                        :argument/text "Es sollen Steckerleisten in den Seminarräumen ausliegen."
                                        :subtitle "In den Seminarräumen wird viel mit Laptops gearbeitet und der Zugang zu Steckdosen ist nur an den Rändern der Tische möglich."
                                        :price 500})}))

(ws/defcard proposal-details-card
  {::wsm/card-width 7 ::wsm/card-height 14}
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root proposal/ProposalDetails
     ::ct.fulcro/initial-state
                      (fn [] #:proposal{:argument/id   1
                                        :argument/text "Es soll ein 3D-Drucker aufgestellt werden. Und noch etwas text for good measure."
                                        :subtext       "Hier steht eine genaue Beschreibung des Vorschlags. Mit seinen Einschränkungen und Bedingungen. \n\nVielleicht auch Anmerkungen von der Moderation. \nVielleicht zusammen, vielleicht alleine stehend.\n\nLorem ipsum dolor sit amet und soweiter und mehr Text, denn man gar nicht lesen braucht, weil er nur den Platz füllen soll. Jetzt solltest du aufhören zu lesen!"
                                        :cost          500
                                        :argumentation #:argumentation{:argument/id      1
                                                                       :upstream         []
                                                                       :new-argument     (comp/initial-state arg/NewArgumentForm {:argumentation/id 1})
                                                                       :current-argument #:argument{:id   1
                                                                                                    :text "Es soll ein 3D Drucker angeschafft werden."
                                                                                                    :type :position
                                                                                                    :pros [#:argument{:id   42
                                                                                                                      :text "3d Drucker sind praktisch"
                                                                                                                      :pros [#:argument{:id 142 :text "Weil sie praktisch sind." :type :pro}]
                                                                                                                      :type :pro}
                                                                                                           #:argument{:id   421
                                                                                                                      :text "3d Drucker sind sehr praktisch"
                                                                                                                      :type :pro}]
                                                                                                    :cons [#:argument{:id   43
                                                                                                                      :text "3d Drucker sind teuer"
                                                                                                                      :type :con}]}}})}))

(ws/defcard big-price-tag
  {::wsm/card-width  2
   ::wsm/card-height 5}
  (ct.react/react-card
    (proposal/big-price-tag 1000 1000000 "$")))


