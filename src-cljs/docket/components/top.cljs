(ns docket.components.top
  (:require [reagent.core :as r]))


(defn cluster-summary [cluster]
  [:div
   [:h1 "Cluster " (:cluster-name @cluster)]
   [:div
    [:div "Running tasks: " (:running-tasks-count @cluster)]
    [:div "Pending tasks: " (:pending-tasks-count @cluster)]
    [:div "Registered container instances: " (:registered-container-instances-count @cluster)]]])

(defn instance-item [instance]
  [:tr
   [:td (:ec2instance-id instance)]
   [:td (:running-tasks-count instance)]])

(defn instances-list [instances]
  [:div
   [:h2 "Instances"]
   [:table
    [:thead
     [:td "EC2 Instance Id"]
     [:td "Running Tasks Count"]]
    [:tbody
     (for [instance @instances]
       ^{:key (:ec2instance-id instance)} [instance-item instance])]]])

(defn top-component [state]
  [:div
   [cluster-summary (r/cursor state [:cluster])]
   [instances-list (r/cursor state [:container-instances])]])
