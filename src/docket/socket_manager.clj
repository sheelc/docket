(ns docket.socket-manager
  (require [com.stuartsierra.component :as component]
           [datascript.core :as d]
           [org.httpkit.server :as httpkit]))

(defprotocol ISocketManager
  (store-socket! [this k v])
  (clear-socket! [this k])
  (broadcast [this]))

(defrecord SocketManager [storage app-state]
  ISocketManager
  (store-socket! [this k socket]
    (swap! storage assoc k socket)
    (httpkit/send! socket (pr-str @app-state)))
  (clear-socket! [this k]
    (when-let [socket (get @storage k)]
      (when (httpkit/open? socket)
        (httpkit/close socket))
      (swap! storage dissoc k)))
  (broadcast [this]
    (doseq [[k socket] @(:storage this)]
      (when (httpkit/open? socket)
        (httpkit/send! socket (pr-str @app-state)))))
  component/Lifecycle
  (start [c]
    (let [inited (assoc c :storage (atom {}))]
      (d/listen! app-state :socket-manager (fn [{:keys [db-before db-after]}]
                                             (when (not= db-before db-after)
                                               (broadcast inited))))
      inited))
  (stop [c]
    (when-let [storage (:storage c)]
      (doseq [[k socket] @storage]
        (when (httpkit/open? socket)
          (httpkit/close socket)))
      (d/unlisten! app-state :socket-manager)
      (dissoc c :storage))))
