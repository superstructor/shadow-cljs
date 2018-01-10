(ns demo.i18n
  (:require [cljs.i18n :refer (tr)]))

(tr "translate me plz")
(tr "translate me plz")

(tr ["foo?" "foo"])
(tr ["foo?" "bar"])

(tr "foo {thing} bar" :thing "yo")
