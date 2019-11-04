(ns decide.model.proposal
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [decide.model.argument :as arg]
            ["react-icons/io" :refer [IoMdMore IoIosCheckmarkCircleOutline IoIosCloseCircleOutline IoMdClose]]))

(defn big-price-tag
  ([price budget]
   (big-price-tag price budget ""))
  ([price budget unit]
   (div :.price-tag-big
     (div (str price " " unit))
     (div "von " budget " " unit))))


(defsc ProposalDetails [this {:keys [argument/text]
                              :proposal/keys [subtext cost argumentation]}]
  {:query         [:proposal/id
                   :argument/text
                   :proposal/subtext
                   :proposal/cost
                   {:proposal/argumentation (comp/get-query arg/Argumentation)}]
   :ident         :proposal/id
   :initial-state (fn [proposal]
                    (merge proposal
                      {:proposal/argumentation (comp/initial-state arg/Argumentation proposal)}))}
  (div :.container-fluid.border
    {:style {:position "relative"}}
    (dom/button :.close
      {:style {:position "absolute"
               :top ".5rem"
               :right ".5rem"}} (IoMdClose))
    (div :.row.justify-content-between.m-4
      (dom/h2 :.detail-card__header text)
      (big-price-tag cost 1000000000 "$"))
    (dom/p (interpose (dom/br) (clojure.string/split-lines subtext)))
    (arg/ui-argumentation argumentation)))

(def ui-proposal-detail (comp/factory ProposalDetails {:keyfn :argument/id}))

(defsc ProposalCard [this {:proposal/keys [subtitle price]
                           :argument/keys [text]}]
  {:query [:argument/id :argument/text :proposal/subtitle :proposal/price]
   :ident :argument/id
   :initial-state (fn [_] {:ui/modal-open? false})}
  (div :.proposal
    (div :.proposal-buttons
      (dom/button :.btn.btn-outline-success
        (IoIosCheckmarkCircleOutline #js {:size "3rem"}))
      (dom/div :.spacer)
      (dom/button :.btn.btn-outline-danger
        (IoIosCloseCircleOutline #js {:size "3rem"})))

    (div :.proposal-content
      (div
        {:style {:display "flex"
                 :padding "10px 5px 10px 10px"
                 :justifyContent "space-between"}}
        (dom/h2 :.proposal-title text)
        (dom/button :.btn
          {:style {:position "absolute"
                   :right "0px"
                   :top "0px"
                   :padding "0.2rem 0"}}
          (IoMdMore #js {:size "24px"})))
      (div
        {:style {:display "flex"
                 :padding "10px"
                 :justifyContent "space-between"}}
        (div :.proposal-subtitle subtitle)
        (div :.proposal-price
          (dom/span :.proposal-price__text (str (+ 1000000000 price) " €")))))))

(def ui-proposal-card (comp/factory ProposalCard))

(defn field [{:keys [label valid? error-message] :as props}]
  (let [input-props (-> props (assoc :name label) (dissoc :label :valid? :error-message))]
    (div :.form-group.field
      (dom/label {:htmlFor label} label)
      (dom/input input-props)
      (dom/div :.ui.error.message {:classes [(when valid? "hidden")]}
        error-message))))

(defsc EnterProposal [this props]
  {:query       [:account/email :account/password :account/password-again fs/form-config-join]
   :initial-state     (fn [_]
                        (fs/add-form-config EnterProposal
                          {:account/email          ""
                           :account/password       ""
                           :account/password-again ""}))
   :form-fields #{:account/email :account/password :account/password-again}})

