(ns decidotron.ui.components.pro-con-addon
  (:require [fulcro.client.primitives :as prim :refer [defsc]]
            [decidotron.ui.models :as models]
            [goog.string :refer [format]]
            [fulcro.client.dom :as dom]))

(defn- format-pro-con [text]
  (format "… %s." text))

(defn discuss-button [prefix id]
  (dom/a :.btn.btn-sm.btn-outline-primary
    {:href (format "%s/jump/%d" prefix id)}
    (dom/i :.far.fa-comments) " Diskutieren"))

(defsc ProConAddon [_this
                    {:dbas.position/keys [pros cons]}
                    {:keys [collapse-id dbas-argument-link]}]
  {:query [{:dbas.position/pros (prim/get-query models/Statement)}
           {:dbas.position/cons (prim/get-query models/Statement)}]}
  (let [pros (shuffle pros)
        cons (shuffle cons)]
    (dom/div :.list-group-item.collapse.pro-con-addon.pr-0 {:id collapse-id}
      (when (not-empty pros)
        (dom/div :.pro-con-addon__pros
          (dom/p
            (dom/span :.text-success "Dafür") " spricht, dass …")
          (dom/ul :.list-group.list-group-flush
            (for [{:dbas.statement/keys [text id argument-id]} pros]
              (dom/li :.list-group-item.d-flex.justify-content-between.align-items-center
                {:key (str collapse-id "-" id)}
                (format-pro-con text)
                (discuss-button dbas-argument-link argument-id))))))

      (when (not-empty cons)
        (dom/div :.pro-con-addon__cons.mt-3
          (dom/p
            (dom/span :.text-danger "Dagegen") " spricht, dass …")
          (dom/ul :.list-group.list-group-flush
            (for [{:dbas.statement/keys [id text argument-id]} cons]
              (dom/li :.list-group-item.d-flex.justify-content-between.align-items-center
                {:key (str collapse-id "-" id)}
                (format-pro-con text)
                (discuss-button dbas-argument-link argument-id)))))))))

(def ui-pro-con-addon (prim/factory ProConAddon))