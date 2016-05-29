(ns docket.components.top
  (:require [reagent.core :as r]
            [datascript.core :as d]))


(defn cluster-summary [cluster]
  [:div
   [:h1 "Cluster " (:cluster-name cluster)]
   [:div
    [:div "Running tasks: " (:running-tasks-count cluster)]
    [:div "Pending tasks: " (:pending-tasks-count cluster)]
    [:div "Registered container instances: " (:registered-container-instances-count cluster)]]])

(defn instance-item [instance]
  [:tr
   [:td (:ec2instance-id instance)]
   [:td (:running-tasks-count instance)]])

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
       ^{:key (:ec2instance-id instance)} [instance-item instance])]]])

(defn top-component [app]
  (let [db (:db @app)
        cluster (d/q '[:find ?e :where [?e :cluster-arn]] @db)]
    (when (seq cluster)
      [:div
       [cluster-summary (d/pull @db '[*] (ffirst cluster))]
       [instances-list (d/pull-many @db '[*] (map first (d/q '[:find ?e :where [?e :ec2instance-id]] @db)))]])))
