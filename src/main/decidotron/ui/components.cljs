(ns decidotron.ui.components
  (:require
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.client.dom :as dom]
    [fulcro.client.mutations :refer [defmutation]]
    [fulcro.client.data-fetch :as df]
    [decidotron.ui.models :as models]
    [decidotron.ui.utils :refer [logged-in? format-cost price-tag refresh-button format-votes-date]]
    [decidotron.ui.components.results :refer [ResultList ui-result-list]]
    [decidotron.ui.components.preferences :refer [PreferenceList PreferredItem preferred-positions ui-pref-list ui-pref-list-item expand-button]]
    [goog.string :refer [format]]
    [fulcro.incubator.dynamic-routing :as dr]
    [cljs-time.core :as t]
    [cljs-time.coerce :as tc])

  (:require-macros [fulcro.incubator.dynamic-routing :refer [defsc-route-target defrouter]]))

(defsc VoteHeader [_ {:dbas.issue/keys [title votes-end]}]
  (dom/div :.vote-header.mb-5
    (dom/h1 title)
    (dom/p
      "Es können für 20.000 € Vorschläge umgesetzt werden.
      Geben Sie dafür hier Ihre Präferenzen an.")
    (dom/p
      "Sie können die Vorschläge sortieren, wobei Ihr Favorit das meiste Gewicht bei der Abstimmung hat, Ihr
      zweit liebster Vorschlag etwas weniger usw. Vorschläge, die Sie nicht unterstützen wollen, wählen Sie einfach nicht
      aus und lassen sie im Bereich \"Weitere Vorschläge\".")
    (dom/p
      "Eine genaue Erklärung, wie das Ergebnis gefunden wird, finden Sie auf " (dom/a {:href "/algorithm"} "dieser Seite") ".")
    (when votes-end
      (dom/p "Die Stimmabgabe ist möglich bis zum " (dom/strong (format-votes-date votes-end)) ". Danach werden die Ergebnisse hier angezeigt."))))

(def ui-vote-header (prim/factory VoteHeader))

(defn result-area [slug result-list]
  (dom/div :.result-area
    (dom/hr :.my-5)
    (dom/h3 "Ergebnisse " (dom/a :.btn.btn-sm.btn-link {:href (str slug "/result")} "Details"))
    (ui-result-list result-list)))

(defsc-route-target PreferenceScreen [this {:preferences/keys [slug list result-list]
                                            :dbas/keys        [issue]}]
  {:query           [:preferences/slug
                     {:preferences/list (prim/get-query PreferenceList)}
                     {:dbas/issue (prim/get-query models/Issue)}
                     {:preferences/result-list (prim/get-query ResultList)}]
   :ident           [:preferences/slug :preferences/slug]
   :route-segment   (fn [] ["preferences" :preferences/slug])
   :route-cancelled (fn [_])
   :will-enter      (fn [reconciler {:keys [preferences/slug]}]
                      (dr/route-deferred [:preferences/slug slug]
                        #(df/load reconciler [:preferences/slug slug] PreferenceScreen
                           {:post-mutation        `dr/target-ready
                            :post-mutation-params {:target [:preferences/slug slug]}})))
   :will-leave      (fn [_] true)}
  (let [{:dbas.issue/keys [budget votes-start votes-end]} issue
        supported-issue? budget
        voting-started?  (or (not votes-start) (t/after? (t/now) (tc/from-date votes-start)))
        voting-ended?    (and votes-end (t/after? (t/now) (tc/from-date votes-end)))
        show-results?    (:result/show? result-list)]
    (if supported-issue?
      (dom/div :.preference-screen
        (ui-vote-header issue)
        (if voting-started?
          (when (not voting-ended?)
            (dom/div
              (dom/div :.voting-area
                (ui-pref-list list))
              (dom/p :.small.text-muted (str (:result/no-of-participants issue) " Teilnehmer haben bisher abgestimmt. "))))

          (dom/p :.alert.alert-info
            (format "Die Stimmabgabe ist möglich ab dem %s. Sie werden darüber informiert!" (format-votes-date votes-start))))

        ; show results, if voting has begun and ended (or no end is defined) and told so by the backend.
        (when (and voting-started? show-results? (or voting-ended? (nil? voting-ended?)))
          (result-area slug (prim/computed result-list {:dbas-argument-link (format "%s/discuss/%s" js/dbas-host slug)})))

        (refresh-button #(df/load this [:preferences/slug slug] PreferenceScreen)))
      (dom/div
        (dom/p :.alert.alert-danger "Für dieses Thema wird keine Entscheidung getroffen!")))))
