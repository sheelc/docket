(ns docket.core
  (:require [docket.socket :as socket]
            [cljs.reader :refer [read-string]]
            [docket.components.top :refer [top-component]]
            [reagent.core :as r]))

(defonce app (r/atom {}))

(defn on-state-change [new-state]
  (swap! app assoc :state (read-string new-state)))

(defn reset-app []
  (reset! app {:socket (socket/create-socket on-state-change)
               :state {}})
  (r/render-component [top-component (r/cursor app [:state])]
                      (.getElementById js/document "content")))

(defn on-jsload []
  (when-let [socket (:socket @app)]
    (.close socket))
  (reset-app))

(defn ^:export debug-app-state []
  (clj->js @app))

(reset-app)
