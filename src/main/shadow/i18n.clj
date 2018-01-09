(ns shadow.i18n
  (:require [cljs.env :as env]
            [cljs.analyzer :as ana]))

(def vec-conj (fnil conj []))

(defmacro tr [msg]
  (let [{:keys [line column]}
        (meta &form)

        string-data
        {:msg msg
         :line line
         :column column}]
    (swap! env/*compiler* update-in [::ana/namespaces (-> &env :ns :name) ::strings] vec-conj string-data))
  `(get-text ~msg))
