(ns decide.model.argument
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div button p a form option label input span]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro-css.css :as css]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
            [ghostwheel.core :as g :refer [>defn => | ?]]
            [clojure.spec.alpha :as s]
            [com.fulcrologic.fulcro.dom.events :as evt]))

(s/def :argument/id int?)
(s/def :argument/text string?)
(s/def :argument/type #{:pro :con})
(s/def :argument/pros (s/coll-of ::argument))
(s/def :argument/type (s/coll-of ::argument))
(s/def ::argument (s/keys :req-un [:argument/id]
                    :opt-un [:argument/text :argument/type
                             :argument/pros :argument/cons]))

(defn select [comp attribute options current-value]
  (dom/select :.form-control
    {:onChange #(m/set-value! comp attribute (keyword (evt/target-value %)))
     :value current-value}
    (for [[type label] options
          :let [value (name type)]]
      (option {:key type
               :value value}
        label))))

(defmutation add-argument [{:keys [argument subtype]}]
  (action [env] true))

(defsc NewArgumentForm [this {:keys [new-argument/id]
                              :ui/keys [open? new-argument new-subtype pro?]}]
  {:query [:new-argument/id
           :ui/open?
           :ui/new-argument :ui/new-subtype :ui/pro?
           fs/form-config-join]
   :ident :new-argument/id
   :form-fields #{:ui/new-argument :ui/new-subtype}
   :initial-state (fn [{:keys [argument/id pro?] :or {pro? true}}]
                    {:new-argument/id id
                     :ui/new-argument ""
                     :ui/pro? pro?
                     :ui/open? false
                     :ui/new-subtype :undermine})}
  (div :.collapse
    {:classes [(when open? "show")]}
    (form :.container.border
      {:onSubmit #(fn submit [e]
                    (evt/prevent-default! e)
                    #_(comp/transact! this [(add-argument {:argument new-argument
                                                           :subtype :subtype})]))}
      (div :.form-group
        (label "Dein neues Argument "
          (if pro?
            (a :.text-success {:onClick #(m/toggle! this :ui/pro?)} "dafür")
            (a :.text-danger {:onClick #(m/toggle! this :ui/pro?)} "dagegen"))
          ":")
        (input :.form-control
          {:type "text" :value new-argument
           :onChange #(m/set-string! this :ui/new-argument :event %)}))

      (div :.form-group
        (label "Wieso nennst du dieses Argument?")
        (select this
          :ui/new-subtype
          {:undermine "Undermine"
           :undercut "Undercut"}
          (name new-subtype)))

      (button :.btn.btn-primary
        {:type "submit"}
        "Submit"))))

(def ui-new-argument (comp/factory NewArgumentForm))

(defmutation open-add-new-argument [{:keys [argument/id pro?]}]
  (action [{:keys [state ref]}]
    (js/console.log ref)
    (swap! state update-in [:new-argument/id id]
      #(-> %
         (assoc :ui/pro? pro?)
         (assoc :ui/open? true)))))

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

(defsc ProCon [this {:argument/keys [id pros cons]} computed]
  {:query [:argument/id
           :argument/text
           :argument/type
           {:argument/pros (comp/get-query Argument)}
           {:argument/cons (comp/get-query Argument)}]
   :ident :argument/id
   :initial-state (fn [argument] (merge {:argument/pros []
                                         :argument/cons []}
                                   argument))}
  (div :.row
    (half-row
      (dom/h6 :.argumentation-header.bg-success "Pro Argumente"
        (button {:onClick #(comp/transact! this [(open-add-new-argument {:argument/id id
                                                                         :pro? true})])} "+"))
      (map #(ui-argument (comp/computed % computed)) pros))

    (half-row
      (dom/h6 :.argumentation-header.bg-danger "Con Argumente"
        (button {:onClick #(comp/transact! this [(open-add-new-argument {:argument/id id
                                                                         :pro? false})])} "+"))
      (map #(ui-argument (comp/computed % computed)) cons))))

(def ui-procon (comp/factory ProCon {:keyfn :argument/id}))

(defn *navigate-forward [{:argumentation/keys [current-argument] :as argumentation} next-argument]
  (-> argumentation
    (assoc :argumentation/current-argument next-argument)
    (update :argumentation/upstream conj current-argument)))

(defmutation navigate-forward [{id :argument/id}]
  (action [{:keys [ref state]}]
    (swap! state update-in ref *navigate-forward [:argument/id id])))

(defn *jump-backwards
  [{:argumentation/keys [upstream] :as argumentation}
   position]
  (if (contains? upstream position)
    (-> argumentation
      (assoc :argumentation/current-argument (nth upstream position))
      (update :argumentation/upstream subvec 0 position))
    argumentation))

(defmutation jump-backwards [{:keys [position]}]
  (action [{:keys [ref state]}]
    (swap! state update-in ref *jump-backwards position)))

(defsc UpstreamItem [_this {:argument/keys [text type]} {:keys [jmp-fn index]}]
  {:query [:argument/id :argument/text :argument/type]
   :ident :argument/id}
  (dom/button :.argumentation-upstream-item
    {:key index
     :data-type type
     :onClick jmp-fn}
    text))

(def ui-upstream-item (comp/factory UpstreamItem))

(defsc Argumentation [this {:argumentation/keys [upstream current-argument new-argument]}]
  {:query [:argument/id
           {:argumentation/upstream (comp/get-query UpstreamItem)}
           {:argumentation/current-argument (comp/get-query ProCon)}
           {:argumentation/new-argument (comp/get-query NewArgumentForm)}]
   :ident :argument/id
   :initial-state (fn [{:argument/keys [id] :as root-arg}]
                    {:argument/id id
                     :argumentation/new-argument (comp/initial-state NewArgumentForm {:argument/id id})
                     :argumentation/upstream []
                     :argumentation/current-argument (comp/initial-state ProCon (:argumentation/current-argument root-arg))})}

  (div
    (dom/ol :.list-group
      (map-indexed
        (fn [i props]
          (ui-upstream-item
            (comp/computed props
              {:jmp-fn #(comp/transact! this [(jump-backwards {:position i})])})))
        (concat upstream [current-argument])))
    (ui-new-argument new-argument)
    (ui-procon
      (comp/computed current-argument
        {:argumentation-root this}))))

(def ui-argumentation (comp/factory Argumentation {:keyfn :argument/id}))