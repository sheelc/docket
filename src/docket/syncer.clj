(ns docket.syncer
  (:require [com.stuartsierra.component :as component]
            [overtone.at-at :as at-at]
            [datascript.core :as d]
            [clojure.set :refer [rename-keys]]
            [clojure.string :as str]
            [amazonica.aws.ecs :as ecs]))

(defn prefix-key [prefix k]
  (keyword (str prefix "/" (str/replace k ":" ""))))

(defn prefix-keys [prefix m]
  (rename-keys m (zipmap (keys m) (map (partial prefix-key prefix) (keys m)))))

(defn rel-from-key [key rel m]
  [rel (key m)])

(defn add-ref [m k f]
  (assoc m k (f m)))

(defn add-refs [refs m]
  (reduce-kv add-ref m refs))

(defn get-cluster [cluster-arn]
  (prefix-keys "cluster"
               (first (:clusters (ecs/describe-clusters {:clusters [cluster-arn]})))))

(defn get-container-instances [cluster-arn]
  (let [instance-ids (:container-instance-arns (ecs/list-container-instances {:cluster cluster-arn}))]
    (map (comp (partial add-refs {:container-instance/cluster (constantly [:cluster/cluster-arn cluster-arn])})
            (partial prefix-keys "container-instance"))
         (:container-instances (ecs/describe-container-instances {:container-instances instance-ids
                                                                  :cluster cluster-arn})))))

(defn get-tasks [cluster-arn]
  (let [task-arns (:task-arns (ecs/list-tasks {:cluster cluster-arn}))]
    (map (comp (partial add-refs {:task/cluster (constantly [:cluster/cluster-arn cluster-arn])
                         :task/container-instance (partial rel-from-key
                                                     :task/container-instance-arn
                                                     :container-instance/container-instance-arn)})
            (partial prefix-keys "task"))
         (:tasks (ecs/describe-tasks {:tasks task-arns
                                      :cluster cluster-arn})))))

(defn sync-state [cluster-arn logger app-state]
  (try
    (d/transact! app-state
                 (concat [(get-cluster cluster-arn)]
                         (get-container-instances cluster-arn)
                         (get-tasks cluster-arn)))
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
