(ns decide.model.proposal
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div form p button input label h2 h4 span small textarea br]]
            [com.fulcrologic.fulcro.dom.events :as evt]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [com.fulcrologic.fulcro.algorithms.merge :as mrg]
            [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
            [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [com.fulcrologic.fulcro.data-fetch :as df]
            [com.fulcrologic.guardrails.core :refer [>defn => | ? <-]]
            [clojure.string :as str]
            [taoensso.timbre :as log]
            [decide.model.argument :as arg]
            ["react-icons/io" :refer [IoMdMore IoIosCheckmarkCircleOutline IoIosCloseCircleOutline IoMdClose]]
            ["bootstrap/js/dist/modal"]
            ["bootstrap/js/dist/collapse"]
            [com.fulcrologic.fulcro-css.css :as css]))

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
      :replace [:proposal/id id :>/argumentation])

    (df/load! app [:proposal/id id] ProposalDetails
      {:post-mutation        `dr/target-ready
       :post-mutation-params {:target [:proposal/id id]}})))


(defsc ProposalDetails [this {:keys          [argument/text >/argumentation process/budget]
                              :proposal/keys [details cost]}]
  {:query         [:proposal/id
                   :argument/text
                   :proposal/details
                   :proposal/cost
                   :process/budget
                   {:>/argumentation (comp/get-query arg/Argumentation)}]
   :ident         :proposal/id
   :route-segment ["proposal" :proposal/id]
   :will-enter    (fn [app {id :proposal/id}]
                    (let [id (uuid id)]
                      (dr/route-deferred [:proposal/id id]
                        #(comp/transact! app [(initial-load {:id id})]))))}
  (div :.container
    (div :.row.justify-content-between.m-4
      (h2 :.detail-card__header text)
      (big-price-tag cost budget "$"))
    (p (interpose (br) (str/split-lines details)))
    (arg/ui-argumentation (comp/computed argumentation {:argumentation-root this}))))

(def ui-proposal-detail (comp/factory ProposalDetails {:keyfn :proposal/id}))

(>defn split-details [details]
  [string? => string?]
  (when (string? details)
    (some-> details (str/split #"\n\s*\n" 2) first)))

(defn proposal-card [comp {:proposal/keys [id details cost]
                           :argument/keys [text]}]
  (div :.proposal
    (div :.proposal-buttons
      (button :.btn.btn-outline-success
        (IoIosCheckmarkCircleOutline #js {:size "3rem"}))
      (div :.spacer)
      (button :.btn.btn-outline-danger
        (IoIosCloseCircleOutline #js {:size "3rem"})))

    (div :.proposal-content.btn-light
      {:style        {:cursor "pointer"}
       :data-toggle  "modal"
       :data-target  (str "#modal-" id)
       :onMouseEnter #(df/load-field! comp :>/proposal-details {})}
      (div
        {:style {:display        "flex"
                 :padding        "10px 5px 10px 10px"
                 :justifyContent "space-between"}}
        (h4 :.proposal-title text)
        (button :.btn
          {:style {:position "absolute"
                   :right    "0px"
                   :top      "0px"
                   :padding  "0.2rem 0"}}
          (IoMdMore #js {:size "24px"})))
      (div
        {:style {:display        "flex"
                 :padding        "10px"
                 :justifyContent "space-between"}}
        (div :.proposal-details (split-details details))
        (div :.proposal-price
          (span :.proposal-price__text (str cost) " €"))))))

(defsc ProposalCard [this {:keys          [>/proposal-details]
                           :proposal/keys [id] :as props}]
  {:query         [:proposal/id :argument/text :proposal/details :proposal/cost
                   {:>/proposal-details (comp/get-query ProposalDetails)}]
   :ident         :proposal/id
   :initial-state (fn [_] {:ui/modal-open? false})}
  [(proposal-card this props)
   (div :.modal.fade.bottom-sheet
     {:id (str "modal-" id)}
     (div :.spacer-frame)
     (div :.modal-dialog.modal-xl
       (div :.modal-content
         (div :.modal-body
           (div {:style {:width "auto"}}
             (button :.close
               {:style        {:position "absolute"
                               :top      "1rem"
                               :right    "1.8rem"}
                :data-dismiss "modal"} (IoMdClose))
             (ui-proposal-detail proposal-details))))))])

(def ui-proposal-card (comp/factory ProposalCard {:keyfn :proposal/id}))

(defn field [{:keys [label valid? error-message] :as props}]
  (let [input-props (-> props (assoc :name label) (dissoc :label :valid? :error-message))]
    (div :.form-group.field
      (label {:htmlFor label} label)
      (input input-props)
      (div :.ui.error.message {:classes [(when valid? "hidden")]}
        error-message))))

(defn- with-placeholder [value placeholder]
  (if (str/blank? value)
    placeholder
    value))

(defn form-dummy-proposal-card [{:proposal/keys [details cost]
                                 :argument/keys [text]}]
  (div :.proposal.mx-auto
    (div :.proposal-buttons
      (button :.btn.btn-outline-success
        {:disabled true}
        (IoIosCheckmarkCircleOutline #js {:size "3rem"}))
      (div :.spacer)
      (button :.btn.btn-outline-danger
        {:disabled true}
        (IoIosCloseCircleOutline #js {:size "3rem"})))

    (div :.proposal-content
      (div
        {:style {:display        "flex"
                 :padding        "10px 5px 10px 10px"
                 :justifyContent "space-between"}}
        (h4 :.proposal-title text))
      (div
        {:style {:display        "flex"
                 :padding        "10px"
                 :justifyContent "space-between"}}
        (small :.proposal-details details)
        (div :.proposal-price
          (span :.proposal-price__text (str cost) " €"))))))

(defmutation new-proposal [params]
  (action [{:keys [state]}]
    (swap! state mrg/merge-component ProposalCard params :append [:all-proposals]))
  (remote [env]
    (m/returning env ProposalCard)))

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

        short-summary       (split-details summary)]
    (div
      (form-dummy-proposal-card {:argument/text    (with-placeholder title "Es sollte ein Wasserspender im Flur aufgestellt werden.")
                                 :proposal/cost    (with-placeholder cost "0")
                                 :proposal/details (with-placeholder short-summary "Ein Wasserspender sorgt dafür, dass alle Studenten und Mitarbeiter mehr trinken. Dies sorgt für ein gesünderes Leben.")})
      (form :.p-5
        {:onSubmit (fn [e]
                     (evt/prevent-default! e)
                     (comp/transact! this [(new-proposal {:proposal/id      (tempid/tempid)
                                                          :proposal/cost    cost
                                                          :proposal/details summary
                                                          :argument/text    title})]))}
        (let [approaching-limit? (> (count title) title-warn-length)
              chars-exceeded?    (> (count title) title-max-length)]
          (div :.form-group
            (label "Vorschlag")
            (input :.form-control
              {:placeholder "Es sollte ein Wasserspender im Flur aufgestellt werden."
               :value       title
               :required    true
               :onChange    #(m/set-string! this :title :event %)})
            (small :.form-text
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
            (textarea :.form-control
              {:placeholder "Ein Wasserspender sorgt dafür, dass alle Studenten und Mitarbeiter mehr trinken. Dies sorgt für ein gesünderes Leben."
               :onChange    #(m/set-string! this :summary :event %)
               :value       summary})
            (small :.form-text
              {:style   {:display (when-not approaching-limit? "none")}
               :classes [(when chars-exceeded? "text-danger")]}
              (str (count title) "/" title-max-length " Buchstaben")
              (when chars-exceeded? ". Bitte beschränken Sie sich auf das Limit!"))))

        (button :.btn.btn-primary "Submit")))))

(def ui-new-proposal-form (comp/factory EnterProposal))

(defsc ProposalCollection [this {:keys [all-proposals new-proposal-form]}]
  {:query         [{[:all-proposals '_] (comp/get-query ProposalCard)}
                   {:new-proposal-form (comp/get-query EnterProposal)}]
   :initial-state (fn [_] {:new-proposal-form (comp/initial-state EnterProposal nil)})
   :ident         (fn [] [:component/id :proposals])
   :route-segment ["proposals"]
   :will-enter    (fn [app _]
                    (dr/route-deferred [:component/id :proposals]
                      #(df/load! app :all-proposals ProposalCard
                         {:without              #{:>/proposal-details}
                          :post-mutation        `dr/target-ready
                          :post-mutation-params {:target [:component/id :proposals]}})))
   :css           [[:.proposal-deck {:display         "flex"
                                     :align-items     "center"
                                     :justify-content "space-evenly"}]]}
  (let [{:keys [proposal-deck]} (css/get-classnames ProposalCollection)]
    (div :.container
      (button :.btn.btn-outline-primary
        {:data-toggle "collapse"
         :data-target "#new-proposal"}
        "Neuen Vorschlag hinzufügen")
      (div :.collapse.p-3.border#new-proposal
        (ui-new-proposal-form new-proposal-form))
      (div
        {:classes [proposal-deck]}
        (for [proposal all-proposals]
          (ui-proposal-card proposal))))))