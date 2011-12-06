(ns ^{:doc "Access log to json converter for line chart"
      :author "Ramamohan Chokkam"}
  log2json.core-line
  (:require  
       [log2json.core :as cr])

(def hour-names '("08" "09" "10" "11" "12" "13" "14" "15" "16" "18" "19" "20"))

(def json-line-map {:chart {:renderTo "container"
                       :plotBackgroundColor "none"
                       :backgroundColor "none"
                       :defaultSeriesType "spline"}
               :credits {:enabled false}
               :xAxis {:categories (vec hour-names)}
               :yAxis {:gridLineColor "rgba(255,255,255,0.05)"
                       :title {:text "SERVER HITS"}}
               :tooltip {:crosshairs true
                         :shared true}
               :plotOptions {:spline {:marker {:radius 4
                                      :lineColor '#666666'
                                      :lineWidth 1}}})

(defn init-hour-count
  "Returns the map"
  []
  (loop [h-map {} h-names hour-names]
    (if (empty? h-names)
      h-map
      (recur (assoc h-map (first h-names) 0) (rest h-names)))))

(defn get-hour
  [^String date-str]
  (let [datetoken (.split date-str ":")]
    (get datetoken 1)))

(defn parse-log-line1
  "Returns map object after parsing the line"
  [^String line]
  (let [tokens (re-find cr/re-expn line)
        hour-uri (get tokens 4)
        request-uri (get tokens 5)
        status-code (get tokens 6)
        response-time (get tokens 7)
        module-name (cr/get-module-name request-uri)
        hour-count (get-hour hour-uri)
        key (str (.toLowerCase module-name) ":" status-code)]
    [key hour-count]))

(defn get-hour-count-data
  "Return vector"
  [h-map]
  (loop [rvec [] hours hour-names]
    (if (empty? hours)
      rvec
      (recur (conj rvec (get h-map (first hours)))
             (rest hours)))))
