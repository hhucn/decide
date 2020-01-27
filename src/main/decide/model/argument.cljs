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
            ["react-icons/md" :refer [MdSubdirectoryArrowRight MdModeComment]]
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

(def argument-card-bottom-decoration
  (small :.container-fluid                                  ;.border.border-secondary.rounded-bottom
    {:style {:position "absolute"
             :bottom   "-5px"
             :height   "5px"
             :left     "1%"
             :width    "98%"}}
    (div :.row.text-center
      (div :.col-6.px-1
        (div :.border.border-secondary.rounded-bottom
          {:style {:minHeight "5px"}}))
      (div :.col-6.px-1
        (div :.border.border-secondary.rounded-bottom
          {:style {:minHeight "5px"}})))))

(>defn seperate-subargument-display [pros cons]
  [coll? coll? => dom/element?]
  (comp/fragment
    (span :.text-success.mr-1 "Pro " (MdSubdirectoryArrowRight) (str (count pros)))
    (span :.text-danger "Con " (MdSubdirectoryArrowRight) (str (count cons)))))

(>defn subargument-display [pros cons]
  [coll? coll? => dom/element?]
  (span :.text-muted (MdModeComment) " " (str (+ (count pros) (count cons)))))

(defsc Argument [this {:argument/keys [id text pros cons author]} {:keys [argumentation-root]}]
  {:query [:argument/id
           :argument/text
           :argument/type                                   ; pro, con, position, ...
           :argument/subtype                                ; undercut, undermine, ...
           :argument/author
           {:argument/pros 1}
           {:argument/cons 1}]
   :ident :argument/id
   :css   [[:.argument-card {:min-height "5em"}]
           [:.argument-body {:padding ".75rem 1.9rem .75rem .75rem"}]
           [:.options-btn {:position  "absolute" :top "0px" :right "0px"
                           :width     "1.2em"
                           :height    "1.2em"
                           :border    "0"
                           :font-size "calc(1.275rem + 0.3vw)"
                           :z-index   "10000"}]]}
  (let [{:keys [argument-card argument-body options-btn]} (css/get-classnames Argument)]
    (div :.mx-0.card.bg-light.rounded.border.border-secondary.mb-3
      {:classes [argument-card]}
      (div :.card-body.text-left {:classes [argument-body]}
        (span :.card-text text))
      (div :.card-footer.border-0.bg-transparent.p-2.d-flex.align-items-center.justify-content-between
        (button :.btn.btn-sm.btn-link.stretched-link
          {:onMouseEnter #(df/load! this [:argument/id id] Argument)
           :onClick      #(comp/transact! argumentation-root [(navigate-forward {:argument/id id})])}
          "Mehr Argumente")
        (small :.float-right (subargument-display pros cons)))
      (div :.p-1 {:classes [options-btn]}
        (button :.close
          {:data-toggle   "dropdown"
           :aria-expanded "false"
           :onClick       evt/stop-propagation!}
          (IoMdMore))
        (div :.dropdown-menu.dropdown-menu-right.border
          {:onClick evt/stop-propagation!}
          (h6 :.dropdown-header "Aktionen")
          (when-not author
            (a :.dropdown-item.disabled
              {:onClick
               (fn [e]
                 (evt/stop-propagation! e)
                 (comp/transact! this [(retract-argument {:argument/id id})]))}
              "Löschen"))))
      argument-card-bottom-decoration)))

(def ui-argument (comp/computed-factory Argument {:keyfn (util/prefixed-keyfn :argument :argument/id)}))

(>defn select [on-change options current-value]
  [[any? => any?] (s/map-of any? dom/element?) string? => dom/element?]
  (dom/select :.form-control
    {:onChange on-change
     :value    current-value}
    (for [[type label] options
          :let [value (name type)]]
      (option {:key   type
               :value value}
        label))))

(>defn radio-select [on-change options current-value]
  [[any? => any?] (s/map-of any? dom/element?) string? => dom/element?]
  (dom/div
    {:onChange on-change}
    (for [[type label] options
          :let [value (name type)]]
      (dom/li :.form-check {:key type}
        (input :.form-check-input
          {:type    "radio"
           :value   value
           :checked (= value current-value)})
        (dom/label :.form-check-label label)))))

(defn argument [id text type subtype]
  #:argument{:id      id
             :text    text
             :type    type
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

(>defn pro-con-toggle [comp]
  [comp/component? => dom/element?]
  (let [pro?  (:ui/pro? (comp/props comp))
        class (if pro? :.text-success :.text-danger)
        text  (if pro? "für" "gegen")]
    (a {:title   "Wechsle zwischen dafür/dagegen"
        :classes [class]
        :role    "button"
        :style   {:textDecoration "underline"
                  :cursor         "pointer"}
        :onClick #(m/toggle! comp :ui/pro?)}
      text)))

(defsc NewArgumentForm [this {:keys    [>/current-argument proposal/id]
                              :ui/keys [open? new-argument-text new-subtype pro?]
                              :as      props
                              :or      {new-argument-text ""}}]
  {:query              [:proposal/id
                        {:>/current-argument (comp/get-query Argument)}
                        :ui/open?
                        :ui/new-argument-text :ui/new-subtype :ui/pro?
                        {[:component/id :session] [:>/current-user]}
                        fs/form-config-join]
   :ident              [:argumentation/id :proposal/id]
   :form-fields        #{:ui/new-argument-text :ui/new-subtype :ui/pro?}
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
                         {:proposal/id          id
                          :ui/new-argument-text ""
                          :ui/pro?              pro?
                          :ui/open?             open?
                          :ui/new-subtype       :support})}
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
                   (comp/transact! this
                     [(new-argument
                        #:argument{:id      (tempid/tempid)
                                   :text    new-argument-text
                                   :type    (if pro? :pro :con)
                                   :subtype new-subtype
                                   :parent  (comp/get-ident Argument current-argument)
                                   :author  (get-in props [[:component/id :session] :>/current-user])})
                      (reset-new-argument-form nil)
                      (m/toggle {:field :ui/open?})]))}
      (dom/p "Du bist " (pro-con-toggle this) ": ")
      (dom/p
        {:data-subtype (:argument/type current-argument)
         :classes      [(case (:argument/type current-argument)
                          :position "text-info"
                          :pro "text-success"
                          :con "text-danger"
                          "")]}
        (:argument/text current-argument))

      (when (and
              (not pro?)
              (not= :position (:argument/type current-argument)))
        (div :.form-group
          #_(label " Wieso nennst du dieses Argument?")
          (radio-select #(m/set-value! this :ui/new-subtype (keyword (evt/target-value %)))
            {:undermine (dom/span "Du findest \"" (dom/i (:argument/text current-argument)) "\" ist nicht richtig.")
             :undercut  (dom/span "\"" (dom/i (:argument/text current-argument)) "\" hat nichts mit dem Argument davor zu tun.")}
            (name (or new-subtype "")))))

      (div :.form-group
        (label "Dein Grund ist: ")
        (input :.form-control
          {:type     "text"
           :value    new-argument-text
           :onChange #(m/set-string! this :ui/new-argument-text :event %)}))

      (button :.btn.btn-primary
        {:type "submit"}
        "Fertig"))))

(def ui-new-argument (comp/factory NewArgumentForm))

(defmutation open-add-new-argument [{:keys [pro?] :or {pro? true}}]
  (action [{:keys [state ref]}]
    (swap! state update-in ref
      assoc
      :ui/pro? pro?
      :ui/open? true)))

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
        (div :.row.mx-sm-0.ml-0.alert.alert-success.d-flex.justify-content-between.align-items-center.mb-2
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
        (div :.row.mx-sm-0.mr-1.alert.alert-danger.d-flex.justify-content-between.align-items-center.mb-2
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
                      (when id
                        {:proposal/id                id
                         :argumentation/new-argument (comp/get-initial-state NewArgumentForm {:proposal/id id})
                         :argumentation/upstream     []
                         :>/current-argument         (comp/get-initial-state ProCon argument)})))}
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