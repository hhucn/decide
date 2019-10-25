(ns decide.model.proposal
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro-css.css :as css]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [fulcro.client.mutations :as m]
            ["react-icons/io" :refer [IoMdMore IoIosCheckmarkCircleOutline IoIosCloseCircleOutline]]))

(defsc ArgumentListEntry [this props]
  {:query []})

(defsc ProposalDetails [this {:proposal/keys [title arguments]}]
  {:query [:proposal/id :proposal/title :proposal/arguments]
   :ident :proposal/id}
  (div :.container
    (dom/ol :.list-group
      (for [argument arguments]
        (dom/li :.list-item "Item 1")))))

(defsc ProposalCard [this {:proposal/keys [id title subtitle price]}]
  {:query [:proposal/id :proposal/title :proposal/subtitle :proposal/price]
   :ident :proposal/id
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
        (dom/h2 :.proposal-title title)
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

