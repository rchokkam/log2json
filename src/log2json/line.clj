(ns ^{:author "Jitendra Takalkar jitendra.takalkar@gmail.com"
      :doc "Access log to json converter"}
  log2json.line
  (:gen-class)
  (:use [log2json.config])
  (:use [log2json.common])
  (:use [log2json.util])
  (:use [clojure.data.json :only (json-str write-json read-json)])
  (:import [java.io File FileFilter FileReader BufferedReader BufferedWriter FileWriter]))

(def hours (atom (sorted-set-by h-comparator)))

(defn get-hour
  [^String date-str]
  (let [datetoken (.split date-str ":")
        hour (get datetoken 1)]
    (swap! hours conj hour)
    hour))

(defn parse-log-line
  "Parse access log line"
  [^String line]
  (let [tokens (re-find re-expn line)
        key2 (get-hour (get tokens 4))
        status-code (get tokens 6)
        module-name (get-module-name (get tokens 5))
        key1 (str (.toLowerCase module-name) ":" status-code)]
    [key1 key2]))

(defn process-access-file
  "For each folder"
  [a-map ^File access-file]
  (with-open [fr (FileReader. access-file)
              br (BufferedReader. fr)]
    (loop [rmap a-map
           lines (line-seq br)]
      (if (empty? lines)
        rmap
        (if (skip-line? (first lines))
          (recur rmap (rest lines))
          (let [line-vec (parse-log-line (first lines))
                tcount (get-in rmap line-vec)
                acount (if (nil? tcount) 0 tcount)]
            (recur (assoc-in rmap line-vec (inc acount))
                   (rest lines))))))))  

(defn get-count-data
  "Returns vector"
  [m-map]
  (loop [rvec [] str-hours @hours]
    (if (empty? str-hours)
      (replace {nil 0} rvec)
      (recur (conj rvec (get m-map (first str-hours)))
             (rest str-hours)))))

(defn get-module-success-data
  "Return the JSON"
  [^String module-name d-map]
  (loop [r-vec [] s-codes success-status-codes]
    (if (empty? s-codes)
      (merge json-map
             {:title {:text (.toUpperCase module-name)
                      :style {:color "#ffffff"
                              :fontWeight "bold"
                              :fontSize "27px"}}
              :subtitle {:text (str "KASIA2-SUCCESS")
                         :style {:color "#ffffff"
                                 :fontSize "15px"}}
              :xAxis {:categories (vec @hours)
                      :labels {:rotation 90
                               :step 2}}
              :series r-vec})
      (recur (conj r-vec {:name (first s-codes)
                          :data (get-count-data (get d-map (str module-name ":" (first s-codes))))})                       
             (rest s-codes)))))

(defn get-module-error-data
  "Return the JSON"
  [^String module-name d-map]
  (loop [r-vec [] s-codes error-status-codes]
    (if (empty? s-codes)
      (merge json-map
             {:title {:text (.toUpperCase module-name)
                      :style {:color "#ffffff"
                              :fontWeight "bold"
                              :fontSize "27px"}}
              :subtitle {:text (str "KASIA2-ERROR")
                         :style {:color "#ffffff"
                                 :fontSize "15px"}}
              :xAxis {:categories (vec @hours)
                      :labels {:rotation 90
                               :step 2}}
              :series r-vec})
      (recur (conj r-vec {:name (first s-codes)
                          :data (get-count-data (get d-map (str module-name ":" (first s-codes))))})                       
             (rest s-codes)))))                 

(defn for-each-module
  "Process"
  [^String base-path d-map]
  (loop [modules k2-modules]
    (if (empty? modules)
      "Done"
      (recur (do (write-to-file (str base-path "/" (first modules) "_" "access_line_log.json")
                                (json-str (get-module-success-data (first modules) d-map)))
                 (write-to-file (str base-path "/" (first modules) "_" "access_line_e_log.json")
                                (json-str (get-module-error-data (first modules) d-map)))                 
                 (rest modules))))))                 

(defn -main
  "Main function"
  [^String base-path & file-names]
  (loop [a-map {} files file-names]
    (if (empty? files)
      (for-each-module base-path a-map) 
      (recur (process-access-file 
              a-map 
              (File. (str base-path (first files)))) 
             (rest files)))))

