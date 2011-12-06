(ns ^{:doc "Access log to json converter"
      :author "Jitendra Takalkar"}
  log2json.core
  (:gen-class)
  (:use [clojure.data.json :only (json-str write-json read-json)])
  (:import [java.io BufferedReader FileReader BufferedWriter FileWriter PrintWriter]))

(def module-names '("abonnement"
                   "adresse"
                   "afsaetning"
                   "kunde"
                   "logistik"
                   "ordre"
                   "produkt"
                   "provisionering"))

(def method-names '("GET" "PUT" "POST" "DELETE"))

(def status-codes '("200" "201" "204" "207" "400" "403" "404" "406" "500" "503"))

(def json-map {:chart {:renderTo "container"
                       :plotBackgroundColor "none"
                       :backgroundColor "none"
                       :defaultSeriesType "column"}
               :credits {:enabled false}
               :xAxis {:categories (vec method-names)}
               :yAxis {:gridLineColor "rgba(255,255,255,0.05)"
                       :title {:text "SERVER HITS"}}
               :tooltip {:crosshairs true
                         :shared true}
               :plotOptions {:column {:pointPadding 0.2
                                      :borderWidth 0}}})

(defn init-method-count
  "Returns the map"
  []
  (loop [m-map {} m-names method-names]
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

(defn get-method-name
  "Returns map containing module and method name"
  [^String req-str]
  (let [tokens (.split req-str " ")]
    (get tokens 0)))

(defn parse-log-line
  "Returns map object after parsing the line"
  [^String line]
  (let [tokens (re-find re-expn line)
        request-uri (get tokens 5)
        status-code (get tokens 6)
        response-time (get tokens 7)
        module-name (get-module-name request-uri)
        method-name (get-method-name request-uri)
        key (str (.toLowerCase module-name) ":" status-code)]
    [key method-name]))

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

(defn get-method-count-data
  "Return vector"
  [m-map]
  (loop [rvec [] methods method-names]
    (if (empty? methods)
      rvec
      (recur (conj rvec (get m-map (first methods)))
             (rest methods)))))

(defn get-module-data
  ""
  [^String env-name ^String module-name d-map]
  (loop [r-vec [] s-codes status-codes]
    (if (empty? s-codes)
      (merge json-map
             {:title {:text (.toUpperCase module-name)}
              :subtitle {:text (str "KASIA2-" (.toUpperCase env-name))}
              :series r-vec})
      (recur (conj r-vec {:name (first s-codes)
                          :data (get-method-count-data (get d-map (str module-name ":" (first s-codes))))})                       
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
  [^String env-name ^String base-path ^String file-name-1 ^String file-name-2]
  (let [file-path-1 (str base-path file-name-1)
        file-path-2 (str base-path file-name-2)
        d-map (parse-log-files file-path-1 file-path-2)        
        out-file-name (str file-name-1 ".json")]
    (loop [rpath [] modules module-names]
      (if (empty? modules)
        rpath
        (recur (conj rpath (str base-path (first modules) "_" out-file-name))
               (do (write-to-file (str base-path (first modules) "_" out-file-name)
                                  (json-str (get-module-data env-name
                                                             (first modules)
                                                             d-map)))
                   (rest modules)))))))

;; (for-each-module "/home/jitendra/" "access_log")

(defn -main
  "Main function"
  [^String env-name ^String base-path ^String file-name-1 ^String file-name-2]
  (for-each-module env-name base-path file-name-1 file-name-2))