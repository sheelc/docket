(ns docket.core
  (:gen-class :main true)
  (:require [com.stuartsierra.component :as component]
            [environ.core :refer [env]]
            [docket.system :as system]))

(def the-system nil)

(.addShutdownHook (Runtime/getRuntime)
                  (Thread. (fn [] (when the-system (component/stop the-system)))))

(defn -main [& args]
  (alter-var-root #'the-system (constantly (system/create-system)))
  (component/start the-system))
