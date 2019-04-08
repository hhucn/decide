(ns decidotron.ui.root
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target defrouter]])
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.client.primitives :as prim :refer [defsc]]
    [dbas.client :as dbas]
    [decidotron.ui.components :as comp]
    [decidotron.ui.components.login :as login]
    [decidotron.api :as ms]
    [fulcro.incubator.dynamic-routing :as dr]
    [decidotron.ui.routing :as routing]
    [decidotron.ui.static-pages.faq :refer [FAQ]]
    [decidotron.ui.static-pages.contact :refer [Contact]]))

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
    (dom/p :.lead "Log dich bitte mit deiner Uni Kennung ein.")
    (login/ui-login-form login-form)))

(defn main-page [this]
  (dom/div
    (dom/h1 "Verteilung von Qualitätsverbesserungsmitteln in der Informatik")
    (dom/p :.lead
      "Die WE Informatik möchte Ihnen die Möglichkeit geben selbst über
     Qualitätsverbesserungsmittel und deren Verwendung abzustimmen. Daher
     haben Sie hier die Möglichkeit Vorschläge einzubringen, die von der
     Studierendenschaft der Informatik später zur Abstimmung freigegeben
     werden.")

    (dom/p
      "Wichtig dabei ist, dass die Vorschläge alle der Qualitätsverbesserung
     dienen, denn die dafür aufgebrachten Mittel müssen zweckgebunden verwendet
     werden.")

    (dom/p
      "Diese Diskussion ist Teil einer wissenschaftlichen Studie. Wir möchten
     hierbei unter Anderem unsere Softwaresysteme testen. Es handelt sich hierbei
     um zwei verschiedene Systeme: D-BAS zum Diskutieren und decide um
     anschließend die Vorschläge zu Priorisieren. Wir beginnen zunächst mit der
     Diskussion und schalten in der zweiten Woche die Priorisierung frei.")

    (dom/br)
    (dom/p :.text-center
      (dom/a :.btn.btn-primary {:href (str js/dbas_host "/discuss/" routing/hardcoded-slug)}
       "Hier geht's zur Diskussion!"))))

(defsc-route-target MainPage [this _]
  {:query           []
   :ident           (fn [] [:screens/id :main-screen])
   :route-segment   (fn [] [""])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :main-screen]))
   :will-leave      (fn [_] true)}
  (main-page this))

(defrouter RootRouter [this {:keys [current-state]}]
  {:router-targets [MainPage LoginScreen comp/PreferenceScreen FAQ Contact]}
  (case current-state
    :initial (main-page this)
    :pending (dom/div "Loading...")
    :failed (dom/div "Oops" (dom/a {:onClick #(js/location.reload) :target "_self"} "Neu laden!"))
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

(defn footer-link [label href]
  (dom/li :.nav-item (dom/a :.btn.btn-sm.btn-light {:href href} label)))

(defsc Root [this {:keys [dbas/connection root/router]}]
  {:query         [:dbas/connection
                   {:root/router (prim/get-query RootRouter)}]
   :initial-state (fn [_]
                    {:root/router     (prim/get-initial-state RootRouter {})
                     :dbas/connection (dbas/new-connection (str js/dbas_host "/api"))})}
  (dom/div :.root.container.mdc-card.mdc-card__root
    (dom/nav :.navbar.navbar-light.bg-light
      (dom/a :.navbar-brand.d-flex.align-items-center
        {:href    "#"
         :onClick #(routing/change-route! this was-sollen-wir-mit-20-000eur-anfangen)}
        (dom/img :.mr-2 {:src "/dbas_logo_round.svg" :style {:height "2rem"}})
        "decide")
      (dom/ul :.nav.mr-auto
        (dom/li :.nav-item (dom/a :.btn.btn-sm.btn-light {:href "/"} "Home"))
        (dom/li :.nav-item (dom/a :.btn.btn-sm.btn-light {:href (str js/dbas_host "/discuss/" routing/hardcoded-slug)} "Diskussion"))
        (dom/li :.nav-item (dom/a :.btn.btn-sm.btn-light {:href (str "/preferences/" routing/hardcoded-slug)} "Abstimmung")))
      (ui-login-button this (dbas.client/logged-in? connection)))
    (dom/div :.container.pt-2
      (ui-router router))
    (dom/hr :.row)
    (dom/nav :.footer
      (dom/ul :.nav.nav-fill.nav-pills
        (footer-link "FAQ" "/faq")
        (footer-link "Kontakt" "/contact")
        (dom/li :.nav-item (dom/a :.btn.btn-sm.btn-light.disabled "Datenschutz") #_(footer-link "Datenschutz" "/privacy"))
        (footer-link "D-BAS" (str js/dbas_host "/discuss/" routing/hardcoded-slug)))))) ; TODO Keep this only for the experiment
