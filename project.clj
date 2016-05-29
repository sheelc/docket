(defproject docket "0.1.0-SNAPSHOT"
  :description "An alternative scheduler for ECS"
  :url "https://github.com/sheelc/docket"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [com.taoensso/timbre "3.4.0"]
                 [com.stuartsierra/component "0.3.1"]
                 [environ "1.0.0"]
                 [compojure "1.5.0"]
                 [noir-exception "0.2.5"]
                 [http-kit "2.2.0-alpha1"]
                 [org.clojure/clojurescript "1.8.51"]
                 [reagent "0.6.0-alpha2"]
                 [amazonica "0.3.58"]
                 [com.fasterxml.jackson.core/jackson-core "2.5.3"]
                 [overtone/at-at "1.2.0"]]
  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.3-2"]]
  :figwheel {:nrepl-port 4000
             :css-dirs ["resources/public/css"]}
  :clean-targets ^{:protect false} [:target-path
                                    "resources/public/js/out/"
                                    "resources/public/css/"]
  :source-paths ["src"]
  :main docket.core
  :repl-options {:init-ns user}
  :jvm-opts ["-XX:-OmitStackTraceInFastThrow" "-Xmx768m"]
  :auto-clean false:cljsbuild
  :cljsbuild
  {:builds
   {:dev
    {:source-paths ["src-cljs"]
     :figwheel {:on-jsload "docket.core/on-jsload"}
     :compiler {:main "docket.core"
                :asset-path "/js/out"
                :output-to "resources/public/js/out/main.js"
                :pretty-print true
                :output-dir "resources/public/js/out"}}
    :release
    {:source-paths ["src-cljs"]
     :compiler {:main "docket.core"
                :asset-path "/js/out"
                :output-to "target/release/js/out/main.js"
                :output-dir "target/release/js/out"
                :pretty-print true}}}}
  :profiles
  {:uberjar {:aot :all}
   :dev {:source-paths ["dev"]
         :dependencies [[diff-eq "0.2.2"]
                        [org.clojure/tools.namespace "0.2.9"]]
         :plugins [[lein-cljfmt "0.3.0"]]
         :injections [(require 'diff-eq.core)
                      (diff-eq.core/diff!)]}})
