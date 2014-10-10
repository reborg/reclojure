(defproject reclojure "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/tools.logging "0.2.6"]
                 [ch.qos.logback/logback-classic "1.0.13" :exclusions [org.slf4j/slf4j-api]]
                 [ch.qos.logback/logback-access "1.0.13"]
                 [ch.qos.logback/logback-core "1.0.13"]
                 [org.slf4j/slf4j-api "1.7.5"]]
  ;;:repl-options {:init (do (set! *warn-on-reflection* true) (require 'midje.repl) (midje.repl/autotest))}
  :repl-options {:init (do (require 'midje.repl) (midje.repl/autotest))}
  :profiles {:uberjar
             {:main some.core, :aot :all}
             :dev
             {:dependencies [[midje "1.6.3"]]
              :plugins [[lein-midje "3.1.1"]]}})
