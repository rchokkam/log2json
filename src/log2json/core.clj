(ns ^{:doc "Access log to json converter"
      :author "Jitendra Takalkar"}
  log2json.core
  (:gen-class)
  (:use [clojure.data.json :only (json-str write-json read-json)])
  (:import [java.io BufferedReader FileReader BufferedWriter FileWriter PrintWriter]))

(def re-expn #"^([\d.]+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] \"(.+?)\" (\d{3}) (\d+) \"(.+?)\" \"(.+?)\" (\d+) (\d+) (\d+)")

(def servers (atom '()))

(def module-names '("abonnement"
                   "adresse"
                   "afsaetning"
                   "kunde"
                   "logistik"
                   "ordre"
                   "produkt"
                   "provisionering"))

(defn init-servers
  "Initialize the servers list"
  [^Integer count]
  (loop [i count]
    (if (= i 0)
      @servers
      (recur (do (swap! servers conj i) (dec i))))))

(def status-codes '("400" "403" "404" "406" "500" "503"))

(def json-map {:chart {:renderTo "container"
                       :plotBackgroundColor "none"
                       :backgroundColor "none"
                       :defaultSeriesType "column"}
               :credits {:enabled false}  
               :xAxis {:categories (vec status-codes)}            
               :yAxis {:gridLineColor "rgba(255,255,255,0.05)"
                       :title {:text "SERVER HITS"}}
               :tooltip {:crosshairs true
                         :shared true}
               :subtitle {:text "KASIA2"
                          :style {:color "#ffffff"
                                  :fontSize "15px"}
                          }
               :plotOptions {:column {:pointPadding 0.2
                                      :borderWidth 0}}})

(defn init-status-code-map
  "Returns the initial map for status codes"
  []
  (loop [s-map {} s-codes status-codes]
    (if (empty? s-codes)
      s-map
      (recur (assoc s-map (first s-codes) 0) (rest s-codes)))))  

(defn init-server-keys
  [^String module-name]
  (loop [rvec [] s @servers]
    (if (empty? s)
      rvec
      (recur (conj rvec (str module-name ":" (first s)))             
             (rest s)))))

(defn init-map-keys
  "Initialize the map keys"
  []
  (loop [rvec [] modules module-names]
    (if (empty? modules)
      rvec
      (recur (into rvec (init-server-keys (first modules)))
             (rest modules)))))

(defn init-column-map
  "This return the initial data structure for column graph"
  []
  (let [s-counts (init-status-code-map)]
    (loop [rmap {} keys (init-map-keys)]
      (if (empty? keys)
        rmap
        (recur (assoc rmap (first keys) s-counts)
               (rest keys))))))


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

(defn parse-log-line
  "Returns map object after parsing the line"
  [^Integer i ^String line]
  (let [tokens (re-find re-expn line)
        request-uri (get tokens 5)
        status-code (get tokens 6)        
        module-name (get-module-name request-uri)
        key (str (.toLowerCase module-name) ":" i)]
    [key status-code]))

(defn parse-log-file
  "Returns an array of Lines of log file"
  [^Integer i r-map ^String file-path]
  (with-open [fr (FileReader. file-path)
              br (BufferedReader. fr)]
    (loop [rmap r-map
           lines (line-seq br)]
      (if (empty? lines)
        rmap
        (if (skip-line? (first lines))
          (recur rmap (rest lines))
          (let [line-vec (parse-log-line i (first lines))
                tcount (get-in rmap line-vec)
                acount (if (nil? tcount) 0 tcount)]
            (recur (assoc-in rmap line-vec (inc acount))
                   (rest lines))))))))

(defn parse-log-files
  "Returns the map datastructure"
  [^String base-path file-names]
  (loop [r-map (init-column-map) f-names file-names i 1]
    (if (empty? f-names)
      r-map
      (recur (parse-log-file i r-map (str base-path (first f-names)))
        (rest f-names)
        (inc i)))))

(defn get-status-code-data
  "Return vector"
  [m-map]
  (loop [rvec [] s-codes status-codes]
    (if (empty? s-codes)
      rvec
      (recur (conj rvec (get m-map (first s-codes)))
             (rest s-codes)))))

(defn get-module-data
  ""
  [^String module-name d-map]
  (loop [r-vec [] s @servers]
    (if (empty? s)
      (merge json-map
             {:title {:text (.toUpperCase module-name)
                      :style {:color "#ffffff"
                              :fontWeight "bold"
                              :fontSize "27px"}}              
              :series r-vec})
      (recur (conj r-vec {:name (str "SERVER-" (first s))
                          :data (get-status-code-data (get d-map (str module-name ":" (first s))))})                       
             (rest s)))))

(defn write-to-file
  "Create new File and write the string content."
  [^String file-path ^String data]
  (with-open [file-writer (FileWriter. file-path)
              bf-writer (BufferedWriter. file-writer)
              out (PrintWriter. bf-writer)]
    (.write out data)))

(defn for-each-module
  "Process for each module"
  [^String base-path file-names]
  (let [d-map (parse-log-files base-path file-names)        
        out-file-name (str "access_log.json")]
    (loop [rpath [] modules module-names]
      (if (empty? modules)
        rpath
        (recur (conj rpath (str base-path (first modules) "_" out-file-name))
               (do (write-to-file (str base-path (first modules) "_" out-file-name) 
                                  (json-str (get-module-data (first modules) d-map)))
                   (rest modules)))))))

(defn -main
  "Main function"
  [^String base-path & file-names]
  (init-servers (count file-names))
  (for-each-module base-path file-names))