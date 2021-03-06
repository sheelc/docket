(ns docket.system
  (:require [taoensso.timbre :as timbre]
            [com.stuartsierra.component :as component]
            [org.httpkit.server :as httpkit]
            [datascript.core :as d]
            [docket.config :as config]
            [docket.handler :refer [create-handler]]
            [docket.syncer :refer [map->Syncer]]
            [docket.schema :refer [schema]]
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
   :app-state (d/create-conn schema)
   :app-handler (map->AppHandler {})
   :embedded-server (map->Server (select-keys config [:server-opts]))
   :syncer (map->Syncer (select-keys config [:cluster-arn]))
   :socket-manager (map->SocketManager {})))

(defn dependency-map []
  {:app-handler [:logger :socket-manager :app-state]
   :socket-manager [:app-state]
   :syncer [:logger :app-state]
   :embedded-server {:app :app-handler}})

(defn create-system
  ([] (create-system {}))
  ([config-overrides]
   (let [config (config/system-config config-overrides)]
     (component/system-using
      (system-map config)
      (dependency-map)))))
