(ns docket.services
  (:require [datascript.core :as d]
            [docket.datascript-utils :as du]))

(defn create-service [app-state params]
  (let [service-name (:service-name params)
        existing-service (ffirst (d/q '[:find ?e :in $ ?sn :where [?e :service/service-name ?sn]]
                                      @app-state service-name))]
    (if existing-service
      {:message (str "Service " service-name " already exists") :success false}
      (do
        (d/transact! app-state (->> (select-keys params [:service-name :number-of-tasks :task-def])
                                    (du/prefix-keys "service")
                                    (conj nil)))
        {:message (str "Service " service-name " created") :success true}))))
