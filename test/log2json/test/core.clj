(ns ^{:doc "Test for Access log to json converter"
      :author "Jitendra Takalkar"}
  log2json.test.core
  (:use [log2json.core])
  (:use [clojure.test]))

(def line1 "10.146.94.71 - - [18/Nov/2011:11:15:10 +0100] \"GET /abonnement/aftale/165287 HTTP/1.1\" 500 120 \"-\" \"-\" 405 355 5221")

(def line2 "10.146.94.71 - - [18/Nov/2011:11:20:31 +0100] \"GET /kunde-v1/alive?_=1321611632099 HTTP/1.1\" 200 2026 \"http://preprod-kasia.yousee.dk/docs/alive.html\" \"Mozilla/5.0 (Windows NT 5.1; rv:5.0) Gecko/20100101 Firefox/5.0\" 616 2301 509399")

(deftest test-get-names 
  (is (= "GET" (get-method-name "GET /abc/xyz/")))
  (is (= "abc" (get-module-name "GET /abc/xyz/"))))

(deftest test-parse-log-line
  (is (= "GET" (get (parse-log-line line1) 1)))
  (is (= "GET" (get (parse-log-line line1) 1)))
  (is (= "abonnement:500" (get (parse-log-line line1) 0)))
  (is (= "abonnement:500" (get (parse-log-line line1) 0)))
  (is (= "kunde:200" (get (parse-log-line line2) 0))))

(deftest test-parse-log-file
  (is (= 32 (count
                (let
                    [vectors
                     (parse-log-file "/home/jitendra/access_log")]
                  (do (println vectors)
                      vectors))))))

(deftest test-init-method-count
  (is (= 4 (count (init-method-count))))
  (is (= 0 (get (init-method-count) "GET"))))

(deftest test-init-keys
  (is (= 64 (count (init-keys)))))

(deftest test-init-structure
  (is (= 64 (count (init-structure)))))

(deftest test-get-method-count-data
  (is (= [1 2 3 4]
         (get-method-count-data {"GET" 1
                             "PUT" 2
                             "POST" 3
                             "DELETE" 4}))))


