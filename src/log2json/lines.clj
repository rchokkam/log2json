(ns ^{:author "Jitendra Takalkar jitendra.takalkar@gmail.com"
      :doc "Access log to json converter"}
  log2json.lines
  (:gen-class)
  (:use [clojure.data.json :only (json-str write-json read-json)])
  (:import [java.io BufferedReader FileReader BufferedWriter FileWriter PrintWriter]
           [java.util Calendar]))

(def module-names '("abonnement"
                    "adresse"
                    "afsaetning"
                    "kunde"
                    "logistik"
                    "ordre"
                    "produkt"
                    "provisionering"))


(def hour-names (atom '()))

(def status-codes '("200" "201" "204" "207"))

(def json-map {:chart {:renderTo "container"
                       :plotBackgroundColor "none"
                       :backgroundColor "none"
                       :defaultSeriesType "spline"}
               :credits {:enabled false}               
               :yAxis {:gridLineColor "rgba(255,255,255,0.05)"
                       :title {:text "SERVER HITS"}}
               :tooltip {:crosshairs true
                         :shared true}
               :plotOptions {:spline {:marker {:radius 4
                                               :lineColor "#666666"
                                               :lineWidth 1}}}})

(defn get-hr
  ""
  []
  (let [hour (.get (Calendar/getInstance) (Calendar/HOUR_OF_DAY))]
    (if (= hour 24) 
      hour
      (inc hour))))


(defn init-hours
  ""
  []
  (let [ch (get-hr)]
    (loop [i ch j 0]
      (if (or (< i 0) (= j 9))
        @hour-names
        (recur (do (swap! hour-names conj (if (< i 10) (str "0" i) (str i))) (dec i)) (inc j))))))                                      

(defn init-method-count
  "Returns the map"
  []
  (loop [m-map {} m-names @hour-names]
    (if (empty? m-names)
      m-map
      (recur (assoc m-map (first m-names) 0) (rest m-names)))))

(defn init-s-keys
  [^String module-name]
  (loop [rvec [] scodes  status-codes]
    (if (empty? scodes)
      rvec
      (recur (conj rvec (str module-name ":" (first scodes)))             
             (rest scodes)))))

(defn init-keys
  []
  (loop [rvec [] modules module-names]
    (if (empty? modules)
      rvec
      (recur (into rvec (init-s-keys (first modules)))
             (rest modules)))))

(defn init-structure
  "This return the initial data structure"
  []
  (let [m-counts (init-method-count)]
    (loop [rmap {} keys (init-keys)]
      (if (empty? keys)
        rmap
        (recur (assoc rmap (first keys) m-counts)
               (rest keys))))))


(def re-expn #"^([\d.]+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] \"(.+?)\" (\d{3}) (\d+) \"(.+?)\" \"(.+?)\" (\d+) (\d+) (\d+)")

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

(defn get-hour
  [^String date-str]
  (let [datetoken (.split date-str ":")]
    (get datetoken 1)))

(defn parse-log-line
  "Returns map object after parsing the line"
  [^String line]
  (let [tokens (re-find re-expn line)
        hour-uri (get tokens 4)
        request-uri (get tokens 5)
        status-code (get tokens 6)
        module-name (get-module-name request-uri)
        hour-count (get-hour hour-uri)
        key (str (.toLowerCase module-name) ":" status-code)]
    [key hour-count]))

(defn parse-log-file
  "Returns an array of Lines of log file"
  [r-map ^String file-path]
  (loop [rmap r-map
         lines (line-seq (BufferedReader. (FileReader. file-path)))]
    (if (empty? lines)
      rmap
      (if (skip-line? (first lines))
        (recur rmap (rest lines))
        (let [line-vec (parse-log-line (first lines))
              tcount (get-in rmap line-vec)
              acount (if (nil? tcount) 0 tcount)]
          (recur (assoc-in rmap line-vec (inc acount))
                 (rest lines)))))))

(defn parse-log-files
  "Returns the map datastructure"
  [& file-paths]
  (loop [r-map (init-structure) f-paths file-paths]
    (if (empty? f-paths)
      r-map
      (recur (parse-log-file r-map (first f-paths))
             (rest f-paths)))))                 

(defn get-hour-count-data
  "Return vector"
  [m-map]
  (loop [rvec [] hours @hour-names]
    (if (empty? hours)
      rvec
      (recur (conj rvec (get m-map (first hours)))
             (rest hours)))))

(defn get-module-data
  ""
  [^String module-name d-map]
  (loop [r-vec [] s-codes status-codes]
    (if (empty? s-codes)
      (merge json-map
             {:title {:text (.toUpperCase module-name)}
              :subtitle {:text (str "KASIA2-SUCCESS")}
              :xAxis {:categories (vec @hour-names)}
              :series r-vec})
      (recur (conj r-vec {:name (first s-codes)
                          :data (get-hour-count-data (get d-map (str module-name ":" (first s-codes))))})                       
             (rest s-codes)))))

(defn map-to-json
  "Return json string representation"
  [m-data]
  (json-str m-data))

(defn write-to-file
  "Create new File and write the string content."
  [^String file-path ^String data]
  (with-open [file-writer (FileWriter. file-path)
              bf-writer (BufferedWriter. file-writer)
              out (PrintWriter. bf-writer)]
    (.write out data)))

(defn for-each-module
  ""
  [^String base-path ^String file-name-1 ^String file-name-2]
  (let [file-path-1 (str base-path file-name-1)
        file-path-2 (str base-path file-name-2)
        d-map (parse-log-files file-path-1 file-path-2)        
        out-file-name (str "access_line_log.json")]
    (loop [rpath [] modules module-names]
      (if (empty? modules)
        rpath
        (recur (conj rpath (str base-path (first modules) "_" out-file-name))
               (do (write-to-file (str base-path (first modules) "_" out-file-name)
                                  (json-str (get-module-data (first modules)
                                                             d-map)))
                   (rest modules)))))))

(defn -main
  "Main function"
  [^String base-path ^String file-name-1 ^String file-name-2]
  (init-hours)
  (for-each-module base-path file-name-1 file-name-2))