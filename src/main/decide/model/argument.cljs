(ns decide.model.argument
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div button p a form option label input span small h6]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro-css.css :as css]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
            [com.fulcrologic.fulcro.algorithms.merge :as mrg]
            [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
            [decide.util :as util]
            [clojure.spec.alpha :as s]
            [com.fulcrologic.fulcro.dom.events :as evt]
            ["react-icons/io" :refer [IoMdAdd IoMdClose IoMdMore IoMdUndo]]
            ["react-icons/md" :refer [MdSubdirectoryArrowRight]]
            [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
            [taoensso.timbre :as log]
            [com.fulcrologic.fulcro.data-fetch :as df]
            ["bootstrap/js/dist/dropdown"]
            [decide.model.session :as session]
            [com.fulcrologic.fulcro.application :as app]))


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

(defn *navigate-forward [{:>/keys [current-argument] :as argumentation} next-argument]
  (log/info "Navigate forward upstream:" argumentation)
  (-> argumentation
    (assoc :>/current-argument next-argument)
    (update :argumentation/upstream (fnil conj []) current-argument)))

(defmutation navigate-forward [{id :argument/id}]
  (action [{:keys [ref state]}]
    (swap! state update-in ref *navigate-forward [:argument/id id])))

(defsc Argument [this {:argument/keys [id text pros cons author]} {:keys [argumentation-root]}]
  {:query [:argument/id
           :argument/text
           :argument/type                                   ; pro, con, position, ...
           :argument/subtype                                ; undercut, undermine, ...
           :argument/author
           {:argument/pros 1}
           {:argument/cons 1}]
   :ident :argument/id
   :css   [[:.argument {:position      "relative"
                        :border        "1px solid black"
                        :border-radius "10px"
                        :padding       "24px"
                        :text-align    "left"
                        :cursor        "pointer"}]
           [:.sublayers {:display     "inline-block"
                         :position    "absolute"
                         :bottom      "0"
                         :right       "0"
                         :padding     "5px 10px"
                         :width       "auto"
                         :margin-left "auto"}]]}
  (let [{:keys [argument sublayers]} (css/get-classnames Argument)]
    (div :.btn.btn-light.my-1
      {:classes      [argument]
       :onMouseEnter #(df/load! this [:argument/id id] Argument)
       :onClick      #(comp/transact! argumentation-root [(navigate-forward {:argument/id id})])}
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
           :onClick       (fn [e] (evt/stop-propagation! e))}
          (IoMdMore))
        (div :.dropdown-menu.dropdown-menu-right.border
          (h6 :.dropdown-header "Aktionen")
          (when-not author
            (a :.dropdown-item.text-danger
              {:onClick
               (fn [e]
                 (evt/stop-propagation! e)
                 (comp/transact! this [(retract-argument {:argument/id id})]))}
              "Löschen"))))
      (small
        {:classes [sublayers]}
        (span :.text-success.pr-2 (MdSubdirectoryArrowRight) (str (count pros)))
        (span :.text-danger (MdSubdirectoryArrowRight) (str (count cons)))))))


(def ui-argument (comp/computed-factory Argument {:keyfn (util/prefixed-keyfn :argument :argument/id)}))

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

(defmutation new-argument [{:argument/keys [id text type subtype parent]}]
  (action [{:keys [state]}]
    (let [new-argument  (argument id text type subtype)
          parent-target (conj parent (if (= type :pro) :argument/pros :argument/cons))]
      (swap! state mrg/merge-component Argument new-argument
        :append parent-target)))
  (remote [_] true))

(defmutation reset-new-argument-form [_]
  (action [{:keys [state ref]}]
    (swap! state update-in ref assoc :ui/new-argument "")))

