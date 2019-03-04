(ns decidotron.cookies
  (:import goog.net.Cookies)
  (:refer-clojure :exclude [get remove set]))

(def ^const decidotron-token "decidotron-token")

(defn get
  "Returns a cookie by its name.
  `nil` if there is no cookie with this name"
  [name]
  (.get (Cookies. js/document) name nil))

(defn remove!
  "Removes a cookie by its name.
  Returns `true` if there was a cookie to delete, `false` otherwise"
  [name]
  (.remove (Cookies. js/document) name "/"))

(defn set!
  [name value]
  (.set (Cookies. js/document) name value (* 30 24 60 60) "/"))