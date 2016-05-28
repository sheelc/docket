(ns docket.socket)

(defn create-socket [on-state-change]
  (let [loc (.-location js/window)]
    (let [conn (js/WebSocket. (str "ws://" loc.host "/socket"))]
      (set! (.-onmessage conn)
            (fn [event]
              (on-state-change (.-data event))))
      conn)))
