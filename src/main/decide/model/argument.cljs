(ns decide.model.argument
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div button p a form option label input span]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro-css.css :as css]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
            [com.fulcrologic.fulcro.algorithms.merge :as mrg]
            [ghostwheel.core :as g :refer [>defn => | ?]]
            [clojure.spec.alpha :as s]
            [com.fulcrologic.fulcro.dom.events :as evt]
            ["react-icons/io" :refer [IoMdAdd]]
            [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
            [taoensso.timbre :as log]))

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

(defn select [comp attribute options current-value]
  (dom/select :.form-control
    {:onChange #(m/set-value! comp attribute (keyword (evt/target-value %)))
     :value current-value}
    (for [[type label] options
          :let [value (name type)]]
      (option {:key type
               :value value}
        label))))

(defn argument [id text type subtype]
  #:argument{:id id
             :text text
             :type type
             :subtype subtype})


(defn *add-argument [state {:keys [id text type subtype parent]}]
  (let [new-argument  (argument id text type subtype)
        parent-target (conj parent (if (= type :pro) :argument/pros :argument/cons))]
    (mrg/merge-component state Argument new-argument :append parent-target)))


(defmutation add-argument [{:keys [id text type subtype parent-ident] :as params}]
  (action [{:keys [state]}]
    (swap! state *add-argument params)))

(defsc NewArgumentForm [this {:argumentation/keys [id current-argument]
                              :ui/keys            [open? new-argument new-subtype pro?]}]
  {:query         [:argumentation/id
                   :argumentation/current-argument
                   :ui/open?
                   :ui/new-argument :ui/new-subtype :ui/pro?
                   fs/form-config-join]
   :ident         :argumentation/id
   :form-fields   #{:ui/new-argument :ui/new-subtype :ui/pro?}
   :initial-state (fn [{:keys [argumentation/id pro? open?]
                        :or   {pro?  true
                               open? false}}]
                    {:argumentation/id id
                     :ui/new-argument  ""
                     :ui/pro?          pro?
                     :ui/open?         open?
                     :ui/new-subtype   :undermine})}
  (div :.collapse
    {:classes [(when open? "show")]}
    (form :.container.border
      {:onSubmit (fn submit [e]
                   (evt/prevent-default! e)
                   (comp/transact! this [(add-argument {:id      (tempid/tempid)
                                                        :text    new-argument
                                                        :type    (if pro? :pro :con)
                                                        :subtype new-subtype
                                                        :parent  current-argument})]))}
      (div :.form-group
        (label "Dein neues Argument "
          (if pro?
            (a :.text-success {:onClick #(m/toggle! this :ui/pro?)} "dafür")
            (a :.text-danger {:onClick #(m/toggle! this :ui/pro?)} "dagegen"))
          ":")
        (input :.form-control
          {:type "text" :value new-argument
           :onChange #(m/set-string! this :ui/new-argument :event %)}))

      (when-not pro?
        (div :.form-group
          (label "Wieso nennst du dieses Argument?")
          (select this
            :ui/new-subtype
            {:undermine "Undermine"
             :undercut "Undercut"}
            (name (or (log/spy :info "New Subtype" new-subtype) "")))))

      (button :.btn.btn-primary
        {:type "submit"}
        "Submit"))))

(def ui-new-argument (comp/factory NewArgumentForm))

(defmutation open-add-new-argument [{:keys [pro?] :or {pro? true}}]
  (action [{:keys [state ref]}]
    (swap! state update-in ref
      assoc
      :ui/pro? pro?
      :ui/open? true)))


(defn half-row [& children]
  (div :.col-sm-6
    {:style {:display "flex"
             :flexDirection "column"}}
    children))

(defn merge-queries [query & clzs]
  (vec (apply concat query (map comp/get-query clzs))))

(defn ui-add-argument-button [type add-argument-fn]
  (button :.btn
    {:classes [(if (= type :pro) "btn-success" "btn-danger")]
     :onClick #(add-argument-fn (= type :pro))}
    "Argument hinzufügen"))

(defsc ProCon [this {:argument/keys [pros cons]}
               {:keys [add-argument-fn] :as computed
                :or   {add-argument-fn #()}}]
  {:query         (fn []
                    (into [:argument/id
                           {:argument/pros (comp/get-query Argument)}
                           {:argument/cons (comp/get-query Argument)}]
                      (comp/get-query Argument)))
   :ident :argument/id
   :initial-state (fn [argument] (merge {:argument/pros []
                                         :argument/cons []}
                                   argument))}
  (div :.row
    (half-row
      (div :.argumentation-header.bg-success
        (dom/h6 "Pro Argumente")
        (ui-add-argument-button :pro add-argument-fn))
      (map #(ui-argument (comp/computed % computed)) pros))

    (half-row
      (div :.argumentation-header.bg-danger
        (dom/h6 "Contra Argumente")
        (ui-add-argument-button :con add-argument-fn))
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

(def ui-upstream-item (comp/factory UpstreamItem {:keyfn :argument/id}))



(defsc Argumentation [this {:argumentation/keys [upstream current-argument new-argument]}]
  {:query         [:argument/id
                   {:argumentation/upstream (comp/get-query UpstreamItem)}
                   {:argumentation/current-argument (comp/get-query ProCon)}
                   {:argumentation/new-argument (comp/get-query NewArgumentForm)}]
   :ident         [:argumentation/id :argument/id]
   :initial-state (fn [{:argument/keys [id] :as root-arg}]
                    {:argument/id                    id
                     :argumentation/new-argument     (comp/initial-state NewArgumentForm {:argumentation/id id})
                     :argumentation/upstream         []
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
        {:argumentation-root this
         :add-argument-fn    (fn add-argument [pro?] (comp/transact! this [(open-add-new-argument {:pro? pro?})]))}))))

(def ui-argumentation (comp/factory Argumentation {:keyfn :argument/id}))