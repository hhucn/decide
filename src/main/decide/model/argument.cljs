(ns decide.model.argument
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro-css.css :as css]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [fulcro.client.mutations :as m]))

(defsc Argumentation [this props])

(defsc Argument [this {:argument/keys [id]}]
  {:query [:argument/id :argument/text]
   :ident :argument/id})
