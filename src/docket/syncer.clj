(ns docket.syncer
  (:require [com.stuartsierra.component :as component]
            [overtone.at-at :as at-at]
            [amazonica.aws.ecs :as ecs]))

(defn get-cluster [logger cluster-arn]
  (first (:clusters (ecs/describe-clusters {:clusters [cluster-arn]}))))

(defn get-container-instances [logger cluster-arn]
  (let [instance-ids (:container-instance-arns (ecs/list-container-instances {:cluster cluster-arn}))]
    (:container-instances (ecs/describe-container-instances {:container-instances instance-ids
                                                             :cluster cluster-arn}))))

(defn sync-state [logger app-state]
  (try
    (let [cluster-arn (get-in @app-state [:cluster :cluster-arn])]
      (reset! app-state
              {:cluster (get-cluster logger cluster-arn)
               :container-instances (get-container-instances logger cluster-arn)}))
    (catch Exception e
      (logger :error e))))

(defrecord Syncer [logger app-state]
  component/Lifecycle
  (start [c]
    (let [pool (at-at/mk-pool)]
      (at-at/every 10000 #(sync-state logger app-state) pool)
      (assoc c :pool pool)))
  (stop [c]
    (when-let [pool (:pool c)]
      (at-at/stop-and-reset-pool! pool)
      (dissoc c :pool))))
