(ns decidotron.ui.root
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target defrouter]])
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [fulcro.incubator.dynamic-routing :as dr]
    [dbas.client :as dbas]
    [decidotron.ui.components :as comp]
    [decidotron.ui.components.login :as login]
    [decidotron.api :as ms]
    [decidotron.ui.routing :as routing]
    [decidotron.ui.static-pages.faq :refer [FAQ]]
    [decidotron.ui.static-pages.contact :refer [Contact]]
    [decidotron.ui.static-pages.algorithm :refer [Algorithm]]
    [decidotron.ui.static-pages.privacy :refer [Privacy]]
    [decidotron.ui.components.results :refer [ResultScreen]]
    [decidotron.ui.components.result-status :refer [ResultStatusScreen]]))

(def was-sollen-wir-mit-20-000eur-anfangen ["preferences" routing/hardcoded-slug])

(defsc-route-target LoginScreen [_this {:keys [login/login-form]}]
  {:query           [{:login/login-form (prim/get-query login/LoginForm)}]
   :ident           (fn [] [:screens/id :login-screen])
   :initial-state   (fn [_] {:login/login-form (prim/get-initial-state login/LoginForm {})})
   :route-segment   (fn [] ["login"])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :login-screen]))
   :will-leave      (fn [_] true)}
  (dom/div :.login-screen
    (dom/p :.lead "Log dich bitte mit deiner Uni-Kennung ein.")
    (login/ui-login-form login-form)))

(defn main-page [this]
  (dom/div
    (dom/h1 "Verteilung von Qualitätsverbesserungsmitteln")
    (dom/p
      "Die Wissenschaftliche Einrichtung Informatik möchte der Studierendenschaft die Möglichkeit geben, über die Verwendung von 20.000 € sogenannter Qualitätsverbesserungsmittel zu diskutieren und zu entscheiden.
      Dazu können in " (dom/a {:href (str js/dbas_host "/discuss/")} "D-BAS") " Vorschläge mit grober Preisabschätzung abgegeben werden, die dann von Ihnen diskutiert werden und über die Sie auch abstimmen können.
      Ihre Vorschläge können nur dann umgesetzt werden, wenn sie der Verbesserung der Qualität der Lehre dienen.

      Der genaue Ablauf kann " (dom/a {:href "/algorithm"} "auf dieser Seite") " nachgelesen werden. ")

    (dom/p
     "Wir werden die Diskussion und den Entscheidungsprozess wissenschaftlich
      begleiten und Sie im Anschluss an den Entscheidungsprozess zu einer (vollkommen
      freiwilligen) Teilnehmendenbefragung einladen. Die Erhebung, Verarbeitung und
      Nutzung der so gewonnenen Daten geschieht ausschließlich anonymisiert und zu
      Forschungszwecken, sie steht in keiner Verbindung mit Ihren Studienleistungen
      oder sonstigen personenbezogenen Eigenschaften.")

    (dom/br)
    (dom/p :.text-center
      (dom/a :.btn.btn-primary {:href (str js/dbas_host "/discuss/" routing/hardcoded-slug)}
        "Hier geht's zur Diskussion!")
      (dom/a :.btn.btn-secondary.m-1 {:href (str "/preferences/" routing/hardcoded-slug)}
        "Hier geht's zur Abstimmung!"))))

(defsc-route-target MainPage [this _]
  {:query           []
   :ident           (fn [] [:screens/id :main-screen])
   :route-segment   (fn [] [""])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :main-screen]))
   :will-leave      (fn [_] true)}
  (main-page this))

(defrouter RootRouter [this {:keys [current-state]}]
  {:router-targets [MainPage LoginScreen ResultScreen ResultStatusScreen comp/PreferenceScreen FAQ Contact Algorithm Privacy]}
  (case current-state
    :initial (main-page this)
    :pending (dom/div "Loading...")
    :failed (dom/div "Oops! " (dom/button :.btn.btn-primary {:onClick #(js/location.reload) :target "_self"} "Neu laden!"))
    (main-page this)))

(def ui-router (prim/factory RootRouter))

(defn ui-login-button [this logged-in?]
  (if logged-in?
    (dom/button :.btn.btn-light
      {:onClick #(do (prim/transact! this `[(ms/logout {})])
                     (routing/change-route! this ["login"]))}
      (dom/i :.fas.fa-sign-out-alt) " Logout")
    (dom/button :.btn.btn-light
      {:onClick #(routing/change-route! this ["login"])}
      (dom/i :.fas.fa-sign-in-alt) " Login")))

(defn nav-link [label href]
  (dom/li :.nav-item (dom/a :.btn.btn-light {:href href} label)))

(defsc Root [this {:keys [dbas/connection root/router]}]
  {:query         [:dbas/connection
                   {:root/router (prim/get-query RootRouter)}]
   :initial-state (fn [_]
                    {:root/router     (prim/get-initial-state RootRouter {})
                     :dbas/connection (dbas/new-connection (str js/dbas_host "/api"))})}
  (dom/div :.root.container.mdc-card.mdc-card__root.mt-sm-3
    (dom/nav :.navbar.navbar-expand-md.navbar-light.bg-light
      (dom/a :.navbar-brand.d-flex.align-items-center
        {:href "/"}
        (dom/img :.mr-2 {:src "/dbas_logo_round.svg" :style {:height "2rem"}})
        "decide")
      (dom/button
        {:type          "button",
         :data-toggle   "collapse",
         :data-target   "#navbarSupportedContent",
         :aria-controls "navbarSupportedContent",
         :aria-expanded "false",
         :aria-label    "Toggle navigation",
         :className     "navbar-toggler"}
        (dom/span {:className "navbar-toggler-icon"}))
      (dom/div :#navbarSupportedContent.collapse.navbar-collapse
        (dom/ul :.navbar-nav.mr-auto
          (nav-link "Startseite" "/")
          (nav-link "Abstimmung" (str "/preferences/" routing/hardcoded-slug))
          (dom/li :.nav-item (dom/a :.btn.btn-light {:href "/algorithm"} "Ablauf")))
        (dom/hr :.d-md-none)
        (dom/ul :.navbar-nav
          (dom/li
            (dom/a :.btn.btn-light {:href (str js/dbas_host "/discuss/" routing/hardcoded-slug)}
              "Zur Diskussion" (dom/sup (dom/i :.fas.fa-caret-up {:style {:transform "rotate(45deg)"}}))))
          (dom/li (ui-login-button this (dbas.client/logged-in? connection))))))
    (dom/div :.container.pt-2
      (ui-router router))
    (dom/hr :.row)
    (dom/nav :.footer
      (dom/ul :.nav.nav-fill.nav-pills
        (nav-link "FAQ" "/faq")
        (nav-link "Kontakt" "/contact")
        (nav-link "Datenschutz" "/privacy")
        (dom/li :.nav-item (dom/a :.btn.btn-sm.btn-light {:href (str js/dbas_host "/discuss/")} "D-BAS"))))))
