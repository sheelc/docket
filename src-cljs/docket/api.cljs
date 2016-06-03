(ns docket.api
  (:require [ajax.core :refer [POST json-response-format]]))

(defn create-service [success-cb failure-cb params]
  (POST "/api/create-service"
        {:params params
         :format :json
         :response-format (json-response-format {:keywords? true})
         :handler success-cb
         :error-handler (comp failure-cb :response)}))
