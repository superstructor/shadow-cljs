(ns shadow.build.i18n
  (:require [shadow.build.data :as data]
            [clojure.java.io :as io]))

(defn process [state]
  (let [messages-file
        (get-in state [:i18n-options :messages-file])]

    (when (seq messages-file)
      (let [all-strings
            (reduce-kv
              (fn [all ns {strings :shadow.i18n/strings}]
                (into all strings))
              []
              (get-in state [:compiler-env :cljs.analyzer/namespaces]))

            de-duped
            (->> all-strings
                 (group-by #(select-keys % [:msg :context]))
                 (into []))

            pot-text
            (with-out-str
              (doseq [[{:keys [msg context] :as tr} occurences] de-duped]
                (println)
                (doseq [{:keys [ns line column]} occurences]
                  (println (str "#: " ns ":" line ":" column)))
                (println (str "msgid " (pr-str msg)))
                (println (str "msgstr \"\""))
                ))

            pot-file
            (data/output-file state messages-file)]

        (io/make-parents pot-file)
        (spit pot-file pot-text)
        )))

  state)
