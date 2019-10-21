(ns decide.argumentation-ws
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.fulcro3 :as ct.fulcro]
            [decide.model.argument :as arg]))

(ws/defcard pro-con-card
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root arg/ProCon
     ::ct.fulcro/initial-state
           (fn [] #:argument{:id 1
                             :text "Es soll ein 3D Drucker angeschafft werden."
                             :pros [#:argument{:id 42
                                               :text "3d Drucker sind praktisch"
                                               :type :pro}
                                    #:argument{:id 421
                                               :text "3d Drucker sind sehr praktisch"
                                               :type :pro}]
                             :cons [#:argument{:id 43
                                               :text "3d Drucker sind teuer"
                                               :type :con}]})}))

(ws/defcard argumentation-card
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root arg/Argumentation
     ::ct.fulcro/initial-state
           (fn [] #:argumentation{:argument/id 1
                                  :upstream []
                                  :current-argument #:argument{:id 1
                                                               :text "Es soll ein 3D Drucker angeschafft werden."
                                                               :type :position
                                                               :pros [#:argument{:id 42
                                                                                 :text "3d Drucker sind praktisch"
                                                                                 :pros [#:argument{:id 142 :text "Weil sie praktisch sind." :type :pro}]
                                                                                 :type :pro}
                                                                      #:argument{:id 421
                                                                                 :text "3d Drucker sind sehr praktisch"
                                                                                 :type :pro}]
                                                               :cons [#:argument{:id 43
                                                                                 :text "3d Drucker sind teuer"
                                                                                 :type :con}]}})}))

(ws/defcard Argument
  (ct.fulcro/fulcro-card
    {::ct.fulcro/root arg/Argument
     ::ct.fulcro/initial-state
           (fn [] #:argument{:id 42
                             :text "3d Drucker sind praktisch"})}))