(ns decidotron.ui.mdc-components
  (:require
    ["@material/react-text-field" :as react-text-field]
    ["@material/react-button" :as react-button]
    ["@material/react-layout-grid" :as react-grid-layout]))

(defn factory-apply [class]
  (fn [props & children]
    (apply js/React.createElement
      class props children)))

(def text-field (factory-apply react-text-field/default))
(def input (factory-apply react-text-field/Input))
(def button (factory-apply react-button/default))
(def grid (factory-apply react-grid-layout/Grid))
(def row (factory-apply react-grid-layout/Row))
(def cell (factory-apply react-grid-layout/Cell))