(ns docket.system
  (:require [taoensso.timbre :as timbre]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :as httpkit]
            [docket.config :as config]
            [docket.handler :refer [create-handler]]
            [docket.socket-manager :refer [map->SocketManager]]))

(defrecord AppHandler [logger socket-manager]
  component/Lifecycle
  (start [c]
    (if (:handler c)
      c
      (assoc c :handler (create-handler c))))
  (stop [c] (dissoc c :handler)))

(defn logger [logger-config]
  (fn [level str]
    (timbre/log logger-config level str)))

(defrecord Server [server-opts app]
  component/Lifecycle
  (start [c]
    (assoc c :server-stop (httpkit/run-server (:handler app) server-opts)))
  (stop [c]
    (when-let [server-stop (:server-stop c)]
      (server-stop)
      (dissoc c :server-stop))))

(defn system-map [config]
  (component/system-map
   :logger (logger (config :logging))
   :app-state (atom {:instances [{:id "first instance"}]})
   :app-handler (map->AppHandler {})
   :embedded-server (map->Server (select-keys config [:server-opts]))
   :socket-manager (map->SocketManager {})))

(defn dependency-map []
  {:app-handler [:logger :socket-manager]
   :socket-manager [:app-state]
   :embedded-server {:app :app-handler}})

(defn create-system
  ([] (create-system {}))
  ([config-overrides]
   (let [config (config/system-config config-overrides)]
     (component/system-using
      (system-map config)
      (dependency-map)))))
