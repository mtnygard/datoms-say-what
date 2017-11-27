(defproject mtnygard/datoms-say-what "1.0.3-SNAPSHOT"
  :description   "Visualize the results of a Datomic transaction"
  :url           "http://github.com/mtnygard/datoms-say-what"
  :license       {:name "Eclipse Public License"
                  :url  "http://www.eclipse.org/legal/epl-v10.html"}
  :repositories {"my.datomic.com" {:url   "https://my.datomic.com/repo"
                                   :creds :gpg}}
  :dependencies  [[org.clojure/clojure "1.9.0-RC2"]
                  [com.datomic/datomic-pro "0.9.5561.54" :scope "provided"]
                  [viz-cljc "0.1.3"]]
  :profiles {:dev {:dependencies [[org.clojure/test.check "0.9.0"]]}})
