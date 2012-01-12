(ns ^{:author "Jitendra Takalkar jitendra.takalkar@gmail.com"
      :doc "Common configuration"}
  log2json.config)

(def k2-modules '("abonnement"
                  "adresse"
                  "afsaetning"
                  "kunde"
                  "logistik"
                  "ordre"
                  "produkt"
                  "provisionering"))

(def success-status-codes '("200" "201" "204" "207"))

(def error-status-codes '("400" "403" "404" "406" "500" "503"))

(def re-expn #"^([\d.]+) (\S+) (\S+) \[([\w:/]+\s[+\-]\d{4})\] \"(.+?)\" (\d{3}) (\d+) \"(.+?)\" \"(.+?)\" (\d+) (\d+) (\d+)")

(def json-map {:chart {:renderTo "container"
                       :plotBackgroundColor "none"
                       :backgroundColor "none"
                       :defaultSeriesType "spline"}
               :credits {:enabled false}               
               :yAxis {:gridLineColor "rgba(255,255,255,0.05)"
                       :title {:text "SERVER HITS"}
                       :min 0}
               :tooltip {:crosshairs true
                         :shared true}
               :plotOptions {:spline {:marker {:radius 4
                                               :lineColor "#666666"
                                               :lineWidth 1}}}})

