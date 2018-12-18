(ns decidotron.ui.mdc-components
  (:require
    ["@material/react-text-field" :as react-text-field]
    ["@material/react-button" :as react-button]
    ["@material/react-layout-grid" :as react-grid-layout]
    ["@material/react-list" :as react-list]
    ["@material/react-radio" :as react-radio]
    ["@material/react-material-icon" :as react-icon]))

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

(def mdc-list (factory-apply react-list/default))
(def list-item (factory-apply react-list/ListItem))
(def list-item-text (factory-apply react-list/ListItemText))
(def list-item-graphic (factory-apply react-list/ListItemGraphic))
(def list-item-meta (factory-apply react-list/ListItemMeta))

(def radio (factory-apply react-radio/default))
(def native-radio (factory-apply react-radio/NativeRadioControl))

(def icon (factory-apply react-icon/default))