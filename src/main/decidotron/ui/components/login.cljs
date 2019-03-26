(ns decidotron.ui.components.login
  (:require [clojure.string :refer [blank?]]
            [fulcro.client.dom :as dom]
            [fulcro.client.primitives :as prim :refer [defsc]]
            [decidotron.cookies :as cookie]
            [decidotron.ui.routing :as routing]
            [fulcro.ui.form-state :as fs]
            [fulcro.client.mutations :as m :refer [defmutation]]
            [fulcro.client.data-fetch :as df]
            [decidotron.ui.models :as models]))


(defsc InputField [_ {:keys [input/value ui/label ui/type]} {:keys [onChange-fn]}]
  {:query         [:input/value :ui/label :ui/type]
   :initial-state {:input/value ""}}
  (dom/div :.form-group
    (dom/label label)
    (dom/input :.form-control
      {:type        type
       :value       value
       :placeholder label
       :aria-label  label
       :onChange    onChange-fn
       :required    true})))

(def ui-input-field (prim/factory InputField))

(defmutation post-login [{:keys [where]}]
  (action [{:keys [state component]}]
    (let [{:dbas.client/keys [login-status token]} (:dbas/connection @state)]
      (when (= :dbas.client/logged-in login-status)
        (cookie/set! cookie/decidotron-token token)
        (swap! state assoc-in [:login-form/id :singleton :login-form/password] "")
        (routing/change-route! component where)))))

(defn alert [type & children]
  {:pre [[(contains? #{:danger :success :info :warning} type)]]}
  (apply dom/div :.alert
    {:role      :alert
     :className (str "alert-" (name type))}
    children))

(defsc LoginForm
  [this {:keys [login-form/nickname login-form/password]}]
  {:query         [:login-form/nickname :login-form/password fs/form-config-join]
   :form-fields   #{:login-form/nickname :login-form/password}
   :ident         (fn [] [:login-form/id :singleton])
   :initial-state {:login-form/nickname "" :login-form/password ""}}
  (let [connection    (prim/shared this [:dbas/connection])
        not-complete? (or (blank? nickname) (blank? password))]
    (dom/div :.login-form.mt-3
      (case (:dbas.client/login-status connection)
        :dbas.client/failed (alert :bla "Login fehlgeschlagen")
        :dbas.client/logged-in (alert :success "Login erfolgreich")
        nil)
      (dom/form
        {:onSubmit (fn login [e]
                     (.preventDefault e)
                     (df/load this :dbas/connection models/Connection
                       {:remote               :dbas
                        :params               {:nickname   nickname
                                               :password   password
                                               :connection connection}
                        :post-mutation        `post-login
                        :post-mutation-params {:where ["preferences" routing/hardcoded-slug]}}))}
        (ui-input-field (prim/computed {:input/value nickname
                                        :ui/label    "Uni-Kennung"
                                        :ui/type     "text"}
                          {:onChange-fn (fn [e] (m/set-string! this :login-form/nickname :event e))}))
        (ui-input-field (prim/computed {:input/value password
                                        :ui/label    "Passwort"
                                        :ui/type     "password"}
                          {:onChange-fn (fn [e] (m/set-string! this :login-form/password :event e))}))
        (dom/button :.btn.btn-primary
          {:type     "submit"
           :disabled not-complete?}
          "Login")))))

(def ui-login-form (prim/factory LoginForm))