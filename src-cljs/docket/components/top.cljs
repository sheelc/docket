(ns docket.components.top
  (:require [reagent.core :as r]
            [docket.query-utils :as qu]
            [docket.components.modal :refer [modal]]
            [clojure.string :as str]
            [datascript.core :as d]
            [docket.accessors.instance :as instance]))

(defn cluster-summary [cluster]
  [:div.p2.bg-off-white
   [:div.h1.mb2 "Cluster " (:cluster/cluster-name cluster)]
   [:div
    [:div "Running tasks: " (:cluster/running-tasks-count cluster)]
    [:div "Pending tasks: " (:cluster/pending-tasks-count cluster)]
    [:div "Registered container instances: " (:cluster/registered-container-instances-count cluster)]]])

(defn instance-item [db instance]
  (let [expanded (r/atom false)]
    (fn [db instance]
      [:div
       [:a.block.clearfix.my1.pointer
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
        [:div.col.col-1 (:container-instance/pending-tasks-count instance)]
        [:div.col.col-1 (:container-instance/running-tasks-count instance)]
        [:div.col.col-2 (instance/remaining-memory instance)]]
       (when @expanded
         [:div.my1.ml3.p1.bg-off-white
          [:div.my1.underline "Tasks"]
          (for [task (qu/qes '[:find ?e
                               :in $ ?c
                               :where [?e :task/container-instance ?c]]
                             @db (:db/id instance))]
            ^{:key (:task/task-arn task)}
            [:div (last (str/split (:task/task-definition-arn task) "/"))])])])))

(defn instances-list [db]
  [:div.my3
   [:div.h2 "Instances"]
   [:div.clearfix
    [:div.my1.clearfix.border-bottom
     [:div.col.col-2 "EC2 Instance Id"]
     [:div.col.col-2 "Status"]
     [:div.col.col-2 "Agent Version"]
     [:div.col.col-2 "Docker Version"]
     [:div.col.col-1 "Pending"]
     [:div.col.col-1 "Running"]
     [:div.col.col-2 "Memory Remaining"]]
    (for [instance (qu/qes-by @db :container-instance/container-instance-arn)]
      ^{:key (:container-instance/container-instance-arn instance)} [instance-item db instance])]])

(defn service-item [service]
  [:div.clearfix.my1
   [:div.col.col-2 (:service/service-name service)]
   [:div.col.col-2 (:service/number-of-tasks service)]
   [:div.col.col-2 (:service/task-def service)]])

(defn services-list [db modal-display]
  [:div.my3
   [:div.clearfix
    [:a.h2.inline-block "Services"]
    [:a.ml2.pointer
     {:on-click #(reset! modal-display :add-service)}
     "+ Add Service"]]
   [:div.clearfix.my1.border-bottom
    [:div.col.col-2 "Service Name"]
    [:div.col.col-2 "Task Definition"]
    [:div.col.col-2 "Desired number of Tasks"]]
   (for [service (qu/qes-by @db :service/service-name)]
     ^{:key (:service/service-name service)} [service-item service])])

(defn top-component [{:keys [db] :as app}]
  (let [db (:db @app)
        modal-display (r/cursor app [:ui :modal-display])
        cluster (qu/qe-by @db :cluster/cluster-arn)]
    (when (seq cluster)
      [:div.p4.relative
       [cluster-summary cluster]
       [instances-list db]
       [services-list db modal-display]
       (when @modal-display [:div.fixed.top-0.left-0.right-0.bottom-0.bg-light-gray.semi-transparent])
       (when @modal-display [modal modal-display])])))
