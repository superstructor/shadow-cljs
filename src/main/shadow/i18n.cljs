(ns shadow.i18n
  (:require-macros [shadow.i18n]))

(def translations-ref (atom {}))
(def language-ref (atom nil))

(defn get-text
  ([key]
   (get @translations-ref [@language-ref key nil] key))
  ([ctx key]
   (get @translations-ref [@language-ref key ctx] key)))
