(ns docket.accessors.task-def)

(defn memory [task-def]
  (apply + (map :memory (:task-definition/container-definitions task-def))))

(defn cpu [task-def]
  (apply + (map :cpu (:task-definition/container-definitions task-def))))

(defn ports [task-def]
  (mapcat #(map :host-port (:port-mappings %)) (:task-definition/container-definitions task-def)))
