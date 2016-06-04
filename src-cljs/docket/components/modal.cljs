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

(defn add-service-modal []
  (let [inputs (r/atom {})
        success-response (r/atom {})
        failure-response (r/atom {})]
    (fn []
      (if (seq @success-response)
        [:div.my2.green (:message @success-response)]
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
           [:div.mt2.orange msg])]))))

(defn modal [modal-display]
  (let [inputs (r/atom {})
        success-response (r/atom {})
        failure-response (r/atom {})]
    (fn [modal-display]
      [:div.fixed.top-0.right-0.bottom-0.left-0.flex.items-center.justify-center
       [:div.border.gray-border.box-shadow.bg-white.p3
        [:div.clearfix.border-bottom.mb2.pb1
         [:a.right.pointer.h2
          {:on-click #(reset! modal-display nil)} "×"]]
        [add-service-modal]]])))
