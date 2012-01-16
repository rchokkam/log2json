(ns ^{:author "Jitendra Takalkar jitendra.takalkar@gmail.com"
      :doc "Order stage statistics"}
  log2json.order
  (:gen-class)
  (:use [log2json.common])
  (:use [clojure.data.json :only (json-str write-json read-json)])
  (:import [java.io File FileReader BufferedReader BufferedWriter FileWriter]))
(def color-map {:Modtaget "FFFF10AA"
                :Parse_OK "FFAA0AAA"
                :Parse_Fejl "FF0000AA"
                :Running "FF5505AA"
                :Afsluttet "AAFF5555"})
(defn process-order-stage
  "Process map"
  [smap]
  {:label (first (keys smap))
   :value (first (vals smap))
   :colour ((first (keys smap)) color-map)})

(defn process-order-data
  "Process Order Status data"
  [^String str-vec]
  (loop [rvec []
         ovec (read-json str-vec)]
    (if (empty? ovec)
      rvec
      (if (= :Total (first (keys (first ovec))))
        (recur rvec (rest ovec))
        (recur (conj rvec (process-order-stage (first ovec))) (rest ovec))))))

(defn -main
  "Main function accept one argument file path"
  [^String file-path]
  (loop [tstr ""
         lines (line-seq (BufferedReader. (FileReader. (File. file-path))))]
    (if (empty? lines)
      (do (write-to-file (str file-path ".json") (json-str {:item (process-order-data tstr)}))
          "Done")
      (recur (str tstr " " (first lines))
             (rest lines)))))

;; (-main "/home/jitendra/ordre.json")