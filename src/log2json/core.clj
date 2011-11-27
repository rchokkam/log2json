(ns ^{:doc "Access log to json converter"
      :author "Jitendra Takalkar"}
  log2json.core
  (:import [java.io BufferedReader FileReader]))

(def module-names '("abonnement"
                   "adresse"
                   "afsaetning"
                   "kunde"
                   "logistik"
                   "ordre"
                   "produckt"
                   "provisionering"))

(def method-names '("GET" "PUT" "POST" "DELETE"))

(def status-codes '("200" "201" "204" "207" "400" "403" "404" "406" "500" "503"))

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
                (.contains line "produckt")
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
  [^String file-path]
  (loop [rmap (init-structure)
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
  [^String module-name d-map]
  (loop [r-vec [] s-codes status-codes]
    (if (empty? s-codes)
      r-vec
      (recur (conj r-vec {"name" (first s-codes)
                          "data" (get-method-count-data (get d-map (str module-name ":" (first s-codes))))})                       
             (rest s-codes)))))