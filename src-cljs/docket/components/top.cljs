(ns docket.components.top)

(defn instance-item [instance]
  [:li (:id instance)])

(defn top-component [state]
  [:div
   [:h1 "Docket"]
   [:h2 "Instances"
    [:ol
     (for [instance (:instances @state)]
       ^{:key (:id instance)} [instance-item instance])]]])
