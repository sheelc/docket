(ns docket.socket-manager
  (require [com.stuartsierra.component :as component]
           [org.httpkit.server :as httpkit]))

(defprotocol ISocketManager
  (store-socket! [this k v])
  (clear-socket! [this k])
  (broadcast [this val]))

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
  (broadcast [this val]
    (doseq [[k socket] @(:storage this)]
      (when (httpkit/open? socket)
        (httpkit/send! socket (pr-str val)))))
  component/Lifecycle
  (start [c]
    (let [inited (assoc c :storage (atom {}))]
      (add-watch app-state :socket-manager (fn [_ _ _ new-state]
                                             (broadcast inited new-state)))
      inited))
  (stop [c]
    (when-let [storage (:storage c)]
      (doseq [[k socket] @storage]
        (when (httpkit/open? socket)
          (httpkit/close socket)))
      (remove-watch app-state :socket-manager)
      (dissoc c :storage))))
