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

(defsc Argument [this {:argument/keys [id text]}]
  {:query [:argument/id :argument/text
           :argument/type ; pro, con, position, ...
           :argument/subtype ; undercut, undermine, ...
           {:argument/pros '...}
           {:argument/cons '...}]
   :ident :argument/id}
  (div :.btn.btn-light
    {:style
     {:border "1px solid black"
      :borderRadius "10px"
      :padding "24px"}
     :onClick (fn [_e])}
    text))

(def ui-argument (comp/factory Argument {:keyfn :argument/id}))

(defsc ProCon [this {:argument/keys [pros cons]}]
  {:query [:argument/id
           :argument/text
           :argument/type
           {:argument/pros (comp/get-query Argument)}
           {:argument/cons (comp/get-query Argument)}]
   :ident :argument/id}
  (div :.row
    (div :.col-sm-6
      {:style {:display "flex"
               :flexDirection "column"}}
      (div {:style {:height "24px"
                    :backgroundColor "green"
                    :color "white"
                    :textAlign "center"}}
        "Pro Argumente")
      (map ui-argument pros))

    (div :.col-sm-6
      {:style {:display "flex"
               :flexDirection "column"}}
      (div {:style {:height "24px"
                    :backgroundColor "red"
                    :color "white"
                    :textAlign "center"}}
        "Con Argumente")
      (map ui-argument cons))))

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

(def border-colors {:pro "btn-outline-success"
                    :con "btn-outline-danger"
                    :position "btn-outline-dark"})

(defsc UpstreamItem [_this {:argument/keys [text type]} {:keys [parent index]}]
  {:query [:argument/id :argument/text :argument/type]
   :ident :argument/id}
  (dom/li :.btn
    {:key index
     :className (border-colors type)
     :style {:textAlign "center"
             :borderRadius "10px"}
     :onClick #(comp/transact! parent [(jump-backwards {:position index})])}
    text))

(def ui-upstream-item (comp/factory UpstreamItem))

(defsc Argumentation [this {:argumentation/keys [upstream current-argument]}]
  {:query [:argument/id
           {:argumentation/upstream (comp/get-query UpstreamItem)}
           {:argumentation/current-argument (comp/get-query ProCon)}]
   :ident :argument/id
   :initial-state {:argumentation/upstream []}}
  (js/console.log (map :argument/id (cons current-argument upstream)))
  (div
    (dom/ol :.list-group
      {:style {:display "flex"
               :flexDirection "column"}}
      (map-indexed
        (fn [i props] (ui-upstream-item (comp/computed props {:index i
                                                              :parent this})))
        (concat upstream [current-argument])))
    (ui-procon current-argument)
    (dom/button :.btn.btn-block.btn-danger
      {:onClick #(comp/transact! this [(navigate-forward {:argument/id 42})])}
      "Nav Test")))
