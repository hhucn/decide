(ns user
  (:require
    [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs]]
    [expound.alpha :as expound]
    [clojure.spec.alpha :as s]
    [mount.core :as mount]
    ;; this is the top-level dependent component...mount will find the rest via ns requires
    [decide.server-components.http-server :refer [http-server]]
    [decide.server-components.ldap :as ldap]))

;; ==================== SERVER ====================
(set-refresh-dirs "src/main" "src/dev" "src/test")
;; Change the default output of spec to be more readable
(alter-var-root #'s/*explain-out* (constantly expound/printer))

(defn start
  "Start the web server"
  [] (mount/start-with {#'ldap/ldap-resolve
                        (fn ldap-resolver [uid password]
                          (when (= uid password)
                            {:givenName      (str "Firstname:" uid)
                             :sn             "Example"
                             :sAMAccountName uid
                             :mail           (str uid "@example.org")}))}))

(defn stop
  "Stop the web server"
  [] (mount/stop))

(defn restart
  "Stop, reload code, and restart the server. If there is a compile error, use:

  ```
  (tools-ns/refresh)
  ```

  to recompile, and then use `start` once things are good."
  []
  (stop)
  (tools-ns/refresh :after 'user/start))

