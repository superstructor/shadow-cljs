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


(defmutation tx-compile [params]
  (action [{:keys [state]}]
    (js/console.log "tx-compile" params)
    ))

(defsc Foo [this {:keys [label]}]
  {:initial-state {:page :pages/foo
                   :label "Foo"}
   :query [:page :label]}
  (html/div {} label))

(defsc BuildItem [this props]
  {:initial-state (fn [p] p)
   :query (fn [] ['*])
   :ident [:builds/by-id :build-id]}
  (if-not (map? props)
    (html/tr
      (html/td "Loading ..."))
    (let [{:keys [build-id target]} props]
      (html/tr
        (html/td {} (name build-id))
        (html/td {} (name target))
        (html/td {} (html/button {:onClick #(fp/transact! this `[(tx-compile {:id ~build-id :mode :watch})])} "watch"))
        (html/td {} (html/button {:onClick #(fp/transact! this `[(tx-compile {:id ~build-id :mode :dev})])} "compile"))
        (html/td {} (html/button {:onClick #(fp/transact! this `[(tx-compile {:id ~build-id :mode :release})])} "release"))
        ))))

(def ui-build-item (fp/factory BuildItem {:keyfn :build-id}))

(defsc BuildList [this {:keys [builds] :as props}]
  {:initial-state
   (fn [p]
     {:page :pages/builds
      :builds []})
   :query
   [:page
    {:builds (fp/get-query BuildItem)}]}

  (html/div
    (html/h1 "builds")
    (html/table
      (html/tbody
        (html/for [build builds]
          (ui-build-item build))))))

(defrouter TopRouter :top-router
  (ident [this props] [(:page props) :top])
  :pages/builds BuildList
  :pages/foo Foo)

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
    {:onClick (fn [e]
                (js/console.log "onclick" e id)
                (fp/transact! this `[(fr/route-to {:handler ~id})]))
     :classes {:active (= id (get-in top-router [::fr/current-route :page]))}}
    title))

(defsc Root [this {:keys [top-router] :as props}]
  {:initial-state
   (fn [p]
     (merge
       (fr/routing-tree
         (fr/make-route :pages/builds [(fr/router-instruction :top-router [:pages/builds :top])])
         (fr/make-route :pages/foo [(fr/router-instruction :top-router [:pages/foo :top])]))
       {:top-router (fp/get-initial-state TopRouter {})}))

   :query
   [:ui/react-key
    {:top-router (fp/get-query TopRouter)}]}

  (html/div
    (html-nav-items
      (nav-item this top-router :pages/builds "builds")
      (nav-item this top-router :pages/foo "foo"))

    (ui-top-router top-router)))

(defn start []
  (reset! app-ref
    ;; I really don't like putting this in a defonce seems to break often
    (fc/new-fulcro-client
      :started-callback
      (fn [app]
        (when-let [prev-state @state-ref]
          ;; can't figure out how to restore app state otherwise
          (let [app-state (get-in app [:reconciler :config :state])]
            (swap! app-state merge prev-state)))
        (fdf/load app :q/builds BuildItem {:target [:pages/builds :top :builds]}))))

  (swap! app-ref fc/mount Root "root"))

(defn stop []
  (reset! state-ref @(get-in @app-ref [:reconciler :config :state])))

(defn get-state []
  @(get-in @app-ref [:reconciler :config :state]))

(defn ^:export init []
  (js/console.log "init")
  (start))

(ns-ready)