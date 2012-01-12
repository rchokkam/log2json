(ns ^{:author "Jitendra Takalkar jitendra.takalkar@gmail.com"
      :doc "Utility functions of log2json utility"}
  log2json.util
  (:import [java.text SimpleDateFormat]
           [java.util Comparator]))

(def s-d-format (SimpleDateFormat. "dd/MMM/yyyy"))

(def d-comparator
  (reify java.util.Comparator
    (^int compare [this s1 s2]
      (let [d1 (.parse s-d-format s1)
            d2 (.parse s-d-format s2)]
        (.compareTo d1 d2)))
    (^boolean equals [this obj]
      (.equals this obj))))