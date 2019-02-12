(ns decidotron.ui.models
  (:require [fulcro.client.primitives :as prim :refer [defsc]]))

(defsc Position [this {:keys [id text cost]}]
  {:query [:id :text :cost]
   :ident [:position/by-id :id]})

(defsc Preference [this {:keys [position preferred-level]}]
  {:query [:preferred-level :position]
   :ident [:preference/by-level :preferred-level]})