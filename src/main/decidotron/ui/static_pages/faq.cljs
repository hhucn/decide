(ns decidotron.ui.static-pages.faq
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target]])
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.incubator.dynamic-routing :as dr]))

(defn faq-entry [question answer]
  (dom/li :.list-group-item
    (dom/p (dom/strong question))
    (dom/p answer)))

(def faq [(faq-entry "Wofür darf das Geld verwendet werden."
            "Für alles, was die Lehre in der Informatik verbessert.")

          (faq-entry "Wird das Geld wirklich ausgegeben?"
            "Ja, solange das Geld wirklich für die Vorschläge ausgegeben werden darf.")

          (faq-entry "Wer darf teilnehmen?"
            "Jeder eingeschriebene Studierende der Informatik.")])


(defsc-route-target FAQ [_ _]
  {:query           []
   :ident           (fn [] [:screens/id :faq-screen])
   :route-segment   (fn [] ["faq"])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :faq-screen]))
   :will-leave      (fn [_] true)}
  [(dom/h1 "Frequently Asked Questions")
   (dom/ul :.list-group faq)])
