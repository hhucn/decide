(ns decidotron.ui.static-pages.algorithm
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target]])
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.incubator.dynamic-routing :as dr]
    [decidotron.ui.routing :refer [hardcoded-slug]]))

(defsc-route-target Algorithm [_ _]
  {:query           []
   :ident           (fn [] [:screens/id :algorithm-screen])
   :route-segment   (fn [] ["algorithm"])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :algorithm-screen]))
   :will-leave      (fn [_] true)}
  (dom/div :#doc
    (dom/h1 "Erklärung des Ablaufes")
    (dom/h2 "Phase 1: Vorschläge sammeln")
    (dom/p "In der ersten Phase ist es möglich über unser Argumentationssystem "
      (dom/em "D-BAS")
      " eigene Vorschläge und eine grobe Preisabschätzung für den Vorschlag abzugeben. Die Vorschläge können dann ausdiskutiert werden. Dabei wird Sie D-BAS durch einen Dialog führen und Ihre Meinungen zum eigenen Vorschlag sowie zu den anderen Vorschlägen einholen. Sie können jederzeit die Meinungen und Vorschläge der anderen Teilnehmenden einsehen und gegen/für diese Meinungen argumentieren. Sollte jemand auf eine Ihrer Aussagen Bezug nehmen, so werden Sie per Mail benachrichtigt. So können Sie direkt wieder in die Diskussion mit einsteigen und Ihre Aussagen verteidigen.")

    (dom/h4 {:id "rules"} "Regeln für gute Vorschläge")
    (dom/div
      (dom/h6 {:id "Ein-Vorschlag-muss"} "Ein Vorschlag muss:")
      (dom/ul
        (dom/li "unter den 20.000 € bleiben")
        (dom/li "mindestens 100 € kosten")
        (dom/li "der Verbesserung der Lehrsituation dienen")
        (dom/li "von der Informatik umsetzbar sein"))
      (dom/p "Also bitte keine Vorschläge einbringen, welche die Infrastruktur der Universität betreffen,\n                beispielsweise:")
      (dom/ul
        (dom/li "Bauliche Maßnahmen")
        (dom/li "Verbesserung des W-LANs")
        (dom/li "Mehr Steckdosen"))
      (dom/p "Diese Vorschläge liegen nicht im Ermessensbereich der Informatik, sondern beim ZIM oder dem Dezernat 6 (Gebäudemanagement)."))
    (dom/div
      (dom/h5 {:id "Beispiel-Vorschläge"} "Beispiele für Vorschläge:")
      (dom/ul
        (dom/li "Neue Veranstaltungen einführen"
          (dom/ul
            (dom/li "Zertifizierungsangebote")
            (dom/li "extern organisierter Hackathon")
            (dom/li "Kurse")))
        (dom/li "Modernisierung des Computerlabors im EG")
        (dom/li "digitale Tafeln in den Seminarräumen")
        (dom/li "ein Wasserspender im Eingangsbereich")))

    (dom/div
      (dom/h2 {:name "phase-2"} "Phase 2: Begutachtung der Vorschläge")
      (dom/p "Nachdem Vorschläge eingegangen sind, werden diese von uns begutachtet.
              Dies geschieht damit klar wird, wovon die eingebrachten Vorschläge handeln und um Vorschläge die nicht umgesetzt werden können, weil sie gegen die oben genannten Regeln verstoßen, zu filtern.
              Um die Transparenz zu wahren, werden Veränderungen an den Vorschlägen nur in Absprache mit den Autoren geschehen, egal ob sie nur umformuliert werden oder entfernt.
              Anschließend erhalten Sie noch eine Woche Zeit, um über diese überarbeiteten Vorschläge zu diskutieren und danach eine weitere Woche, um über die Vorschläge abzustimmen."))

    (dom/h2 "Phase 3: Abstimmung")
    (dom/p "Für die Abstimmungs-Phase wechseln wir in unser Entscheidungsfindungssystem "
      (dom/em "decide") ". Schauen Sie sich dort nun die Vorschläge an und entscheiden Sie, von welchen Sie möchten, dass sie umgesetzt werden. Wenn Sie die Vorschläge nicht gut finden, dann wählen Sie diese einfach nicht aus.")
    (dom/p "Nachdem Sie eine Auswahl getätigt haben, sollten Sie die Vorschläge priorisieren. Klicken Sie die wichtigsten Vorhaben an die Spitze der Liste und die weniger wichtigen an das Ende.")
    (dom/p "Das endgültige Ergebnis an umzusetzenden Vorschlägen wird ausgewählt, indem jeder Priorität ein Wert zugeordnet wird. Neben Ihnen haben noch viele andere ihre Stimmen abgegeben und ihre Stimmen bekommt auch einen Wert. Jede erste Priorität bekommt denselben Wert, jede zweite Priorität denselben kleineren Wert, usw.")
    (dom/p "Anschließend werden die Werte aller Vorschläge summiert, um somit eine Rangfolge zu erhalten. In dem Ergebnis werden nun die am besten bewerteten Vorschläge eingebracht, solange sie mit in das restliche Budget passen.")
    (dom/p "Diese Phase dauert eine Woche. Im Anschluss finden Sie die Ergebnisse unter "
      (dom/a
        {:href   (str "https://decide.dbas.cs.uni-duesseldorf.de/preferences/" hardcoded-slug "/result",)
         :target "_blank"}
        (str "https://decide.dbas.cs.uni-duesseldorf.de/preferences/" hardcoded-slug "/result")))
    (dom/h3 "Beispiel für die Auswertung")
    (dom/p "Angenommen wir haben drei Teilnehmer Christian, Alexander und Markus. Sie wollen über drei mögliche Vorschläge entscheiden. Es stehen 10.000 € zur Verfügung. Vorgeschlagen werden ein Hackathon für 4.000 €, ein Wasserspender im Eingangsbereich für 2.000 € und die Modernisierung des Computerraums für 7.000 €. Sie geben Ihre Stimmen ab, wobei Christian nicht mit dem Computerraum einverstanden ist und Markus keinen Wasserspender will.")
    (dom/div :.table-responsive
      (dom/table :.table
        (dom/thead
          (dom/tr
            (dom/th)
            (dom/th "1. Priorität (3 P)")
            (dom/th "2. Priorität (2 P)")
            (dom/th "3. Priorität (1 P)")
            (dom/th "Abgelehnt (0 P)")))
        (dom/tbody
          (dom/tr
            (dom/th "Christian")
            (dom/td "Wasserspender")
            (dom/td "Hackathon")
            (dom/td "-")
            (dom/td "Computerraum"))
          (dom/tr
            (dom/th "Alexander")
            (dom/td "Computerraum")
            (dom/td "Hackathon")
            (dom/td "Wasserspender")
            (dom/td "-"))
          (dom/tr
            (dom/th "Markus")
            (dom/td "Hackathon")
            (dom/td "Computerraum")
            (dom/td "-")
            (dom/td "Wasserspender")))))
    (dom/p
      "Nachdem die drei abgestimmt haben, werden ihre Entscheidungen gewertet. Ihre höchsten Prioritäten bekommen dieselben Punkte (P), alle weiteren Vorschläge jeweils einen Punkt weniger wie oben beschrieben. Vorschläge, die sie nicht gut finden, bekommen keine Punkte.")
    (dom/p "Zusammengerechnet ergeben sich also folgende Punktzahlen:")
    (dom/div :.table-responsive
      (dom/table :.table
        (dom/thead
          (dom/tr (dom/th) (dom/th "Punkte") (dom/th "Kosten")))
        (dom/tbody
          (dom/tr
            (dom/th "Hackathon")
            (dom/td "7")
            (dom/td "4.000 €"))
          (dom/tr
            (dom/th "Computerraum")
            (dom/td "5")
            (dom/td "7.000 €"))
          (dom/tr
            (dom/th "Wasserspender")
            (dom/td "4")
            (dom/td "2.000 €")))))
    (dom/p
      "Nun werden die gewinnenden Vorschläge zusammengefasst. Nicht alle Vorschläge können angenommen werden, da dies das Budget sprengen würde."
      (dom/br)
      "Als Vorschlag mit den meisten Punkten wird der Hackathon ein Gewinner, denn er kostet 4.000 €, also bleiben noch 6.000 € übrig. Der Computerraum hat die zweit meisten Punkte, passt mit den veranschlagten 7.000 € aber nicht ins Budget und wird damit kein Gewinner. Obwohl der Wasserspender am wenigsten Punkte bekommen hat, passt er mit seinen 2.000 € Kosten in das restliche Budget. Er wird also auch ein Gewinner.")
    (dom/p
      "Die Gewinner sind: Der " (dom/strong "Hackathon") " und der " (dom/strong "Wasserspender") "."
      (dom/br)
      "Verloren hat das " (dom/strong "Computerraum") ".")

    (dom/h3 "Unentschieden")
    (dom/p "Angenommen es nimmt ein vierter Teilnehmer, Martin, teil.")
    (dom/div :.table-responsive
      (dom/table :.table
        (dom/thead
          (dom/tr
            (dom/th)
            (dom/th "1. Priorität (3 P)")
            (dom/th "2. Priorität (2 P)")
            (dom/th "3. Priorität (1 P)")
            (dom/th "Abgelehnt (0 P)")))
        (dom/tbody
          (dom/tr
            (dom/th "Martin")
            (dom/td "Computerraum")
            (dom/td "Wasserspender")
            (dom/td "Hackathon")
            (dom/td "-")))))
    (dom/p "Nun sehen die Punkte etwas anders aus, der Computerraum hat aufgeholt! Es besteht die verzwickte Situation, dass es ein Unentschieden gibt. Da alle dem Hackathon zustimmen und ihn somit keiner ablehnt, die anderen beiden Vorschläge allerdings nicht die Zustimmung aller bekommen haben, verliert hier der Computerraum schon wieder gegen den Hackathon.")
    (dom/table :.table
      (dom/thead
        (dom/tr
          (dom/th)
          (dom/th "Punkte")
          (dom/th "Kosten")
          (dom/th "Zustimmungen")))
      (dom/tbody
        (dom/tr
          (dom/th "Computerraum")
          (dom/td "8 (5+3)")
          (dom/td "7.000 €")
          (dom/td "3"))

        (dom/tr
          (dom/th "Hackathon")
          (dom/td "8 (7+1)")
          (dom/td "4.000 €")
          (dom/td "4"))

        (dom/tr
          (dom/th "Wasserspender")
          (dom/td "6 (4+2)")
          (dom/td "2.000 €")
          (dom/td "3"))))
    (dom/p
      "Die Gewinner werden wie bereits genannt bestimmt und wieder gewinnt der "
      (dom/strong "Hackathon")
      " und der "
      (dom/strong "Wasserspender")
      ". Der Computerraum war dicht dran, aber es hat dennoch verloren.")))