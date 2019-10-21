(ns decide.model.argument
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro-css.css :as css]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
            [ghostwheel.core :as g :refer [>defn => | ?]]
            [clojure.spec.alpha :as s]))

(s/def :argument/id int?)
(s/def :argument/text string?)
(s/def :argument/type #{:pro :con})
(s/def :argument/pros (s/coll-of ::argument))
(s/def :argument/type (s/coll-of ::argument))
(s/def ::argument (s/keys :req-un [:argument/id]
                    :opt-un [:argument/text :argument/type
                             :argument/pros :argument/cons]))

(defsc Argument [this {:argument/keys [id text]} {:keys [argumentation-root]}]
  {:query [:argument/id :argument/text
           :argument/type ; pro, con, position, ...
           :argument/subtype ; undercut, undermine, ...
           {:argument/pros '...}
           {:argument/cons '...}]
   :ident :argument/id}
  (dom/button :.btn.btn-light
    {:style
     {:border "1px solid black"
      :borderRadius "10px"
      :padding "24px"}
     :onClick #(comp/transact! argumentation-root `[(navigate-forward {:argument/id ~id})])}
    text))

(def ui-argument (comp/factory Argument {:keyfn :argument/id}))

(defn half-row [& children]
  (div :.col-sm-6
    {:style {:display "flex"
             :flexDirection "column"}}
    children))

(defsc ProCon [this {:argument/keys [pros cons]} computed]
  {:query [:argument/id
           :argument/text
           :argument/type
           {:argument/pros (comp/get-query Argument)}
           {:argument/cons (comp/get-query Argument)}]
   :ident :argument/id}
  (div :.row
    (half-row
      (dom/h6 :.argumentation-header.bg-success "Pro Argumente")
      (map #(ui-argument (comp/computed % computed)) pros))

    (half-row
      (dom/h6 :.argumentation-header.bg-danger "Con Argumente")
      (map #(ui-argument (comp/computed % computed)) cons))))

(def ui-procon (comp/factory ProCon {:keyfn :argument/id}))

(defmutation navigate-forward [{id :argument/id}]
  (action [{:keys [component state] :as env}]
    (let [ident (comp/get-ident component)
          current (comp/get-ident Argument (:argumentation/current-argument (comp/props component)))]
      (swap! state
        #(-> %
           (assoc-in (conj ident :argumentation/current-argument) [:argument/id id])
           (update-in (conj ident :argumentation/upstream) conj current))))))

(defn *jump-backwards
  [{:argumentation/keys [upstream] :as argumentation}
   position]
  (if (contains? upstream position)
    (-> argumentation
      (assoc :argumentation/current-argument (nth upstream position))
      (update :argumentation/upstream subvec 0 position))
    argumentation))

(defmutation jump-backwards [{:keys [position]}]
  (action [{:keys [component state] :as env}]
    (swap! state update-in (comp/get-ident component) *jump-backwards position)))

(defsc UpstreamItem [_this {:argument/keys [text type]} {:keys [jmp-fn index]}]
  {:query [:argument/id :argument/text :argument/type]
   :ident :argument/id}
  (dom/button :.list-group-item.list-group-item-action
    {:key index
     :className (type {:pro "list-group-item-success"
                       :con "list-group-item-danger"
                       :position "list-group-item-info"})
     :style {:textAlign "center"
             :borderRadius "10px"}
     :onClick jmp-fn}
    text))

(def ui-upstream-item (comp/factory UpstreamItem))

(defsc Argumentation [this {:argumentation/keys [upstream current-argument]}]
  {:query [:argument/id
           {:argumentation/upstream (comp/get-query UpstreamItem)}
           {:argumentation/current-argument (comp/get-query ProCon)}]
   :ident :argument/id
   :initial-state {:argumentation/upstream []}}
  (div
    (dom/ol :.list-group
      (map-indexed
        (fn [i props]
          (ui-upstream-item
            (comp/computed props
              {:jmp-fn #(comp/transact! this [(jump-backwards {:position i})])})))
        (concat upstream [current-argument])))
    (ui-procon
      (comp/computed current-argument
        {:argumentation-root this}))))
