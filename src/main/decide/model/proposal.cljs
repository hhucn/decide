(ns decide.model.proposal
  (:require [com.fulcrologic.fulcro.dom :as dom :refer [div]]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro-css.css :as css]
            [com.fulcrologic.fulcro.algorithms.form-state :as fs]
            [fulcro.client.mutations :as m]))



(defsc ArgumentListEntry [this props]
  {:query []})

(defsc ProposalDetails [this {:proposal/keys [title arguments]}]
  {:query [:proposal/id :proposal/title :proposal/arguments]
   :ident :proposal/id}
  (div :.container
    (dom/ol :.list-group
      (for [argument arguments]
        (dom/li :.list-item "Item 1")))))

(def accept-icon
  (dom/svg {:width "32" :height "32" :viewBox "0 0 32 32" :xmlns "http://www.w3.org/2000/svg"}
    (dom/path {:d "M16 0C7.16342 0 0 7.16342 0 16C0 24.8366 7.16342 32 16 32C24.8366 32 32 24.8366 32 16C32 7.16342 24.8366 0 16 0ZM16 3.09677C23.1311 3.09677 28.9032 8.86781 28.9032 16C28.9032 23.1311 23.1322 28.9032 16 28.9032C8.8689 28.9032 3.09677 23.1322 3.09677 16C3.09677 8.8689 8.86781 3.09677 16 3.09677ZM25.0454 11.5011L23.5915 10.0354C23.2904 9.73187 22.8002 9.72987 22.4966 10.031L13.3772 19.0772L9.51961 15.1884C9.21852 14.8848 8.72832 14.8828 8.42477 15.1839L6.95903 16.6379C6.65548 16.939 6.65348 17.4292 6.95464 17.7328L12.8115 23.637C13.1126 23.9406 13.6028 23.9426 13.9063 23.6414L25.0411 12.596C25.3446 12.2948 25.3465 11.8046 25.0454 11.5011Z"})))

(def decline-icon
  (dom/svg {:width "32" :height "33" :viewBox "0 0 32 33" :xmlns "http://www.w3.org/2000/svg"}
    (dom/path {:d "M16 0.66333C7.16129 0.66333 0 7.75372 0 16.5049C0 25.2561 7.16129 32.3465 16 32.3465C24.8387 32.3465 32 25.2561 32 16.5049C32 7.75372 24.8387 0.66333 16 0.66333ZM16 29.2804C8.87097 29.2804 3.09677 23.5634 3.09677 16.5049C3.09677 9.44647 8.87097 3.72944 16 3.72944C23.129 3.72944 28.9032 9.44647 28.9032 16.5049C28.9032 23.5634 23.129 29.2804 16 29.2804ZM22.5677 12.5317L18.5548 16.5049L22.5677 20.4781C22.871 20.7783 22.871 21.2638 22.5677 21.564L21.1097 23.0076C20.8065 23.3079 20.3161 23.3079 20.0129 23.0076L16 19.0345L11.9871 23.0076C11.6839 23.3079 11.1935 23.3079 10.8903 23.0076L9.43226 21.564C9.12903 21.2638 9.12903 20.7783 9.43226 20.4781L13.4452 16.5049L9.43226 12.5317C9.12903 12.2315 9.12903 11.7461 9.43226 11.4458L10.8903 10.0022C11.1935 9.70198 11.6839 9.70198 11.9871 10.0022L16 13.9754L20.0129 10.0022C20.3161 9.70198 20.8065 9.70198 21.1097 10.0022L22.5677 11.4458C22.871 11.7461 22.871 12.2315 22.5677 12.5317Z"})))
  

(defsc ProposalCard [this {:proposal/keys [id title subtitle price]}]
  {:query [:proposal/id :proposal/title :proposal/subtitle :proposal/price]
   :ident :proposal/id
   :initial-state (fn [_] {:ui/modal-open? false})}
  (div :.proposal
    (div :.proposal-buttons
      (dom/button :.btn.btn-success.proposal-accept
        accept-icon)
      (dom/div :.spacer)
      (dom/button :.btn.btn-danger.proposal-decline
        decline-icon))

    (div :.proposal-content
      (div
        {:style {:display "flex"
                 :padding "10px 5px 10px 10px"
                 :justify-content "space-between"}}
        (dom/h2 :.proposal-title title)
        (dom/img {:src "/assets/icons/more.svg"
                  :height "24px"
                  :style {:text-align "center"}}))
      (div
        {:style {:display "flex"
                 :padding "10px"
                 :justify-content "space-between"}}
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

