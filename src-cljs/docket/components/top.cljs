(ns docket.components.top
  (:require [reagent.core :as r]
            [docket.query-utils :as qu]
            [clojure.string :as str]
            [datascript.core :as d]))

(defn cluster-summary [cluster]
  [:div.p2.bg-off-white
   [:div.h1.mb2 "Cluster " (:cluster/cluster-name cluster)]
   [:div
    [:div "Running tasks: " (:cluster/running-tasks-count cluster)]
    [:div "Pending tasks: " (:cluster/pending-tasks-count cluster)]
    [:div "Registered container instances: " (:cluster/registered-container-instances-count cluster)]]])

(defn find-resource [name resources]
  (first (filter (comp (partial = name) :name) resources)))

(def remaining-memory (comp :integer-value (partial find-resource "MEMORY") :container-instance/remaining-resources))

(defn instance-item [db instance]
  (let [expanded (r/atom false)]
    (fn [db instance]
      [:div
       [:div.clearfix.my1.pointer
        {:on-click #(swap! expanded not)}
        [:div.col.col-2
         (if @expanded
           [:div.mr2.inline-block.triangle.triangle-down {:style {:border-width "8.7px 5px 0 5px"}}]
           [:div.mr2.inline-block.triangle.triangle-right {:style {:border-width "5px 0 5px 8.7px" :margin-left "2px"}}])
         (:container-instance/ec2instance-id instance)]
        (let [status (:container-instance/status instance)]
          [:div.col.col-2 {:class (if (= "ACTIVE" status) "green" "orange")} status])
        [:div.col.col-2 (:agent-version (:container-instance/version-info instance))]
        [:div.col.col-2 (:docker-version (:container-instance/version-info instance))]
        [:div.col.col-2 (:container-instance/running-tasks-count instance)]
        [:div.col.col-2 (remaining-memory instance)]]
       (when @expanded
         [:div.my1.ml3.p1.bg-off-white
          [:div.my1.underline "Tasks"]
          (for [task (qu/qes '[:find ?e
                               :in $ ?c
                               :where [?e :task/container-instance ?c]]
                             @db (:db/id instance))]
            ^{:key (:task/task-arn task)}
            [:div (last (str/split (:task/task-definition-arn task) "/"))])])])))

(defn instances-list [db instances]
  [:div.my3
   [:div.h2 "Instances"]
   [:div.clearfix
    [:div.my1.clearfix.border-bottom
     [:div.col.col-2 "EC2 Instance Id"]
     [:div.col.col-2 "Status"]
     [:div.col.col-2 "Agent Version"]
     [:div.col.col-2 "Docker Version"]
     [:div.col.col-2 "Running Tasks Count"]
     [:div.col.col-2 "Memory Remaining"]]
    (for [instance instances]
      ^{:key (:container-instance/container-instance-arn instance)} [instance-item db instance])]])

(defn top-component [app]
  (let [db (:db @app)
        cluster (qu/qe-by @db :cluster/cluster-arn)]
    (when (seq cluster)
      [:div.p4
       [cluster-summary cluster]
       [instances-list db (qu/qes-by @db :container-instance/container-instance-arn)]])))
