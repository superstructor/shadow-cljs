(ns shadow.i18n
  (:require [cljs.env :as env]
            [cljs.analyzer :as ana]))

(def vec-conj (fnil conj []))

(defmacro tr [msg]
  (let [{:keys [line column]}
        (meta &form)

        current-ns
        (-> &env :ns :name)

        string-data
        {:msg msg
         :ns current-ns
         :line line
         :column column}]

    (swap! env/*compiler* update-in [::ana/namespaces current-ns ::strings] vec-conj string-data))
  `(get-text ~msg))
