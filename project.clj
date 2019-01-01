(defproject decidotron "0.1.0-SNAPSHOT"
  :description "My Cool Project"
  :license {:name "MIT" :url "https://opensource.org/licenses/MIT"}
  :min-lein-version "2.7.0"

  :dependencies [[org.clojure/clojure "1.10.0"]
                 [thheller/shadow-cljs "2.7.9"]
                 [fulcrologic/fulcro "2.7.0"]
                 [com.wsscode/pathom "2.2.5"]
                 [ring/ring-defaults "0.3.2"]
                 [org.clojure/core.async "0.4.490"]
                 [garden "1.3.6"]
                 [mount "0.1.15"]
                 [hiccup "1.0.5"]

                 [http-kit "2.3.0"]
                 [ring/ring-core "1.7.1"]
                 [bk/ring-gzip "0.3.0"]
                 [bidi "2.1.5"]

                 ;; the following 3 are not used directly, but are pinned to ensure consistency.
                 ;; delete then if you upgrade anything and reanalyze deps
                 [commons-codec "1.11"]
                 [args4j "2.33"]
                 [com.google.errorprone/error_prone_annotations "2.3.2"]
                 [com.google.code.findbugs/jsr305 "3.0.2"]

                 [nubank/workspaces "1.0.2" :scope "test" :exclusions [com.cognitect/transit-java]]

                 ; only required if you want to use this for tests
                 [fulcrologic/fulcro-spec "2.1.3" :scope "test"]

                 ; for dbas client
                 [com.wsscode/pathom "2.2.5"]
                 [cljs-http "0.1.45"]
                 [org.clojure/data.json "0.2.6"]
                 [org.clojure/tools.reader "1.3.2"]
                 [org.clojure/spec.alpha "0.2.176"]]

  :uberjar-name "decidotron.jar"

  :source-paths ["src/main"]
  :test-paths ["src/test"]

  :test-refresh {:report       fulcro-spec.reporters.terminal/fulcro-report
                 :with-repl    true
                 :changes-only true}

  :profiles {:uberjar    {:main           decidotron.server-main
                          :aot            :all
                          :jar-exclusions [#"public/js/test" #"public/js/workspaces" #"public/workspaces.html"]
                          :prep-tasks     ["clean" ["clean"]
                                           "compile" ["with-profile" "cljs" "run" "-m" "shadow.cljs.devtools.cli" "release" "main"]]}
             :production {}
             :cljs       {:source-paths ["src/main" "src/test" "src/workspaces"]
                          :dependencies [[binaryage/devtools "0.9.10"]
                                         [org.clojure/clojurescript "1.10.439"]
                                         [fulcrologic/fulcro-inspect "2.2.4"]]}
             :dev        {:source-paths ["src/dev" "src/main" "src/workspaces"]
                          :jvm-opts     ["-XX:-OmitStackTraceInFastThrow" "-Xmx1g"]

                          :plugins      [[com.jakemccrary/lein-test-refresh "0.23.0"]]

                          :dependencies [[org.clojure/tools.namespace "0.3.0-alpha4"]
                                         [org.clojure/tools.nrepl "0.2.13"]
                                         [cider/piggieback "0.3.10"]]
                          :repl-options {:init-ns          user
                                         :nrepl-middleware [cider.piggieback/wrap-cljs-repl]}}})
