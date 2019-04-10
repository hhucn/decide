(ns decidotron.ui.static-pages.contact
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target]])
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.incubator.dynamic-routing :as dr]))

(defsc-route-target Contact [_ _]
  {:query           []
   :ident           (fn [] [:screens/id :contact-screen])
   :route-segment   (fn [] ["contact"])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :contact-screen]))
   :will-leave      (fn [_] true)}
  [(dom/h1 :.mb-3 "Kontakt")
   (dom/div :.mb-3
     (dom/h2 "decide")
     (dom/address
       (dom/strong :.name "Björn Ebbinghaus")
       " (" (dom/a {:href "mailto:bjoern.ebbinghaus@uni-duesseldorf.de"} "bjoern.ebbinghaus@uni-duesseldorf.de") ")" (dom/br)
       "Gebäude: 25.12" (dom/br)
       "Etage/Raum: 02.45" (dom/br)
       "Universitätsstraße 1" (dom/br)
       "40225 Düsseldorf" (dom/br)))
   (dom/div
     (dom/h2 "D-BAS")
     (dom/address
       (dom/a {:href "dbas@cs.uni-duesseldorf.de"} "dbas@cs.uni-duesseldorf.de")))])
