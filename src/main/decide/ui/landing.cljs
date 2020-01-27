(ns decide.ui.landing
  (:require [cljs-time.core :as time]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom :refer [div figure p h4]]
            [cljs-time.format :as tf]
            [cljs-time.coerce :as tc]
            ["react-icons/md" :refer [MdLightbulbOutline]]
            ["react-icons/io" :refer [IoMdCheckmarkCircleOutline]]
            ["react-icons/fa" :refer [FaVoteYea]]
            [taoensso.timbre :as log]))

(defsc Phase [_ _]
  {:query [:phase/id :phase/allowed :phase/starts]
   :ident :phase/id})

(defsc CoolPanel [_ props]
  {:query         [{[:phase/id :proposal] [:phase/starts]}
                   {[:phase/id :moderation] [:phase/starts]}
                   {[:phase/id :vote] [:phase/starts]}
                   {[:phase/id :over] [:phase/starts]}
                   {[:current-phase '_] (comp/get-query Phase)}]
   :ident         (fn [] [:component/id :info-panel])
   :initial-state {}}
  (log/info props)
  (let [{:phase/keys [id]} (get props :current-phase)
        now              (time/now)
        proposal-start   (tc/to-date-time (get-in props [[:phase/id :proposal] :phase/starts]))
        moderation-start (tc/to-date-time (get-in props [[:phase/id :moderation] :phase/starts]))
        vote-start       (tc/to-date-time (get-in props [[:phase/id :vote] :phase/starts]))
        over             (tc/to-date-time (get-in props [[:phase/id :over] :phase/starts]))]
    (div
      (div :.row
        (div :.col-sm-4.p-1
          (div :.card.bg-success.text-white.text-center.h-100.shadow
            {:classes [(when (= :proposal id) :.card-highlight)]}
            (figure :.card-img-top.pt-4 (MdLightbulbOutline #js {:size "4em"}))
            (div :.card-body
              (h4 :.card-title "Vorschläge")
              (p :.card-text "Die Vorschläge kommen von Ihnen!"))
            (when proposal-start
              (div :.card-footer
                (cond
                  (time/after? now moderation-start) "Vorbei"
                  (time/after? now proposal-start) (dom/b "Aktuell!")
                  :else (tf/unparse {:format-str "dd.MM.yyyy HH:mm"} proposal-start))))))

        (div :.col-sm-4.p-1
          (div :.card.bg-warning.text-dark.text-center.h-100
            {:classes [(when (= :moderation id) :.card-highlight)]}
            (figure :.card-img-top.pt-4 (IoMdCheckmarkCircleOutline #js {:size "4em"}))
            (div :.card-body
              (h4 :.card-title "Moderation")
              (p :.card-text "Die eingereichten Vorschläge werden auf Ihre Umsetzbarkeit überprüft."))
            (when moderation-start
              (div :.card-footer
                (cond
                  (time/after? now vote-start) "Vorbei"
                  (time/after? now moderation-start) (dom/b "Aktuell!")
                  :else (tf/unparse {:format-str "dd.MM.yyyy HH:mm"} proposal-start))))))

        (div :.col-sm-4.p-1
          (div :.card.bg-danger.text-white.text-center.h-100
            {:classes [(when (= :vote id) :.card-highlight)]}
            (figure :.card-img-top.pt-4 (FaVoteYea #js {:size "4em"}))
            (div :.card-body
              (h4 :.card-title "Abstimmung")
              (p :.card-text "Am Ende können " (dom/em "Sie") " abstimmen und damit Einfluss nehmen!"))
            (when vote-start
              (div :.card-footer
                (cond
                  (time/after? now over) "Vorbei"
                  (time/after? now vote-start) "Aktuell!"
                  :else (tf/unparse {:format-str "dd.MM.yyyy HH:mm"} proposal-start)))))))
      (div :.row
        (div :.col-sm-12.p-1
          (div :.card.bg-primary.text-white.text-center
            (h4 :.card-title.pt-2 "Ergebnis")))))))

(def ui-coolpanel (comp/factory CoolPanel))