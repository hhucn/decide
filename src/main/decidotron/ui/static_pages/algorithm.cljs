(ns decidotron.ui.static-pages.algorithm
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target]])
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.incubator.dynamic-routing :as dr]))

(defsc-route-target Algorithm [_ _]
  {:query           []
   :ident           (fn [] [:screens/id :algorithm-screen])
   :route-segment   (fn [] ["algorithm"])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :algorithm-screen]))
   :will-leave      (fn [_] true)}
  (dom/div :#doc
    (dom/h1 "Erklärung des Experiment-Ablaufes")
    (dom/h2 "Vorschlags-Phase")
    (dom/p "In der ersten Phase ist es möglich über unser Argumentationssystem "
      (dom/em "D-BAS")
      " eigene Vorschläge und eine grobe Preisabschätzung für den Vorschlag abzugeben. Die Vorschläge können dann ausdiskutiert werden. Dabei wird Sie D-BAS durch einen Dialog führen und Ihre Meinungen zum eigenen Vorschlag sowie zu den anderen Vorschlägen einholen. Sie können jederzeit die Meinungen und Vorschläge der anderen Teilnehmenden einsehen und gegen/für diese Meinungen argumentieren. Sollte jemand auf eine Ihrer Aussagen Bezug nehmen, so werden Sie per Mail benachrichtigt. So können Sie direkt wieder in die Diskussion mit einsteigen und Ihre Aussagen verteidigen.")
    (dom/p "Die erste Phase dauert eine Woche. In der zweiten Woche können Sie weiter auf D-BAS diskutieren, Sie sollten sich aber bewusst sein, dass die Abstimmungs-Phase dann schon begonnen hat.")
    (dom/h2 "Abstimmungs-Phase")
    (dom/p "Für die Abstimmungsphase wechseln wir in unser Entscheidungsfindungssystem "
      (dom/em "decide") ". Schauen Sie sich dort nun die Vorschläge an und entscheiden Sie, von welchen Sie möchten, dass sie umgesetzt werden. Wenn Sie die Vorschläge nicht gut finden, dann wählen Sie diese einfach nicht aus.")
    (dom/p "Nachdem Sie eine Auswahl getätigt haben, sollten Sie die Vorschläge priorisieren. Klicken Sie die wichtigsten Vorhaben an die Spitze der Liste und die weniger wichtigen an das Ende.")
    (dom/p "Das endgültige Ergebnis an umzusetzenden Vorschlägen wird ausgewählt, indem jeder Priorität ein Wert zugeordnet wird. Neben Ihnen haben noch viele andere ihre Stimmen abgegeben und ihre Stimmen bekommt auch einen Wert. Jede erste Priorität bekommt denselben Wert, jede zweite Priorität denselben kleineren Wert, usw.")
    (dom/p "Anschließend werden die Werte aller Vorschläge miteinander addiert, um somit eine Rangfolge zu erhalten. In dem Ergebnis werden nun die am besten bewerteten Vorschläge eingebracht, solange sie mit in das restliche Budget passen.")
    (dom/p "Diese Phase dauert eine Woche und findet direkt im Anschluss an die Vorschlags-Phase statt. Im Anschluss finden Sie die Ergebnisse unter "
      (dom/a
        {:href   "https://decide.dbas.cs.uni-duesseldorf.de/preferences/verteilung-von-qualitatsverbesserungsmittel-in-der-informatik/result",
         :target "_blank"}
        "https://decide.dbas.cs.uni-duesseldorf.de/preferences/verteilung-von-qualitatsverbesserungsmittel-in-der-informatik/result"))
    (dom/h3 "Beispiel für die Auswertung")
    (dom/p "Angenommen wir haben drei Teilnehmer Christian, Alexander und Markus. Sie wollen über drei mögliche Vorschläge entscheiden. Es stehen 10.000 € zur Verfügung. Vorgeschlagen werden ein Schokobrunnen für 4.000 €, ein Wasserspender für 2.000 € und ein Bällebad für 7.000 €. Sie geben Ihre Stimmen ab, wobei Christian nicht mit dem Bällebad einverstanden ist und Markus keinen Wasserspender will.")
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
          (dom/td "Schokobrunnen")
          (dom/td "-")
          (dom/td "Bällebad"))
        (dom/tr
          (dom/th "Alexander")
          (dom/td "Bällebad")
          (dom/td "Schokobrunnen")
          (dom/td "Wasserspender")
          (dom/td "-"))
        (dom/tr
          (dom/th "Markus")
          (dom/td "Schokobrunnen")
          (dom/td "Bällebad")
          (dom/td "-")
          (dom/td "Wasserspender"))))
    (dom/p
      "Nachdem die drei abgestimmt haben, werden ihre Entscheidungen gewertet. Ihre höchsten Prioritäten bekommen dieselben Punkte (P), alle weiteren Vorschläge jeweils einen Punkt weniger wie oben beschrieben. Vorschläge, die sie nicht gut finden, bekommen keine Punkte.")
    (dom/p "Zusammengerechnet ergeben sich also folgende Punktzahlen:")
    (dom/table :.table
      (dom/thead
        (dom/tr (dom/th) (dom/th "Punkte") (dom/th "Kosten")))
      (dom/tbody
        (dom/tr
          (dom/th "Schokobrunnen")
          (dom/td "7")
          (dom/td "4.000 €"))
        (dom/tr
          (dom/th "Bällebad")
          (dom/td "5")
          (dom/td "7.000 €"))
        (dom/tr
          (dom/th "Wasserspender")
          (dom/td "4")
          (dom/td "2.000 €"))))
    (dom/p
      "Nun werden die gewinnenden Vorschläge zusammengefasst. Nicht alle Vorschläge können angenommen werden, da dies das Budget sprengen würde."
      (dom/br)
      "Als Vorschlag mit den meisten Punkten wird der Schokobrunnen ein Gewinner, denn er kostet 4.000 €, also bleiben noch 6.000 € übrig. Das Bällebad hat die zweitmeisten Punkte, passt mit den veranschlagten 7.000 € aber nicht ins Budget und wird damit kein Gewinner. Obwohl der Wasserspender am wenigsten Punkte bekommen hat, passt er mit seinen 2.000 € Kosten in das restliche Budget. Er wird also auch ein Gewinner.")
    (dom/p
      "Die Gewinner sind: Der " (dom/strong "Schokobrunnen") " und der " (dom/strong "Wasserspender") "."
      (dom/br)
      "Verloren hat das " (dom/strong "Bällebad") ".")

    (dom/h3 "Unentschieden")
    (dom/p "Angenommen es nimmt ein vierter Teilnehmer, Martin, teil.")
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
          (dom/td "Bällebad")
          (dom/td "Wasserspender")
          (dom/td "Schokobrunnen")
          (dom/td "-"))))
    (dom/p "Nun sehen die Punkte etwas anders aus, das Bällebad hat aufgeholt! Es besteht die verzwickte Situation, dass es ein Unentschieden gibt. Da alle dem Schokobrunnen zustimmen und ihn somit keiner ablehnt, die anderen beiden Vorschläge allerdings nicht die Zustimmung aller bekommen haben, verliert hier das Bällebad schon wieder gegen den Schokobrunnen.")
    (dom/table :.table
      (dom/thead
        (dom/tr
          (dom/th)
          (dom/th "Punkte")
          (dom/th "Kosten")
          (dom/th "Zustimmungen")))
      (dom/tbody
        (dom/tr
          (dom/th "Bällebad")
          (dom/td "8 (5+3)")
          (dom/td "7.000 €")
          (dom/td "3"))

        (dom/tr
          (dom/th "Schokobrunnen")
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
      (dom/strong "Schokobrunnen")
      " und der "
      (dom/strong "Wasserspender")
      ". Das Bällebad war dicht dran, aber es hat dennoch verloren.")))
