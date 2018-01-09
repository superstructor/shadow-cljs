(ns shadow.i18n
  (:require-macros [shadow.i18n]))

(def translations-ref (atom {}))

(defn get-text [key]
  (get-in @translations-ref key key))
