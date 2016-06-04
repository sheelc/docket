(ns docket.syncer
  (:require [com.stuartsierra.component :as component]
            [overtone.at-at :as at-at]
            [datascript.core :as d]
            [clojure.set :refer [difference]]
            [docket.accessors.instance :as instance]
            [docket.datascript-utils :as du]
            [amazonica.aws.ecs :as ecs]))

(defn get-cluster [cluster-arn]
  (du/prefix-keys "cluster"
               (first (:clusters (ecs/describe-clusters {:clusters [cluster-arn]})))))

(defn retract-entity [lookup-ref lookup-val]
  [:db.fn/retractEntity [lookup-ref lookup-val]])

(defn resource-txns [app-state ident current-idents get-updates]
  (let [prev-idents (map first (d/q '[:find ?lr :in $ ?ident :where [?e ?ident ?lr]] @app-state ident))
        gone-idents (difference (set prev-idents) (set current-idents))
        updates (get-updates current-idents)
        retracts (map (partial retract-entity ident) gone-idents)]
    (concat updates retracts)))

(defn add-resource-keys [{rem-resources :container-instance/remaining-resources :as inst}]
  (assoc inst
         :container-instance/remaining-memory (instance/remaining-memory inst)
         :container-instance/remaining-cpu (instance/remaining-cpu inst)))

(defn update-container-instance-txns [cluster-arn container-instance-arns]
  (map (comp (partial du/add-refs {:container-instance/cluster (constantly [:cluster/cluster-arn cluster-arn])})
          (partial add-resource-keys)
          (partial du/prefix-keys "container-instance"))
       (:container-instances (ecs/describe-container-instances {:container-instances container-instance-arns
                                                                :cluster cluster-arn}))))

(defn container-instance-txns [app-state cluster-arn]
  (resource-txns app-state
                 :container-instance/container-instance-arn
                 (:container-instance-arns (ecs/list-container-instances {:cluster cluster-arn}))
                 (partial update-container-instance-txns cluster-arn)))

(defn update-task-txns [cluster-arn task-arns]
  (map (comp (partial du/add-refs {:task/cluster (constantly [:cluster/cluster-arn cluster-arn])
                       :task/container-instance (partial du/rel-from-key
                                                   :task/container-instance-arn
                                                   :container-instance/container-instance-arn)})
          (partial du/prefix-keys "task"))
       (:tasks (ecs/describe-tasks {:tasks task-arns
                                    :cluster cluster-arn}))))

(defn task-txns [app-state cluster-arn]
  (resource-txns app-state
                 :task/task-arn
                 (:task-arns (ecs/list-tasks {:cluster cluster-arn}))
                 (partial update-task-txns cluster-arn)))

(defn sync-state [cluster-arn logger app-state]
  (try
    (d/transact! app-state
                 (concat [(get-cluster cluster-arn)]
                         (container-instance-txns app-state cluster-arn)
                         (task-txns app-state cluster-arn)))
    (catch Exception e
      (logger :error e))))

(defrecord Syncer [cluster-arn logger app-state]
  component/Lifecycle
  (start [c]
    (let [pool (at-at/mk-pool)]
      (at-at/every 10000 #(sync-state cluster-arn logger app-state) pool)
      (assoc c :pool pool)))
  (stop [c]
    (when-let [pool (:pool c)]
      (at-at/stop-and-reset-pool! pool)
      (dissoc c :pool))))
