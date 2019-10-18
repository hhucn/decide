(ns decide.proposals-ws
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [decide.model.proposal :as proposal]))

(ws/defcard proposal-card-card
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root proposal/ProposalCard
     ::ct.fulcro/initial-state
                      (fn [] #:proposal{:id 1
                                        :title "Es sollen Steckerleisten in den Seminarräumen ausliegen."
                                        :subtitle "In den Seminarräumen wird viel mit Laptops gearbeitet und der Zugang zu Steckdosen ist nur an den Rändern der Tische möglich."
                                        :price 500})}))


