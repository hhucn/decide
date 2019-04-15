(ns decidotron.ui.static-pages.privacy
  (:require-macros [fulcro.incubator.dynamic-routing :as dr :refer [defsc-route-target]])
  (:require
    [fulcro.client.dom :as dom]
    [fulcro.incubator.dynamic-routing :as dr]))

(defsc-route-target Privacy [_ _]
  {:query           []
   :ident           (fn [] [:screens/id :privacy-screen])
   :route-segment   (fn [] ["privacy"])
   :route-cancelled (fn [_])
   :will-enter      (fn [_ _] (dr/route-immediate [:screens/id :privacy-screen]))
   :will-leave      (fn [_] true)}
  [(dom/h1 :.mb-3 "Datenschutz")
   (dom/p
     "Der Schutz Ihrer Privatsphäre bei der Nutzung unseres Online-Angebots ist uns wichtig. "
     "Mit dieser Datenschutzerklärung möchten wir Sie über die von uns erhobenen, verarbeiteten und gespeicherten personenbezogenen Daten auf dieser Plattform informieren. ")

   (dom/h5 "Verantwortlicher für die Datenerhebung, Datenverarbeitung und den Datenschutz:")
   (dom/address
     (dom/strong :.name "Björn Ebbinghaus")
     " (" (dom/a {:href "mailto:bjoern.ebbinghaus@uni-duesseldorf.de"} "bjoern.ebbinghaus@uni-duesseldorf.de") ")" (dom/br)
     "Gebäude: 25.12" (dom/br)
     "Etage/Raum: 02.45" (dom/br)
     "Universitätsstraße 1" (dom/br)
     "40225 Düsseldorf" (dom/br))

   (dom/h5 "Datenschutzbeauftragter der Heinrich-Heine-Universität:")
   (dom/address
     "Datenschutzbeauftragter und Leiter der Stabsstelle Datenschutz " (dom/br)
     (dom/strong :.name "Kurt Finkbeiner") (dom/br)
     "Universitätsstr. 1" (dom/br)
     "Gebäude: 16.11" (dom/br)
     "Etage/Raum: 01.81" (dom/br)
     "40225 Düsseldorf" (dom/br)
     "Tel.: +49 211 81-13214" (dom/br)
     "Fax: +49 211 81-10549" (dom/br))

   (dom/h2 "Personenbezogene Daten")
   (dom/p
     "Personenbezogene Daten sind Informationen, die Ihrer Person zugeordnet werden können, wie beispielsweise Ihre E-Mail-Adresse. "
     "Personenbezogene Daten dürfen von uns nur erhoben werden, wenn sie für die Inanspruchnahme von Funktionen oder Services unseres Angebotes erforderlich sind und sofern Ihre Einwilligung vorliegt oder es uns eine gesetzliche Vorschrift erlaubt. "
     "Neben den Zugriffsdaten (IP-Adresse; Zugriffszeitpunkte; genutzter Browser; Webseite, von der der Zugang erfolgt; Name des Internet-Zugangs / -Providers) fallen personenbezogene Daten auf diesem Online-Angebot erst an, wenn Sie eine Stimme abgeben. "
     "Diese Stimme kann ihrem Account bei D-BAS (" (dom/a {:href "https://dbas.cs.uni-duesseldorf.de"} "https://dbas.cs.uni-duesseldorf.de") ") zugewiesen werden. "
     "Beim Entfernen Ihrer Stimme, wird diese von uns dauerhaft gelöscht, nicht jedoch die Information, dass sie einmal eine Stimme abgegeben haben. ")
   (dom/p
     "Die von Ihnen übertragenen Daten werden von uns anonymisiert im Rahmen von wissenschaftlichen Begleitforschungen weiterverwendet und eventuell veröffentlicht. ")

   (dom/h2 "Wissenschaftliche Begleitforschung")
   (dom/p
     "Dieses Angebot wird im Rahmen einer Masterarbeit wissenschaftlich begleitet. "
     "Dies schließt unter anderem eine Analyse der übertragenen Daten sowie eine Befragung der Teilnehmenden ein. "
     "Alle Auswertungen werden vollständig anonym vorgenommen. "
     "Eine Veröffentlichung der Ergebnisse findet ausschließlich in anonymisierter Form statt, die keine Rückschlüsse auf individuelle Teilnehmenden des Verfahrens zulässt. ")

   (dom/h2 "Nutzungsdaten")
   (dom/p
     "Grundsätzlich können Sie dieses Online-Angebot besuchen, ohne Ihre Identität offen zu legen. "
     "Zum Schutz unserer Datenverarbeitungsanlagen vor Angriffen und zur Sicherstellung des reibungslosen Betriebs protokollieren wir den Zugriff auf unserem Online-Angebot im laufenden Betrieb für eine begrenzte Dauer. "
     "Diese Server-Zugriffsdaten dienen ausschließlich dem Zweck der Missbrauchsanalyse. "
     "Sie werden getrennt von den auf dem Online-Angebot selbst erhobenen Daten gespeichert und werden weder von uns noch von weiteren Dienstleistern für andere Zwecke genutzt. "
     "Wir nehmen ausschließlich Einblick in diese Protokolldateien, wenn technische Probleme vorliegen und die dort gespeicherten Daten zu deren Behebung beitragen könnten. ")

   (dom/h2 "Cookies")
   (dom/p
     "Dieses Online-Angebot verwendet Cookies. "
     "Bei Cookies handelt es sich um kleine Textdateien, die lokal im Zwischenspeicher Ihres Internetbrowsers gespeichert werden. ")

   (dom/p
     "Grundsätzlich können Sie die Verwendung von Cookies auf diesem Online-Angebot in Ihrem Internetbrowser vollständig ausschalten, in dem Fall müssen Sie sich jedes mal erneut einloggen, wenn Sie Ihre Stimme abgeben oder ändern wollen. ")

   (dom/h2 "Datenlöschung, Widerruf der Einwilligung und Auskunftsrecht")
   (dom/p
     "Alle personenbezogenen Daten werden ausschließlich zu den oben beschriebenen Zwecken erhoben und genutzt. "
     "Sie haben als Nutzerin oder Nutzer jederzeit das Recht, Ihre Einwilligung zur Verarbeitung personenbezogener Daten zu widerrufen. "
     "Sie haben außerdem das Recht, unentgeltlich Auskunft über die über Sie gespeicherten personenbezogenen Daten zu erhalten. "
     "Zusätzlich haben Sie nach Maßgabe der gesetzlichen Bestimmungen das Recht auf Berichtigung oder Löschung Ihrer personenbezogenen Daten. "
     "Dafür wenden Sie sich bitte an unseren " (dom/a {:href "/contact"} "Kontakt") ". ")
   (dom/p
     "Sollten Sie mit uns per E-Mail in Kontakt treten wollen, weisen wir Sie darauf hin, dass der Inhalt unverschlüsselter E Mails von Dritten eingesehen werden kann. "
     "Wir empfehlen daher, vertrauliche Informationen verschlüsselt oder über den Postweg zuzusenden.")])

