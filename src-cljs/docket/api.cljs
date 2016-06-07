(ns docket.api
  (:require [ajax.core :refer [POST json-response-format]]))

(defn post-req [path success-cb failure-cb params]
  (POST path
        {:params params
         :format :json
         :response-format (json-response-format {:keywords? true})
         :handler success-cb
         :error-handler (comp failure-cb :response)}))

(def create-service (partial post-req "/api/create-service"))
(def update-service (partial post-req "/api/update-service"))
