(ns ^{:doc "Test for Access log to json converter"
      :author "Jitendra Takalkar"}
  log2json.test.core
  (:use [log2json.core])
  (:use [clojure.test]))

(def line1 "10.146.94.71 - - [18/Nov/2011:11:15:10 +0100] \"GET /abonnement/aftale/165287 HTTP/1.1\" 500 120 \"-\" \"-\" 405 355 5221")

(def line2 "10.146.94.71 - - [18/Nov/2011:11:20:31 +0100] \"GET /kunde-v1/alive?_=1321611632099 HTTP/1.1\" 200 2026 \"http://preprod-kasia.yousee.dk/docs/alive.html\" \"Mozilla/5.0 (Windows NT 5.1; rv:5.0) Gecko/20100101 Firefox/5.0\" 616 2301 509399")

