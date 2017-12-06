(ns shadow.cljs.ui.fulcro-light
  (:require [shadow.react.component :as comp]
            [shadow.cljs.ui.make-fulcro-happy]
            [fulcro.client.impl.protocols :as fprot]
            [fulcro.client.primitives :as fprim]
            [fulcro.client :as fc]))

(defn add-initial-state [comp initial-state-fn]
  (specify! comp
    fprim/InitialAppState
    (initial-state [comp props]
      (initial-state-fn props))))

(defn add-query [comp query]
  (specify! comp
    fprim/IQuery
    (query [comp]
      (if (fn? query)
        (query)
        query))))

(defn mixin
  [{::keys [initial-state query]
    :as config}]
  (-> config


      (assoc ::comp/will-receive-props
             (fn [{:keys [props pending-props] ::keys [reconciler] :as this}]
               (when (implements? fprim/Ident this)
                 (let [ident (fprim/ident this props)
                       next-ident (fprim/ident this pending-props)]
                   (when (not= ident next-ident)
                     (let [idxr (get-in reconciler [:config :indexer])]
                       (when-not (nil? idxr)
                         (swap! (:indexes idxr)
                           (fn [indexes]
                             (-> indexes
                                 (update-in [:ref->components ident] disj this)
                                 (update-in [:ref->components next-ident] (fnil conj #{}) this)))))))))
               this))

      (cond->
        initial-state
        (update ::comp/specify conj #(add-initial-state % initial-state))

        query
        (update ::comp/specify conj #(add-query % query))
        )))

