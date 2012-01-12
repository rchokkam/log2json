;; Input: Folders path for server1 and server2.
;; Output: Generate files for each module.

(ns ^{:author "Jitendra Takalkar jitendra.takalkar@gmail.com"
      :doc "Last seven days access log to json convertor"}
  log2json.historic
  (:gen-class)
  (:use [log2json.config])
  (:use [log2json.common])
  (:use [log2json.util])
  (:use [clojure.data.json :only (json-str write-json read-json)])
  (:import [java.io File FileFilter FileReader BufferedReader BufferedWriter FileWriter]))

(def dates (atom (sorted-set-by d-comparator)))

(def dir-file-filter
  (reify java.io.FileFilter
    (^boolean accept [this ^File pathname]
      (.isDirectory pathname))))

(def access-file-filter
  (reify java.io.FileFilter
    (^boolean accept [this ^File pathname]
      (and (.isFile pathname) (.contains (.getName pathname) "access_log")))))

(defn get-str-date
  "Return date in dd/MON/yyyy format as a string"
  [token]
  (let [date-token (.substring token (inc (.indexOf token "[")) (.indexOf token ":"))]
    (swap! dates conj date-token)
    date-token))

(defn parse-log-line
  "Parse access log line"
  [^String line]
  (let [tokens (re-find re-expn line)
        key2 (get-str-date (get tokens 4))
        status-code (get tokens 6)
        module-name (get-module-name (get tokens 5))
        key1 (str (.toLowerCase module-name) ":" status-code)]
    [key1 key2]))

(defn process-access-file
  "For each folder"
  [a-map ^File access-file]
  (println access-file)
  (loop [rmap a-map
         lines (line-seq (BufferedReader. (FileReader. access-file)))]
    (if (empty? lines)
      rmap
      (if (skip-line? (first lines))
        (recur rmap (rest lines))
        (let [line-vec (parse-log-line (first lines))
              tcount (get-in rmap line-vec)
              acount (if (nil? tcount) 0 tcount)]
          (recur (assoc-in rmap line-vec (inc acount))
                 (rest lines)))))))

(defn get-count-data
  "Returns vector"
  [m-map]
  (loop [rvec [] str-dates @dates]
    (if (empty? str-dates)
      (replace {nil 0} rvec)
      (recur (conj rvec (get m-map (first str-dates)))
             (rest str-dates)))))

(defn get-module-success-data
  "Return the JSON"
  [^String module-name d-map]
  (loop [r-vec [] s-codes success-status-codes]
    (if (empty? s-codes)
      (merge json-map
             {:title {:text (.toUpperCase module-name)}
              :subtitle {:text (str "KASIA2-SUCCESS")}
              :xAxis {:categories (vec @dates)
                      :labels {:rotation 90
                               :y 40
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
             {:title {:text (.toUpperCase module-name)}
              :subtitle {:text (str "KASIA2-ERROR")}
              :xAxis {:categories (vec @dates)
                      :labels {:rotation 90
                               :y 40
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
      (recur (do (write-to-file (str base-path "/" (first modules) "_" "a_s_log.json")
                                (json-str (get-module-success-data (first modules) d-map)))
                 (write-to-file (str base-path "/" (first modules) "_" "a_e_log.json")
                                (json-str (get-module-error-data (first modules) d-map)))                 
                 (rest modules))))))

(defn get-list-of-files
  "Returns the list of access log files only"
  [folder-paths]
  (loop [files '()
         folders folder-paths]
    (if (empty? folders)
      files
      (recur (concat files (.listFiles (first folders) access-file-filter))
             (rest folders)))))

(defn -main
  "Main function"
  [^String folder-path-name]
  (let [folder-path (File. folder-path-name)]
    (if (.isDirectory folder-path)
      (loop [a-map {}
             files (get-list-of-files (.listFiles folder-path dir-file-filter))]
        (if (empty? files)
          (for-each-module folder-path-name a-map) ;; (do (println dates) (println a-map) (for-each-module folder-path-name a-map))
          (recur (process-access-file a-map (first files))
                 (rest files)))))))

(-main "/home/jitendra/rsync")