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
    [cljs.pprint :refer (pprint)]
    [shadow.dom :as dom]
    ["react-dom" :as rdom]))

(defonce app-ref (atom nil))
(defonce state-ref (atom nil))


(defmutation tx-build-action [params]
  (action [{:keys [state]}]
    (js/console.log "tx-compile" params))
  (remote [{:keys [ast]}]
    ast))

(defmutation tx-select-build [{:keys [id]}]
  (action [{:keys [state]}]
    (swap! state assoc-in [:pages/builds :top :selected-build] [:builds/by-id id])))

(defsc Foo [this {:keys [label]}]
  {:initial-state {:page :pages/foo
                   :label "Foo"}
   :query [:page :label]}
  (html/div {} label))

(defstyled html-build-item :div [env]
  {:cursor "pointer"
   :padding [2 0]
   "&:hover, &.selected"
   {:font-weight "bold"}})

(defsc BuildItem [this props]
  {:initial-state (fn [p] p)
   :query (fn [] ['*])
   :ident [:builds/by-id :build-id]}
  (if-not (map? props)
    (html/div "Loading ...")
    (let [{:keys [build-id target]} props
          {:keys [on-click selected]} (fp/get-computed props)]
      (html-build-item
        {:classes {:selected selected}
         :onClick on-click}
        (name build-id))
      )))

(def ui-build-item (fp/factory BuildItem {:keyfn :build-id}))

(defstyled section-header :div [env]
  {:font-weight "bold"
   :font-size "1.2em"
   :margin-bottom 10})

(defsc BuildDetail [this props]
  {:query (fn [] ['*])
   :ident [:builds/by-id :build-id]}
  (let [{:keys [build-id]} props]
    (html/div
      (section-header (name build-id))
      (html/div
        (html/button {:onClick #(fp/transact! this `[(tx-build-action {:id ~build-id :action :watch})])} "watch")
        (html/button {:onClick #(fp/transact! this `[(tx-build-action {:id ~build-id :action :compile})])} "compile")
        (html/button {:onClick #(fp/transact! this `[(tx-build-action {:id ~build-id :action :release})])} "release"))
      (html/pre
        (with-out-str
          (pprint props))))))

(def ui-build-detail (fp/factory BuildDetail {:keyfn :build-id}))

(defstyled html-split :div [env]
  {:display "flex"})

(defstyled html-build-list-container :div [env]
  {:width 170})

(defstyled html-build-detail-container :div [env]
  {:flex 1})

(defsc BuildList [this props]
  {:initial-state
   (fn [p]
     {:page :pages/builds
      :builds []
      :selected-build nil})
   :query
   [:page
    {:builds (fp/get-query BuildItem)}
    {:selected-build (fp/get-query BuildDetail)}]}

  (let [{:keys [builds selected-build]} props]
    (html-split
      (html-build-list-container
        (section-header "builds")
        (html/for [build builds]
          (ui-build-item
            (fp/computed build
              {:selected (and selected-build (= (:build-id build) (:build-id selected-build)))
               :on-click #(fp/transact! this `[(tx-select-build {:id ~(:build-id build)})])
               }))))
      (html-build-detail-container
        (if-not selected-build
          (html/div
            (section-header "no build selected")
            (html/div "<- select a build"))
          (ui-build-detail selected-build))))))

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
      :mutation-merge
      (fn [state msym {:keys [updates] :as return-value}]
        (reduce
          (fn [state {:keys [ident value]}]
            (update-in state ident merge value))
          state
          updates))

      :started-callback
      (fn [app]
        (when-let [prev-state @state-ref]
          ;; can't figure out how to restore app state otherwise
          (let [app-state (get-in app [:reconciler :config :state])]
            (swap! app-state merge prev-state)))

        #_(fulcro.client.primitives/transact! (:reconciler (fulcro.inspect.core/global-inspector))
            [:fulcro.inspect.core/floating-panel "main"]
            [`(fulcro.client.mutations/set-props {:ui/visible? true})])

        (fdf/load app :q/builds BuildItem {:target [:pages/builds :top :builds]}))))

  (swap! app-ref fc/mount Root "root"))

(defn stop []
  (reset! state-ref @(get-in @app-ref [:reconciler :config :state]))
  (rdom/unmountComponentAtNode (dom/by-id "root")))

(defn get-state []
  @(get-in @app-ref [:reconciler :config :state]))

(defn ^:export init []
  (js/console.log "init")
  (start))

(ns-ready)