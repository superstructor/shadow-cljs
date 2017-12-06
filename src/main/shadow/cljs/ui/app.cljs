(ns shadow.cljs.ui.app
  (:require
    [shadow.api :refer (ns-ready)]
    [shadow.markup.react :as html :refer (defstyled)]
    [shadow.react.component :as comp :refer (deffactory)]
    [shadow.cljs.ui.make-fulcro-happy]
    [fulcro.client :as fc]
    [fulcro.client.routing :as fr :refer (defrouter)]
    [fulcro.client.mutations :as fm :refer (defmutation)]
    [fulcro.client.primitives :as fp :refer (defsc)]
    [fulcro.client.data-fetch :as fdf]
    ))

(defonce app-ref (atom nil))
(defonce state-ref (atom nil))

(defsc Foo [this {:keys [label]}]
  {:initial-state {:page :foo :label "Foo"}
   :query [:page :label]}
  (html/div {} label))

(defsc Bar [this {:keys [label]}]
  {:initial-state {:page :bar :label "Bar"}
   :query [:page :label]}
  (html/div {} label))

(defsc BuildItem [this props]
  {:initial-state (fn [p] p)
   :query [:build-id]
   :ident [:builds/by-id :build-id]}
  (html/div (pr-str props)))

(def ui-build-item (fp/factory BuildItem {:keyfn :build-id}))

(defsc BuildList [this {:keys [builds] :as props}]
  {:initial-state
   (fn [p]
     {:page :build-list
      :builds [(fp/get-initial-state BuildItem {:build-id :foo})]})
   :query
   [:page
    {:builds (fp/get-query BuildItem)}]}

  (html/for [build builds]
    (ui-build-item build)))

(defrouter TopRouter :top-router
  (ident [this props] [(:page props) :top])
  :build-list BuildList
  :foo Foo
  :bar Bar)

(def ui-top-router (fp/factory TopRouter {}))

(defstyled html-nav-items :div [env]
  {:display "flex"
   :margin-bottom 10})

(defstyled html-nav-item :div [env]
  {:flex 1
   :border "1px solid #ccc"
   :margin [0 5]
   :padding 5
   "&.active"
   {:border "1px solid red"}})

(defn nav-item [this top-router id title]
  (html-nav-item
    {:onClick #(fp/transact! this `[(fr/route-to {:handler ~id})])
     :classes {:active (= id (get-in top-router [::fr/current-route :page]))}}
    title))

(defsc Root [this {:keys [top-router] :as props}]
  {:initial-state
   (fn [p]
     (merge
       (fr/routing-tree
         (fr/make-route :build-list [(fr/router-instruction :top-router [:build-list :top])])
         (fr/make-route :foo [(fr/router-instruction :top-router [:foo :top])])
         (fr/make-route :bar [(fr/router-instruction :top-router [:bar :top])]))
       {:top-router (fp/get-initial-state TopRouter {})}))

   :query
   [:ui/react-key
    {:top-router (fp/get-query TopRouter)}]}

  (html/div
    (html-nav-items
      (nav-item this top-router :build-list "build-list")
      (nav-item this top-router :foo "foo")
      (nav-item this top-router :bar "bar"))

    (ui-top-router top-router)))

(defn start []
  (reset! app-ref
    ;; I really don't like putting this in a defonce seems to break often
    (fc/new-fulcro-client
      :started-callback
      (fn [app]
        (js/console.log "fulcro started")
        (fdf/load app :q/builds BuildList {:target [:builds]})
        (when-let [prev-state @state-ref]
          ;; can't figure out how to restore app state otherwise
          (let [app-state (get-in app [:reconciler :config :state])]
            (swap! app-state merge prev-state))))))

  (swap! app-ref fc/mount Root "root"))

(defn stop []
  (reset! state-ref @(get-in @app-ref [:reconciler :config :state])))

(defn get-state []
  @(get-in @app-ref [:reconciler :config :state]))

(defn ^:export init []
  (js/console.log "init")
  (start))

(ns-ready)