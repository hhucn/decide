(ns decidotron.material-ws
  (:require [nubank.workspaces.core :as ws]
            [nubank.workspaces.card-types.react :as ct.react]
            [decidotron.ui.mdc-components :as material]))

(ws/defcard text-field-card--empty
  (ct.react/react-card
    (material/text-field #js {:label "Nickname"}
      (material/input #js {:type "text"
                           :value ""}))))

(ws/defcard text-field-card--filled
  (ct.react/react-card
    (material/text-field #js {:label "Nickname"}
      (material/input #js {:type "text"
                           :value "Björn"}))))