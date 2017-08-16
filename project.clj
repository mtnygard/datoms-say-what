(defproject datoms-say-what "0.1.0-SNAPSHOT"
  :description   "Visualize the results of a Datomic transaction"
  :url           "http://github.com/mtnygard/datoms-say-what"
  :license       {:name "Eclipse Public License"
                  :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies  [[org.clojure/clojure "1.9.0-alpha17"]
                  [com.datomic/datomic-pro "0.9.5561.54" :scope "provided"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
