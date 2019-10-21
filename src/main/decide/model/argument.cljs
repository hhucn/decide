(ns decide.model.argument
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro-css.css :as css]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))

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

(defn stack-entry [comp {:argument/keys [id text]}]
  (dom/li {:style {:textAlign "center"
                   :borderRadius "10px"
                   :border "2px solid grey"}}
    text))

(defmutation navigate-forward [{id :argument/id}]
  (action [{:keys [component state] :as env}]
    (let [ident (comp/get-ident component)
          current (comp/get-ident Argument (:argumentation/current-argument (comp/props component)))]
      (swap! state
        #(-> %
           (assoc-in (conj ident :argumentation/current-argument) [:argument/id id])
           (update-in (conj ident :argumentation/upstream) conj current))))))

(defmutation jump-backwards [{id :argument/id}]
  (action [{:keys [component state] :as env}]
    (let [ident (comp/get-ident component)
          current (comp/get-ident Argument (:argumentation/current-argument (comp/props component)))]
      (swap! state
        #(-> %
           (assoc-in (conj ident :argumentation/current-argument) [:argument/id id])
           (update-in (conj ident :argumentation/upstream) conj current))))))

(defsc Argumentation [this {:argumentation/keys [upstream current-argument]}]
  {:query [:argument/id
           {:argumentation/upstream (comp/get-query Argument)}
           {:argumentation/current-argument (comp/get-query ProCon)}]
   :ident :argument/id}
  (js/console.log (map :argument/id (cons current-argument upstream)))
  (div
    (dom/ol
      {:style {:listStyleType "none"
               :paddingInlineStart "0"}};}}
      (map #(stack-entry this %) (concat upstream [current-argument])))
    (ui-procon current-argument)
    (dom/button :.btn.btn-danger
      {:onClick #(comp/transact! this [(navigate-forward {:argument/id 42})])}
      "Nav Test")))



