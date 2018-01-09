(ns demo.i18n
  (:require
    [shadow.i18n :refer (tr trc)]
    ))

(tr "translate me plz")

(trc "foo" "foo?")
(trc "bar" "foo?")
