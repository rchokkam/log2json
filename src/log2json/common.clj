(ns ^{:author "Jitendra Takalkar jitendra.takalkar@gmail.com"
      :doc "Common functions used in the log2json utility"}
  log2json.common
  (:use [log2json.config])
  (:import [java.io FileWriter BufferedWriter PrintWriter]))

(defn skip-line?
  [^String line]
  (or (= "" (.trim line))
       (.contains line "alive.html")
       (.contains line "alive?")
       (.contains line "/docs")
       (not (or (.contains line "abonnement")
                (.contains line "adresse")
                (.contains line "afsaetning")
                (.contains line "kunde")
                (.contains line "logistik")
                (.contains line "ordre")
                (.contains line "produkt")
                (.contains line "provisionering")))
       (nil? (re-matches re-expn line))))

(defn get-module-name
  "Return module name"
  [^String token]
  (let [tokens1 (.split token " ")
        token (get tokens1 1)
        tokens2 (.split token "/")
        cnt (count tokens2)]
    (if (== 0 cnt)
      token
      (.replace (get tokens2 1) "-v1" ""))))

(defn write-to-file
  "Create new File and write the string content."
  [^String file-path ^String data]
  (with-open [file-writer (FileWriter. file-path)
              bf-writer (BufferedWriter. file-writer)
              out (PrintWriter. bf-writer)]
    (.write out data)))