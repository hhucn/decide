(ns decide.util
  #?(:cljs (:refer-clojure :exclude [uuid]))
  (:require [com.fulcrologic.guardrails.core :as g :refer [>defn => | ?]]
            [clojure.spec.alpha :as s]
            [clojure.set :refer [rename-keys]])
  (:import (java.util UUID)))

(>defn str->uuid [s] [string? => uuid?] (UUID/fromString s))
(>defn str-id->uuid-id
  "Updates :argument/id in map to uuid"
  [m]
  [(comp string? :argument/id) => (comp uuid? :argument/id)]
  (update m :argument/id str->uuid))

(defn arg-id->prop-id [proposal]
  (rename-keys proposal {:argument/id :proposal/id}))

(>defn uuid
  "Generate a UUID the same way via clj/cljs.  Without args gives random UUID. With args, builds UUID based on input (which
  is useful in tests)."
  #?(:clj ([] [=> uuid?] (java.util.UUID/randomUUID)))
  #?(:clj ([int-or-str]
           [(s/or :i int? :s string?) => uuid?]
           (if (int? int-or-str)
             (java.util.UUID/fromString
               (format "ffffffff-ffff-ffff-ffff-%012d" int-or-str))
             (java.util.UUID/fromString int-or-str))))
  #?(:cljs ([] [=> uuid?] (random-uuid)))
  #?(:cljs ([& args]
            [(s/* any?) => uuid?]
            (cljs.core/uuid (apply str args)))))
