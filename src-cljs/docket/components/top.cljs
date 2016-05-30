(ns docket.components.top
  (:require [reagent.core :as r]
            [docket.query-utils :as qu]
            [datascript.core :as d]))


(defn cluster-summary [cluster]
  [:div
   [:h1 "Cluster " (:cluster/cluster-name cluster)]
   [:div
    [:div "Running tasks: " (:cluster/running-tasks-count cluster)]
    [:div "Pending tasks: " (:cluster/pending-tasks-count cluster)]
    [:div "Registered container instances: " (:cluster/registered-container-instances-count cluster)]]])

(defn instance-item [instance]
  [:tr
   [:td (:container-instance/ec2instance-id instance)]
   [:td (:container-instance/running-tasks-count instance)]])

(defn instances-list [instances]
  [:div
   [:h2 "Instances"]
   [:table
    [:thead
     [:tr
      [:th "EC2 Instance Id"]
      [:th "Running Tasks Count"]]]
    [:tbody
     (for [instance instances]
       ^{:key (:container-instance/container-instance-arn instance)} [instance-item instance])]]])

(defn task-item [task]
  [:tr
   [:td (:task/task-arn task)]])

(defn tasks-list [tasks]
  [:div
   [:h2 "Tasks"]
   [:table
    [:thead
     [:tr]]
    [:tbody
     (for [task tasks]
       ^{:key (:task/task-arn task)} [task-item task])]]])

(defn top-component [app]
  (let [db (:db @app)
        cluster (qu/qe-by @db :cluster/cluster-arn)]
    (when (seq cluster)
      [:div
       [cluster-summary cluster]
       [instances-list (qu/qes-by @db :container-instance/container-instance-arn)]
       [tasks-list (qu/qes-by @db :task/task-arn)]])))
