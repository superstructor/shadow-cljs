(ns shadow.cljs.ui.make-fulcro-happy
  (:require ["react" :as react]
            ["react-dom" :as rdom]))

;; can't require anything from fulcro here since everything here needs to run before fulcro loads
;; since its built on the globals still

(js/goog.exportSymbol "React" react)
(js/goog.exportSymbol "ReactDOM" rdom)

