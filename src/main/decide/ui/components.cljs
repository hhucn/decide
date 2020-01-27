(ns decide.ui.components
  (:require
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    ["bootstrap/js/dist/collapse"]
    ["jquery" :as $]
    [goog.object :as gobj]
    [taoensso.timbre :as log]))

(defn toggle-collapse [ref show?]
  (.collapse ref
    (if show? "show" "hide")))

(defsc Collapse [this _props]
  {:query              [:ui/open?]
   :initial-state      {:ui/open? false}
   :initLocalState     (fn [this _props]
                         {:refn (fn [r]
                                  (gobj/set this "collapse" ($ r)))})
   :componentDidMount  (fn [this]
                         (some-> (comp/isoget this "collapse") (toggle-collapse (:ui/open? (comp/props this)))))
   :componentDidUpdate (fn [this _ _ _]
                         (some-> (comp/isoget this "collapse") (toggle-collapse (:ui/open? (comp/props this)))))}
  (dom/div :.collapse {:ref (comp/get-state this :refn)}
    (comp/children this)))

(def collapse (comp/factory Collapse))