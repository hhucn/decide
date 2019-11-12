(defproject decide "2.0.0-SNAPSHOT"

  :plugins [[lein-tools-deps "0.4.5"]]
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}

  :main decide.server-main
  :clean-targets ^{:protect false} [:target-path "resources/public/js/" "resources/public/workspaces/js/" "resources/public/css/main.css.map"]

  :profiles {:uberjar    {:main           decide.server-main
                          :aot            [decide.server-main]
                          :uberjar-name "decide.jar"
                          :jar-exclusions [#"public/js/test" #"public/js/workspaces" #"public/workspaces.html"]
                          :prep-tasks     ["clean" ["clean"]
                                           "compile" ["with-profile" "cljs" "run" "-m" "shadow.cljs.devtools.cli" "release" "main"]]}
             :cljs       {:source-paths ["src/main"]
                          :dependencies [[com.google.javascript/closure-compiler-unshaded "v20191027"]
                                         [org.clojure/google-closure-library "0.0-20190213-2033d5d9"]
                                         [thheller/shadow-cljs "2.8.69"]]}})
