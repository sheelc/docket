(ns docket.components.modal
  (:require [reagent.core :as r]
            [docket.api :as api]))

(defn store-input [inputs k e]
  (swap! inputs assoc k (.. e -target -value)))

(defn store-response [res-atom res]
  (reset! res-atom res))

(defn prevent-default [f e]
  (.preventDefault e)
  (f))

(defn modal [modal-display]
  (let [inputs (r/atom {})
        success-response (r/atom {})
        failure-response (r/atom {})]
    (fn [modal-display]
      [:div.absolute.body-centered.border.gray-border.box-shadow.bg-white.p3
       (if (seq @success-response)
         [:div
          [:div.my2.green (pr-str (:message @success-response))]
          [:a.block.underline.pointer
           {:on-click #(reset! modal-display nil)}
           "Done"]]
         [:form
          {:on-submit (partial prevent-default
                         #(api/create-service
                           (partial store-response success-response)
                           (partial store-response failure-response)
                           @inputs))}
          [:label.h3.block.mb1 "Service Name"]
          [:input.h3.block {:type "text" :on-change (partial store-input inputs :service-name)}]

          [:label.h3.block.mt3.mb1 "Desired Number of Tasks"]
          [:input.h3.block {:type "number" :min 0 :on-change (partial store-input inputs :number-of-tasks)}]

          [:label.h3.block.mt3.mb1 "Task Definition"]
          [:input.h3.block {:type "text" :on-change (partial store-input inputs :task-def)}]

          [:input.mt3 {:type "submit" :value "Create!"}]
          (when-let [msg (:message @failure-response)]
            [:div.mt2.orange (pr-str msg)])])])))
