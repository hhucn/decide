(ns decidotron.ui.models
  (:require [fulcro.client.primitives :as prim :refer [defsc]]
            [dbas.client :as client]))

(defsc Statement [_ _]
  {:query [:dbas.statement/id :dbas.statement/text :dbas.statement/is-supportive :dbas.statement/argument-id]
   :ident [:dbas.statement/id :dbas.statement/id]})

(defsc Position [_ _]
  {:query [:dbas.position/text
           :dbas.position/id
           :dbas.position/cost
           {:dbas.position/pros (prim/get-query Statement)}
           {:dbas.position/cons (prim/get-query Statement)}]
   :ident [:dbas.position/id :dbas.position/id]})

(defsc Issue [_ _]
  {:query [:dbas.issue/id
           :dbas.issue/slug
           :dbas.issue/budget
           :dbas.issue/title
           :dbas.issue/info
           :dbas.issue/long-info
           :dbas.issue/votes-end
           :dbas.issue/currency-symbol
           {:dbas.issue/positions (prim/get-query Position)}]
   :ident [:dbas.issue/slug :dbas.issue/slug]})

(defsc Connection [_ _]
  {:query [::client/base ::client/nickname ::client/id ::client/token ::client/login-status]})