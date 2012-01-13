(defproject log2json "1.0.0"
  :description "Access log to json converter"
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [org.clojure/data.json "0.1.2"]]
  :dev-dependencies [[swank-clojure "1.3.3"]
                     [lein-ring "0.4.5"]]
  :main log2json.historic
  :repositories {"sonatype-oss-public" "http://oss.sonatype.org/content/groups/public/"})
