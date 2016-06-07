(ns docket.syncer
  (:require [com.stuartsierra.component :as component]
            [overtone.at-at :as at-at]
            [datascript.core :as d]
            [clojure.set :refer [difference intersection]]
            [docket.accessors.instance :as instance]
            [docket.accessors.task-def :as task-def]
            [docket.datascript-utils :as du]
            [docket.query-utils :as qu]
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
         :container-instance/remaining-cpu (instance/remaining-cpu inst)
         :container-instance/used-ports (instance/used-ports inst)))

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
  (d/transact! app-state
               (concat [(get-cluster cluster-arn)]
                       (container-instance-txns app-state cluster-arn)
                       (task-txns app-state cluster-arn))))

(defn ports-available? [used-ports req-ports]
  (empty? (intersection (set used-ports) (set req-ports))))

(defn best-container-instance-for-task [app-state mem cpu ports]
  (ffirst (d/q '[:find ?arn
                 :in $ ?req-mem ?req-cpu ?req-ports ?ports-available?
                 :where
                 [?c :container-instance/remaining-memory ?rem-mem]
                 [?c :container-instance/remaining-cpu ?rem-cpu]
                 [?c :container-instance/used-ports ?used-ports]
                 [(<= ?req-mem ?rem-mem)]
                 [(<= ?req-cpu ?rem-cpu)]
                 [(?ports-available? ?used-ports ?req-ports)]
                 [?c :container-instance/container-instance-arn ?arn]]
               @app-state mem cpu ports ports-available?)))

(defn ecs-start-task [logger cluster-arn container-instance-arn started-by task-def-name]
  (let [res (ecs/start-task {:cluster cluster-arn
                             :container-instances [container-instance-arn]
                             :started-by started-by
                             :task-definition task-def-name})]
    (when (seq (:failures res))
      (logger :info (str "Failed proper scheduling of task: " (pr-str (:failures res)))))))

(defn start-task [logger cluster-arn app-state service]
  (let [service-name (:service/service-name service)
        task-def (:service/task-definition service)
        mem (task-def/memory task-def)
        cpu (task-def/cpu task-def)
        ports (map str (task-def/ports task-def))]
    (if-let [container-instance-arn (best-container-instance-for-task app-state mem cpu ports)]
      (do
        (logger :info (str "Starting task on " container-instance-arn " for " service-name))
        (ecs-start-task logger
                        cluster-arn
                        container-instance-arn
                        (str "docket-svc/" service-name)
                        (:task-definition/name task-def))
        (d/transact! app-state (update-container-instance-txns cluster-arn [container-instance-arn])))
      (logger :info (str "No suitable container instance found for " (:service/service-name service))))))

(defn start-tasks [logger cluster-arn app-state service num-to-start]
  (dotimes [i num-to-start]
    (start-task logger cluster-arn app-state service)))

(defn stop-task [logger cluster-arn app-state service]
  (let [service-name (:service/service-name service)
        task-to-stop-arn (ffirst
                          (d/q '[:find ?arn
                                 :in $ ?started-by
                                 :where
                                 [?t :task/started-by ?started-by]
                                 [?t :task/desired-status "RUNNING"]
                                 [?t :task/task-arn ?arn]]
                               @app-state (str "docket-svc/" service-name)))]
    (if task-to-stop-arn
      (do
        (logger :info (str "Stopping task " task-to-stop-arn "for service " service-name))
        (ecs/stop-task {:cluster cluster-arn :task task-to-stop-arn})
        (d/transact! app-state (update-task-txns cluster-arn [task-to-stop-arn])))
      (logger :info (str "Could not find task to stop for " service-name)))))

(defn stop-tasks [logger cluster-arn app-state service num-to-stop]
  (dotimes [i num-to-stop]
    (stop-task logger cluster-arn app-state service)))

(defn sync-running-tasks [cluster-arn logger app-state]
  (let [snapshot @app-state]
    (doseq [service (qu/qes-by snapshot :service/service-name)]
      (let [desired-count (:service/number-of-tasks service)
            actual-count (-> (d/q '[:find (count ?t)
                                    :in $ ?started-by %
                                    :where
                                    [?t :task/started-by ?started-by]
                                    (scheduled-task ?t)]
                                  snapshot
                                  (str "docket-svc/" (:service/service-name service))
                                  '[[(scheduled-task ?t)
                                     [?t :task/desired-status "RUNNING"]
                                     [?t :task/last-status "RUNNING"]]
                                    [(scheduled-task ?t)
                                     [?t :task/desired-status "RUNNING"]
                                     [?t :task/last-status "PENDING"]]])
                             ffirst
                             (or 0))
            delta (- actual-count desired-count)]
        (cond
          (neg? delta) (start-tasks logger cluster-arn app-state service (- delta))
          (pos? delta) (stop-tasks logger cluster-arn app-state service delta)
          :else :noop)))))

(defn sync-cluster [cluster-arn logger app-state]
  (try
    (sync-state cluster-arn logger app-state)
    (sync-running-tasks cluster-arn logger app-state)
    (catch Exception e
      (logger :error e))))

(defrecord Syncer [cluster-arn logger app-state]
  component/Lifecycle
  (start [c]
    (let [pool (at-at/mk-pool)]
      (at-at/every 10000 #(sync-cluster cluster-arn logger app-state) pool)
      (assoc c :pool pool)))
  (stop [c]
    (when-let [pool (:pool c)]
      (at-at/stop-and-reset-pool! pool)
      (dissoc c :pool))))