(defn pro-con-toggle [comp]
  (if (:ui/pro? (comp/props comp))
    (a :.text-success
      {:style   {:textDecoration "underline"}
       :href    "#"
       :onClick #(m/toggle! comp :ui/pro?)} "dafür")
    (a :.text-danger
      {:style   {:textDecoration "underline"}
       :href    "#"
       :onClick #(m/toggle! comp :ui/pro?)} "dagegen")))

(defsc NewArgumentForm [this {:keys    [>/current-argument proposal/id]
                              :ui/keys [open? new-argument new-subtype pro?]
                              :as      props
                              :or      {new-argument ""}}]
  {:query              [:proposal/id
                        :>/current-argument
                        :ui/open?
                        :ui/new-argument :ui/new-subtype :ui/pro?
                        {[:component/id :session] [:>/current-user]}
                        fs/form-config-join]
   :ident              [:argumentation/id :proposal/id]
   :form-fields        #{:ui/new-argument :ui/new-subtype :ui/pro?}
   :componentDidUpdate (fn [this _prev-props _prev-state]
                         (let [{:ui/keys [new-subtype pro?]} (comp/props this)]
                           (if (#{:undermine :undercut} new-subtype) ;; TODO Merge this with the Toggle Button!
                             (when pro?
                               (m/set-value! this :ui/new-subtype :support))
                             (when-not pro?
                               (m/set-value! this :ui/new-subtype :undermine)))))
   :initial-state      (fn [{:keys [proposal/id pro? open?]
                             :or   {pro?  true
                                    open? false}}]
                         {:proposal/id     id
                          :ui/new-argument ""
                          :ui/pro?         pro?
                          :ui/open?        open?
                          :ui/new-subtype  :support})}
  (div :.collapse.container.border.p-4.my-3
    {:classes [(when open? "show")]
     :id      (str "collapse-" id)}
    (dom/button :.close
      {:type    "button"
       :style   {:position "relative"
                 :top      "-1.2rem"
                 :right    "-1.2rem"}
       :onClick #(m/toggle! this :ui/open?)}
      (IoMdClose))
    (form
      {:onSubmit (fn submit [e]
                   (evt/prevent-default! e)
                   (log/info current-argument)
                   (comp/transact! this
                     [(new-argument
                        #:argument{:id      (tempid/tempid)
                                   :text    new-argument
                                   :type    (if pro? :pro :con)
                                   :subtype new-subtype
                                   :parent  current-argument
                                   :author  (get-in props [[:component/id :session] :>/current-user])})
                      (reset-new-argument-form nil)
                      (m/toggle {:field :ui/open?})]))}
      (div :.form-group
        (label "Dein neues Argument " (pro-con-toggle this) ":")
        (input :.form-control
          {:type     "text"
           :value    new-argument
           :onChange #(m/set-string! this :ui/new-argument :event %)}))

      (when-not pro?
        (div :.form-group
          (label " Wieso nennst du dieses Argument?")
          (select this
            :ui/new-subtype
            {:undermine "Undermine"
             :undercut  "Undercut"}
            (name (or new-subtype "")))))

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


(defn ui-new-argument-button [type new-argument-fn]
  (button :.btn.btn-sm
    {:classes [(if (= type :pro) "btn-success" "btn-danger")]
     :onClick #(new-argument-fn (= type :pro))}
    "Argument hinzufügen"))

(defsc ProCon [this {:argument/keys [pros cons] :as props}
               {:keys [new-argument-fn] :as computed
                :or   {new-argument-fn #()}}]
  {:query         #(into
                     [:argument/id
                      {:argument/pros (comp/get-query Argument)}
                      {:argument/cons (comp/get-query Argument)}
                      session/valid?-query]
                     (comp/get-query Argument))
   :ident         :argument/id
   :initial-state (fn [{:keys [id] :as argument}]
                    (merge #:argument{:id id, :pros [], :cons []} argument))}
  (let [logged-in? (session/get-logged-in? props)]
    (div :.row.pt-1
      (div :.col-6
        {:style {:display       "flex"
                 :flexDirection "column"}}
        (div :.row.m-sm-0.ml-1.alert.alert-success.d-flex.justify-content-between.align-items-center
          "Pro Argumente"
          (button :.btn.btn-sm.btn-success
            {:title    (str "Füge ein Argument dafür hinzu. " (when-not logged-in? "Man muss eingeloggt sein"))
             :onClick  #(new-argument-fn true)
             :disabled (not logged-in?)}
            "Argument hinzufügen"))
        (if (empty? pros)
          (p :.p-3.text-muted "Es gibt noch keine Pro-Argumente. "
            #_(a {:onClick (fn [e]
                             (evt/prevent-default! e)
                             (new-argument-fn true))} "Füge eins hinzu!"))
          (map #(ui-argument % computed) pros)))

      (div :.col-6
        {:style {:display       "flex"
                 :flexDirection "column"}}
        (div :.row.m-sm-0.mr-1.alert.alert-danger.d-flex.justify-content-between.align-items-center
          "Contra Argumente"
          (button :.btn.btn-sm.btn-danger
            {:title    (str "Füge ein Gegenargument hinzu. " (when-not logged-in? "Man muss eingeloggt sein"))
             :onClick  #(new-argument-fn false)
             :disabled (not logged-in?)}
            "Argument hinzufügen"))
        (if (empty? cons)
          (p :.p-3.text-muted "Es gibt noch keine Contra-Argumente. "
            #_(a {:href    ""
                  :onClick (fn [e]
                             (evt/prevent-default! e)
                             (new-argument-fn false))} "Füge eins hinzu!"))
          (map #(ui-argument % computed) cons))))))

(def ui-procon (comp/computed-factory ProCon {:keyfn (util/prefixed-keyfn :procon :argument/id)}))

(defn *jump-backwards
  [{:argumentation/keys [upstream] :as argumentation}
   position]
  (if (contains? upstream position)
    (-> argumentation
      (assoc :>/current-argument (nth upstream position))
      (update :argumentation/upstream subvec 0 position))
    argumentation))

(defmutation jump-backwards [{:keys [position]}]
  (action [{:keys [ref state]}]
    (swap! state update-in ref *jump-backwards position)))

(defsc UpstreamItem [_this {:argument/keys [text type]} {:keys [onClick index]}]
  {:query [:argument/id :argument/text :argument/type]
   :ident :argument/id}
  (button :.argumentation-upstream-item.my-1
    {:key       index
     :data-type type
     :onClick   onClick
     :title     "Springe zurück zu diesem Argument"}
    (span :.ml-auto text)
    (span :.ml-auto.pl-2 (IoMdUndo))))

(def ui-upstream-item (comp/computed-factory UpstreamItem {:keyfn (util/prefixed-keyfn :upstream-item :argument/id)}))

(>defn ns?
  ([ns]
   [string? => fn?]
   (fn [map-entry] (ns? ns map-entry)))
  ([ns map-entry]
   [string? map-entry? => boolean?]
   (= ns (namespace (first map-entry)))))

(defsc Argumentation [this {:keys               [proposal/id >/current-argument]
                            :argumentation/keys [upstream new-argument]}]
  {:query         [:proposal/id
                   {:argumentation/upstream (comp/get-query UpstreamItem)}
                   {:>/current-argument (comp/get-query ProCon)}
                   {:argumentation/new-argument (comp/get-query NewArgumentForm)}]
   :ident         [:argumentation/id :proposal/id]
   :initial-state (fn [{:proposal/keys [id] :as params}]
                    (let [argument (into {:argument/id id} (filter (ns? "argument")) params)]
                      {:proposal/id                id
                       :argumentation/new-argument (comp/initial-state NewArgumentForm {:proposal/id id})
                       :argumentation/upstream     []
                       :>/current-argument         (comp/initial-state ProCon argument)}))}
  (div
    (dom/ol :.list-group
      (map-indexed
        (fn [i props]
          (ui-upstream-item props
            {:onClick #(comp/transact! this [(jump-backwards {:position i})])}))
        (concat upstream [current-argument])))
    (ui-new-argument (merge {:proposal/id id} new-argument))
    (when current-argument
      (ui-procon current-argument
        {:argumentation-root this
         :new-argument-fn    (fn new-argument [pro?] (comp/transact! this [(open-add-new-argument {:pro? pro?})]))}))))

(def ui-argumentation (comp/factory Argumentation {:keyfn (util/prefixed-keyfn :argumentation :proposal/id)}))