(ns decidotron.ui.mdc-components
  (:require
    ["@material/react-text-field" :as react-text-field]
    ["@material/react-button" :as react-button]
    ["@material/react-layout-grid" :as react-grid-layout]
    ["@material/react-list" :as react-list]
    ["@material/react-radio" :as react-radio]
    ["@material/react-material-icon" :as react-icon]
    ["@material/react-top-app-bar" :as react-topbar]
    ["@material/react-icon-button" :as react-icon-button]
    ["@material/react-menu-surface" :as react-menu-surface]
    ["@material/react-drawer" :as react-drawer]))

(defn factory-apply [class]
  (fn [props & children]
    (apply js/React.createElement
      class props children)))

(def text-field (factory-apply react-text-field/default))
(def input (factory-apply react-text-field/Input))

(def button (factory-apply react-button/default))
(def icon-button (factory-apply react-icon-button/default))
(def icon-toggle (factory-apply react-icon-button/IconToggle))

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

(def top-app-bar (factory-apply react-topbar/default))
(def top-app-bar-fixed-adjust (factory-apply react-topbar/TopAppBarFixedAdjust))

(def menu-surface (factory-apply react-menu-surface/default))

(def drawer (factory-apply react-drawer/default))
(def drawer-header (factory-apply react-drawer/DrawerHeader))
(def drawer-subtitle (factory-apply react-drawer/DrawerSubtitle))
(def drawer-title (factory-apply react-drawer/DrawerTitle))
(def drawer-content (factory-apply react-drawer/DrawerContent))
(def drawer-app-content (factory-apply react-drawer/DrawerAppContent))