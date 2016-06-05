(ns docket.services
  (:require [datascript.core :as d]
            [amazonica.aws.ecs :as ecs]
            [docket.datascript-utils :as du])
  (:import [com.amazonaws AmazonServiceException]))

(defn add-task-def-name [t]
  (assoc t :name (str (:family t) ":" (:revision t))))

(defn create-service [app-state params]
  (let [service-name (:service-name params)
        existing-service (ffirst (d/q '[:find ?e :in $ ?sn :where [?e :service/service-name ?sn]]
                                      @app-state service-name))]
    (if existing-service
      {:message (str "Service " service-name " already exists") :success false}
      (do
        (try
          (let [task-def (:task-definition (ecs/describe-task-definition {:task-definition (:task-def-name params)}))]
            (d/transact! app-state (concat (->> task-def
                                                add-task-def-name
                                                (du/prefix-keys "task-definition")
                                                (conj nil))
                                           (->> (select-keys params [:service-name :number-of-tasks :task-def-name])
                                                (du/prefix-keys "service")
                                                (du/add-refs {:service/task-definition (partial du/rel-from-key
                                                                                          :service/task-def-name
                                                                                          :task-definition/name)})
                                                (conj nil)))))
          {:message (str "Service " service-name " created") :success true}
          (catch AmazonServiceException e
            {:message "Task def couldn't be fetched, does it exist?" :success false}))))))
