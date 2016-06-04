(ns docket.handler
  (:require [docket.config :as config]
            [compojure.core :refer :all]
            [compojure.route :as route]
            [ring.util.response :refer [redirect response status content-type]]
            [docket.socket-manager :refer [store-socket! clear-socket!]]
            [docket.services :as services]
            [org.httpkit.server :refer [with-channel on-close send!]]
            [ring.middleware.json :refer [wrap-json-params wrap-json-response]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [noir-exception.core :refer [wrap-exceptions]]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(defn create-socket [sockets logger req]
  (let [socket-key (uuid)]
    (with-channel req channel
      (on-close channel (fn [_]
                          (logger :info (str "socket connection " socket-key " closed"))
                          (clear-socket! sockets socket-key)))
      (logger :info (str "socket connection " socket-key " opened"))
      (store-socket! sockets socket-key channel))))

(defn res->api-resp [res]
  (let [api-resp (dissoc res :success)]
    (if (:success res)
      (response api-resp)
      (status (response api-resp) 400))))

(defn create-handler
  ([] (create-handler {}))
  ([{:keys [logger socket-manager app-state] :as ctx}]
   (-> (routes (GET "/healthcheck" [] "cool beans")
               (GET "/" [] (redirect "/index.html"))
               (GET "/socket" req (create-socket socket-manager logger req))
               (context "/api" []
                        (POST "/create-service" {params :params}
                              (res->api-resp (services/create-service app-state params))))
               (route/resources "/")
               (route/not-found "Not found"))
       (wrap-json-response)
       (wrap-keyword-params)
       (wrap-json-params)
       (wrap-exceptions))))
