(ns docket.datascript-utils
  (:require [clojure.string :as str]
            [clojure.set :refer [rename-keys]]))

(defn prefix-key [prefix k]
  (keyword (str prefix "/" (str/replace k ":" ""))))

(defn prefix-keys [prefix m]
  (rename-keys m (zipmap (keys m) (map (partial prefix-key prefix) (keys m)))))

(defn rel-from-key [key rel m]
  [rel (key m)])

(defn add-ref [m k f]
  (assoc m k (f m)))

(defn add-refs [refs m]
  (reduce-kv add-ref m refs))
