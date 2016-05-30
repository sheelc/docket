(ns docket.core
  (:require [docket.socket :as socket]
            [cljs.reader :refer [read-string]]
            [docket.readers :as docket-readers]
            [datascript.core :as d]
            [docket.components.top :refer [top-component]]
            [reagent.core :as r]))

(defonce app (r/atom {}))

(defn on-state-change [new-db]
  (swap! app assoc :db (d/conn-from-db (read-string new-db)))
  (r/force-update-all))

(defn reset-app []
  (reset! app {:socket (socket/create-socket on-state-change)
               :db (d/create-conn {})})
  (r/render-component [top-component app]
                      (.getElementById js/document "content")))

(defn on-jsload []
  (when-let [socket (:socket @app)]
    (.close socket))
  (reset-app))

(defn ^:export debug-app-state []
  (clj->js @app))

(reset-app)
