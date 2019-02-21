(ns decidotron.ui.models
  (:require [fulcro.client.primitives :as prim :refer [defsc]]))

(defsc Statement [_ _]
  {:query [:dbas.statement/id :dbas.statement/text :dbas.statement/is-supportive]
   :ident [:dbas.statement/id :dbas.statement/id]})

(defsc Position [_ _]
  {:query [:dbas.position/text
           :dbas.position/id
           :dbas.position/cost
           {:dbas/statements (prim/get-query Statement)}]
   :ident [:dbas.position/id :dbas.position/id]})

(defsc Issue [_ _]
  {:query [:dbas.issue/slug
           {:dbas.issue/positions (prim/get-query Position)}]
   :ident [:dbas.issue/slug :dbas.issue/slug]})