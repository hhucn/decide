(ns decide.model.argument
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div button p a form option label input span]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro-css.css :as css]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
            [com.fulcrologic.fulcro.algorithms.merge :as mrg]
            [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
            [clojure.spec.alpha :as s]
            [com.fulcrologic.fulcro.dom.events :as evt]
            ["react-icons/io" :refer [IoMdAdd IoMdClose IoMdFunnel IoMdMore]]
            [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
            [taoensso.timbre :as log]
            [com.fulcrologic.fulcro.data-fetch :as df]
            ["bootstrap"]))


(s/def :argument/id (some-fn uuid? tempid/tempid?))
(s/def :argument/text string?)
(s/def :argument/type #{:pro :con :position})
(s/def :argument/pros (s/coll-of (s/or
                                   :tree-form ::argument
                                   :ident (s/tuple keyword? :argument/id))))
(s/def :argument/cons (s/coll-of (s/or
                                   :tree-form ::argument
                                   :ident (s/tuple keyword? :argument/id))))
(s/def ::argument (s/keys :req [:argument/id]
                    :opt [:argument/text :argument/type
                          :argument/pros :argument/cons]))

(>defn retract-argument-from-arguments [argument ident]
  [::argument vector? => ::argument]
  (-> argument
    (mrg/remove-ident* ident [:argument/pros])
    (mrg/remove-ident* ident [:argument/cons])))


(defmutation retract-argument [{:keys [argument/id]}]
  (action [{:keys [state]}]
    (swap! state update :argument/id
      #(into {} (map (fn [[k v]] [k (retract-argument-from-arguments v [:argument/id id])])) %)))
  (remote [_] true))


(defsc Argument [this {:argument/keys [id text pros cons]} {:keys [argumentation-root]}]
  {:query [:argument/id
           :argument/text
           :argument/type ; pro, con, position, ...
           :argument/subtype ; undercut, undermine, ...
           {:argument/pros '...}
           {:argument/cons '...}]
   :ident :argument/id}
  (div :.btn.btn-light.my-1
    {:style
              {:position     "relative"
               :border       "1px solid black"
               :borderRadius "10px"
               :padding      "24px"
               :textAlign    "left"}
     :onClick #(comp/transact! argumentation-root `[(navigate-forward {:argument/id ~id})])}
    text
    (div :.btn-group
      {:style {:position "absolute" :top "0px" :right "0px"
               :padding  "0"}}
      (button :.close
        {:style         {:backgroundColor "transparent"
                         :border          "0"
                         :zIndex          "100"}
         :data-toggle   "dropdown"
         :aria-expanded "false"
         :onClick       (fn [e]
                          (evt/stop-propagation! e)
                          (log/info "Open Dropdown!"))}
        (IoMdMore))
      (div :.dropdown-menu.dropdown-menu-right.border
        (a :.dropdown-item.bg-danger.text-white
          {:onClick
           (fn [e]
             (evt/stop-propagation! e)
             (comp/transact! this [(retract-argument {:argument/id id})]))}
          "Löschen")))
    (div :.ml-auto.small
      {:style {:display  "inline-block "
               :position "absolute "
               :bottom   "0"
               :right    "0"
               :padding  "5px 10px"
               :width    " auto"}}
      (span :.text-success.pr-2 (IoMdFunnel) (str (count pros)))
      (span :.text-danger (IoMdFunnel) (str (count cons))))))


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

(defmutation add-argument [{:keys [id text type subtype parent]}]
  (action [{:keys [state]}]
    (let [new-argument  (argument id text type subtype)
          parent-target (conj parent (if (= type :pro) :argument/pros :argument/cons))]
      (swap! state mrg/merge-component Argument new-argument
        :append parent-target)))
  (remote [_] true))

(defsc NewArgumentForm [this {:argumentation/keys [current-argument]
                              :ui/keys            [open? new-argument new-subtype pro?]
                              :or                 {new-argument " "}}]
  {:query              [:proposal/id
                        :argumentation/current-argument
                        :ui/open?
                        :ui/new-argument :ui/new-subtype :ui/pro?
                        fs/form-config-join]
   :ident              [:argumentation/id :proposal/id]
   :form-fields        #{:ui/new-argument :ui/new-subtype :ui/pro?}
   :componentDidUpdate (fn [this _prev-props _prev-state]
                         (let [{:ui/keys [new-subtype pro?]} (comp/props this)]
                           (if (= new-subtype :support)
                             (when-not pro?
                               (m/set-value! this :ui/new-subtype :undermine))
                             (when pro?
                               (m/set-value! this :ui/new-subtype :support)))))
   :initial-state      (fn [{:keys [proposal/id pro? open?]
                             :or   {pro?  true
                                    open? false}}]
                         {:proposal/id     id
                          :ui/new-argument ""
                          :ui/pro?         pro?
                          :ui/open?        open?
                          :ui/new-subtype  :undermine})}
  (div :.collapse.container.border.p-4.my-3
    {:classes [(when open? " show")]}
    (dom/button :.close
      {:type    " button"
       :style   {:position " relative"
                 :top      " -1.2rem"
                 :right    " -1.2rem"}
       :onClick #(m/toggle! this :ui/open?)}
      (IoMdClose))
    (form
      {:onSubmit (fn submit [e]
                   (evt/prevent-default! e)
                   (comp/transact! this [(add-argument {:id      (tempid/tempid)
                                                        :text    new-argument
                                                        :type    (if pro? :pro :con)
                                                        :subtype new-subtype
                                                        :parent  current-argument})]))}
      (div :.form-group
        (label " Dein neues Argument "
          (if pro?
            (a :.text-success {:onClick #(m/toggle! this :ui/pro?)} " dafür")
            (a :.text-danger {:onClick #(m/toggle! this :ui/pro?)} " dagegen"))
          " :")
        (input :.form-control
          {:type     " text" :value new-argument
           :onChange #(m/set-string! this :ui/new-argument :event %)}))

      (when-not pro?
        (div :.form-group
          (label " Wieso nennst du dieses Argument?")
          (select this
            :ui/new-subtype
            {:undermine " Undermine"
             :undercut  " Undercut"}
            (name (or new-subtype " ")))))

      (button :.btn.btn-primary
        {:type "submit "}
        "Submit "))))

(def ui-new-argument (comp/factory NewArgumentForm))

(defmutation open-add-new-argument [{:keys [pro?] :or {pro? true}}]
  (action [{:keys [state ref]}]
    (swap! state update-in ref
      assoc
      :ui/pro? pro?
      :ui/open? true)))


(defn half-row [& children]
  (div :.col-6
    {:style {:display       "flex "
             :flexDirection "column "}}
    children))

(defn ui-add-argument-button [type add-argument-fn]
  (button :.btn
    {:classes [(if (= type :pro) "btn-success " "btn-danger ")]
     :onClick #(add-argument-fn (= type :pro))}
    "Argument hinzufügen"))

(defsc ProCon [this {:argument/keys [pros cons]}
               {:keys [add-argument-fn] :as computed
                :or   {add-argument-fn #()}}]
  {:query         #(into [:argument/id
                          {:argument/pros (comp/get-query Argument)}
                          {:argument/cons (comp/get-query Argument)}]
                     (comp/get-query Argument))
   :ident         :argument/id
   :initial-state (fn [{:keys [id] :as argument}]
                    (merge #:argument{:id id, :pros [], :cons []} argument))}
  (div :.row
    (half-row
      (div :.argumentation-header.bg-success
        (dom/h6 " Pro Argumente ")
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
  (dom/button :.argumentation-upstream-item.my-1
    {:key index
     :data-type type
     :onClick jmp-fn}
    text))

(def ui-upstream-item (comp/factory UpstreamItem {:keyfn :argument/id}))

(>defn ns?
  ([ns]
   [string? => fn?]
   (fn [map-entry] (ns? ns map-entry)))
  ([ns map-entry]
   [string? map-entry? => boolean?]
   (= ns (namespace (first map-entry)))))

(defsc Argumentation [this {:keys               [proposal/id]
                            :argumentation/keys [upstream current-argument new-argument]}]
  {:query         [:proposal/id
                   {:argumentation/upstream (comp/get-query UpstreamItem)}
                   {:argumentation/current-argument (comp/get-query ProCon)}
                   {:argumentation/new-argument (comp/get-query NewArgumentForm)}]
   :ident         [:argumentation/id :proposal/id]
   :initial-state (fn [{:proposal/keys [id] :as params}]
                    (let [argument (into {:argument/id id} (filter (ns? "argument")) params)]
                      {:proposal/id                    id
                       :argumentation/new-argument     (comp/initial-state NewArgumentForm {:proposal/id id})
                       :argumentation/upstream         []
                       :argumentation/current-argument (comp/initial-state ProCon argument)}))}
  (div
    (dom/ol :.list-group
      (map-indexed
        (fn [i props]
          (ui-upstream-item
            (comp/computed props
              {:jmp-fn #(comp/transact! this [(jump-backwards {:position i})])})))
        (concat upstream [current-argument])))
    (ui-new-argument (merge {:proposal/id id} new-argument))
    (ui-procon
      (comp/computed current-argument
        {:argumentation-root this
         :add-argument-fn    (fn add-argument [pro?] (comp/transact! this [(open-add-new-argument {:pro? pro?})]))}))))

(def ui-argumentation (comp/factory Argumentation {:keyfn :proposal/id}))