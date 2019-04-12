(ns decidotron.ui.utils
  (:require [fulcro.client.primitives :as prim]
            [fulcro.client.dom :as dom]
            [goog.string :refer [format]]
            [cljs-time.core :as t]
            [cljs-time.coerce :as tc]
            [cljs-time.format :as tf]))

(defn logged-in?
  "Returns true if the"
  [this]
  (= (prim/shared this [:dbas/connection :dbas.client/login-status])
    :dbas.client/logged-in))

(defn format-cost [cost]
  (format "%.0f €" cost))

(defn price-tag [cost]
  (dom/span :.price.text-muted (format-cost cost)))

(defn close-button [on-click-fn]
  (dom/button :.close {:type       "button"
                       :aria-label "Close"
                       :onClick    on-click-fn}
    (dom/span {:aria-hidden true} (str "×"))))

(defn refresh-button [on-click-fn]
  (dom/button :.btn.btn-sm.btn-link
    {:onClick on-click-fn}
    (dom/i :.fas.fa-redo) " Aktualisieren"))

(defn format-votes-date [votes-date]
  (tf/unparse (tf/formatter "dd.MM.yyyy' um 'HH:mm' Uhr'") (t/to-default-time-zone (tc/from-date votes-date))))