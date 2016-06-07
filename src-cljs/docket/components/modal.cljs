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

(defn add-service-modal [args]
  (let [inputs (r/atom {})
        success-response (r/atom {})
        failure-response (r/atom {})]
    (fn [args]
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
         [:input.h3.block {:type "text" :on-change (partial store-input inputs :task-def-name)}]

         [:input.mt3 {:type "submit" :value "Create!"}]
         (when-let [msg (:message @failure-response)]
           [:div.mt2.orange msg])]))))

(defn update-service-modal [{:keys [service]}]
  (let [inputs (r/atom {:number-of-tasks (:service/number-of-tasks service)})
        success-response (r/atom {})
        failure-response (r/atom {})]
    (fn [{:keys [service]}]
      (let [service-name (:service/service-name service)]
        (if (seq @success-response)
          [:div.my2.green (:message @success-response)]
          [:div
           [:div.h3 "Updating service " service-name]
           [:form
            {:on-submit (partial prevent-default
                           #(api/update-service
                             (partial store-response success-response)
                             (partial store-response failure-response)
                             (merge @inputs {:service-name service-name})))}

            [:label.h3.block.mt3.mb1 "Desired Number of Tasks"]
            [:input.h3.block {:type "number" :min 0 :value (:number-of-tasks @inputs)
                              :on-change (partial store-input inputs :number-of-tasks)}]

            [:input.mt3 {:type "submit" :value "Update!"}]
            (when-let [msg (:message @failure-response)]
              [:div.mt2.orange msg])]])))))

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
        (let [[modal-display-style modal-display-args] @modal-display]
          (condp = modal-display-style
            :add-service [add-service-modal modal-display-args]
            :update-service [update-service-modal modal-display-args]))]])))
