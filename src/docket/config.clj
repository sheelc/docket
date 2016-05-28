(ns docket.config
  (:require [environ.core :refer [env]]
            [taoensso.timbre :as timbre]))

(defn development? []
  (= (env :environment) "development"))


(def default-config {:server-opts {:port 3012}
                     :logging
                     (merge (timbre/get-default-config)
                            {:appenders
                             {:standard-out
                              (get-in timbre/example-config [:appenders :standard-out])}})})

(def env-config {:environment (env :environment)})

(defn deep-merge
  [& maps]
  (if (every? map? maps)
    (apply merge-with deep-merge maps)
    (last maps)))

(defn system-config [overrides]
  (deep-merge default-config env-config overrides))
