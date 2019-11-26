(ns decide.model.proposal
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div form p button input label]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.algorithms.merge :as mrg]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [clojure.string :refer [split-lines blank? split]]
            [clojure.edn :as edn]
            [decide.model.argument :as arg]
            ["react-icons/io" :refer [IoMdMore IoIosCheckmarkCircleOutline IoIosCloseCircleOutline IoMdClose]]
            [taoensso.timbre :as log]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [decide.model.argument :as argument]
            [com.fulcrologic.fulcro.dom.events :as evt]
            ["bootstrap/js/dist/modal"]))

(declare ProposalDetails)

(defn big-price-tag
  ([price budget]
   (big-price-tag price budget ""))
  ([price budget unit]
   (div :.price-tag-big
     (div (str price " " unit))
     (div "von " budget " " unit))))


(defmutation initial-load [{:keys [id]}]
  (action [{:keys [state app]}]
    (swap! state mrg/merge-component arg/Argumentation
      (comp/initial-state arg/Argumentation {:proposal/id id})
      :replace [:proposal/id id :ui/argumentation])

    (df/load! app [:proposal/id id] ProposalDetails
      {:post-mutation        `dr/target-ready
       :post-mutation-params {:target [:proposal/id id]}})
    #_(df/load! app [:argument/id id] argument/ProCon
        {:target [:argumentation/id id :argumentation/current-argument]})))


(defsc ProposalDetails [this {:keys          [argument/text >/argumentation]
                              :proposal/keys [subtext cost]}]
  {:query         [:proposal/id
                   :argument/text
                   :proposal/subtext
                   :proposal/cost
                   {:>/argumentation (comp/get-query arg/Argumentation)}]
   :ident         :proposal/id
   ;:initial-state (fn [proposal]
   ;                 {:ui/argumentation (comp/initial-state arg/Argumentation {})}
   :route-segment ["proposal" :proposal/id]
   :will-enter    (fn [app {id :proposal/id}]
                    (let [id (uuid id)]
                      (dr/route-deferred [:proposal/id id]
                        #(comp/transact! app [(initial-load {:id id})]))))}

  (div :.container.border
    {:style {:position "relative"}}
    (dom/button :.close
      {:style {:position "absolute"
               :top      ".5rem"
               :right    ".5rem"}
       :data-dismiss "modal"} (IoMdClose))
    (div :.row.justify-content-between.m-4
      (dom/h2 :.detail-card__header text)
      (big-price-tag cost 1000000000 "$"))
    (dom/p (interpose (dom/br) (split-lines subtext)))
    (arg/ui-argumentation (comp/computed argumentation {:argumentation-root this}))))

(def ui-proposal-detail (comp/factory ProposalDetails {:keyfn :argument/id}))

(defn proposal-card [comp {:proposal/keys [id subtitle cost]
                           :argument/keys [text]}]
  (div :.proposal
    (div :.proposal-buttons
      (dom/button :.btn.btn-outline-success
        (IoIosCheckmarkCircleOutline #js {:size "3rem"}))
      (dom/div :.spacer)
      (dom/button :.btn.btn-outline-danger
        (IoIosCloseCircleOutline #js {:size "3rem"})))

    (div :.proposal-content.btn-light
      {:style       {:cursor "pointer"}
       :data-toggle "modal"
       :data-target (str "#modal-" id)}
      (div
        {:style {:display        "flex"
                 :padding        "10px 5px 10px 10px"
                 :justifyContent "space-between"}}
        (dom/h4 :.proposal-title text)
        (dom/button :.btn
          {:style {:position "absolute"
                   :right    "0px"
                   :top      "0px"
                   :padding  "0.2rem 0"}}
          (IoMdMore #js {:size "24px"})))
      (div
        {:style {:display        "flex"
                 :padding        "10px"
                 :justifyContent "space-between"}}
        (div :.proposal-subtitle subtitle)
        (div :.proposal-price
          (dom/span :.proposal-price__text (str cost) " €"))))))

(defn form-dummy-proposal-card [{:proposal/keys [subtitle cost]
                                 :argument/keys [text]}]
  (div :.proposal.mx-auto
    (div :.proposal-buttons
      (dom/button :.btn.btn-outline-success
        {:disabled true}
        (IoIosCheckmarkCircleOutline #js {:size "3rem"}))
      (dom/div :.spacer)
      (dom/button :.btn.btn-outline-danger
        {:disabled true}
        (IoIosCloseCircleOutline #js {:size "3rem"})))

    (div :.proposal-content
      (div
        {:style {:display        "flex"
                 :padding        "10px 5px 10px 10px"
                 :justifyContent "space-between"}}
        (dom/h4 :.proposal-title text))
      (div
        {:style {:display        "flex"
                 :padding        "10px"
                 :justifyContent "space-between"}}
        (dom/small :.proposal-subtitle subtitle)
        (div :.proposal-price
          (dom/span :.proposal-price__text (str cost) " €"))))))

(defsc ProposalCard [this {:keys          [>/details]
                           :proposal/keys [id subtitle cost]
                           :argument/keys [text] :as props}]
  {:query         [:proposal/id :argument/text :proposal/subtitle :proposal/cost
                   {:>/details (comp/get-query ProposalDetails)}]
   :ident         :proposal/id
   :initial-state (fn [_] {:ui/modal-open? false})}
  [(proposal-card this props)
   (div :.modal.fade
     {:id (str "modal-" id)}
     (div :.modal-dialog.modal-xl
       (div :.modal-content
         (div :.modal-body
           (div {:style {:width "auto"}}
             (ui-proposal-detail details))))))])

(def ui-proposal-card (comp/factory ProposalCard))

(defn field [{:keys [label valid? error-message] :as props}]
  (let [input-props (-> props (assoc :name label) (dissoc :label :valid? :error-message))]
    (div :.form-group.field
      (dom/label {:htmlFor label} label)
      (dom/input input-props)
      (dom/div :.ui.error.message {:classes [(when valid? "hidden")]}
        error-message))))

(defn- with-placeholder [value placeholder]
  (if (blank? value)
    placeholder
    value))

(defsc EnterProposal [this {:keys [title cost summary]}]
  {:query         [:title :cost :summary fs/form-config-join]
   :initial-state (fn [_]
                    (fs/add-form-config EnterProposal
                      {:title   ""
                       :cost    ""
                       :summary ""}))
   :ident         (fn [] [:component/id :new-proposal])
   :form-fields   #{:title :cost :summary}}
  (let [title-max-length    100
        title-warn-length   (- title-max-length 10)
        summary-max-length  500
        summary-warn-length (- summary-max-length 20)

        short-summary       (first (split summary #"\n\s*\n" 1))]
    (div
      (form-dummy-proposal-card {:argument/text     (with-placeholder title "Es sollte ein Wasserspender im Flur aufgestellt werden.")
                                 :proposal/cost     (with-placeholder cost "0")
                                 :proposal/subtitle (with-placeholder short-summary "Ein Wasserspender sorgt dafür, dass alle Studenten und Mitarbeiter mehr trinken. Dies sorgt für ein gesünderes Leben.")})
      (form :.p-5
        {:onSubmit evt/prevent-default!}

        (let [approaching-limit? (> (count title) title-warn-length)
              chars-exceeded?    (> (count title) title-max-length)]
          (div :.form-group
            (label "Vorschlag")
            (input :.form-control
              {:placeholder "Es sollte ein Wasserspender im Flur aufgestellt werden."
               :value       title
               :required    true
               :onChange    #(m/set-string! this :title :event %)})
            (dom/small :.form-text
              {:style   {:display (when-not approaching-limit? "none")}
               :classes [(when chars-exceeded? "text-danger")]}
              (str (count title) "/" title-max-length " Buchstaben")
              (when chars-exceeded? ". Bitte fassen Sie sich kurz!"))))

        (div :.form-group
          (label "Geschätzte Kosten")
          (input :.form-control
            {:type        "number"
             :value       cost
             :placeholder "1000"
             :min         "0"
             :step        "1"
             :required    true
             :onChange    #(m/set-string! this :cost :event %)}))

        (let [approaching-limit? (> (count title) summary-warn-length)
              chars-exceeded?    (> (count title) summary-max-length)]
          (div :.form-group
            (label "Details zum Vorschlag")
            (dom/textarea :.form-control
              {:placeholder "Ein Wasserspender sorgt dafür, dass alle Studenten und Mitarbeiter mehr trinken. Dies sorgt für ein gesünderes Leben."
               :onChange    #(m/set-string! this :summary :event %)
               :value       summary})
            (dom/small :.form-text
              {:style   {:display (when-not approaching-limit? "none")}
               :classes [(when chars-exceeded? "text-danger")]}
              (str (count title) "/" title-max-length " Buchstaben")
              (when chars-exceeded? ". Bitte beschränken Sie sich auf das Limit!"))))

        (button :.btn.btn-primary "Submit")))))

(def ui-new-proposal-form (comp/factory EnterProposal))

(defsc ProposalCollection [this {:keys [all-proposals new-proposal-form]}]
  {:query         [{[:all-proposals '_] (comp/get-query ProposalCard)}
                   {:new-proposal-form (comp/get-query EnterProposal)}]
   :initial-state (fn [_] {:all-proposals     []
                           :new-proposal-form (comp/initial-state EnterProposal nil)})
   :ident         (fn [] [:component/id :proposals])
   :route-segment ["proposals"]
   :will-enter    (fn [app _]
                    (dr/route-deferred [:component/id :proposals]
                      #(df/load! app :all-proposals ProposalCard
                         {:post-mutation        `dr/target-ready
                          :post-mutation-params {:target [:component/id :proposals]}})))}
  (div :.container
    (button :.btn.btn-outline-primary "Neuen Vorschlag hinzufügen")
    (div :.collapse.show.p-3.border
      (ui-new-proposal-form new-proposal-form))
    (div :.card-deck.d-flex.justify-content-center
      (for [proposal all-proposals]
        (dom/div :.col-lg-6.p-3
          (ui-proposal-card proposal))))))