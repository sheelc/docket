(ns docket.accessors.instance)

(defn find-resource [name resources]
  (first (filter (comp (partial = name) :name) resources)))

(def remaining-memory (comp :integer-value (partial find-resource "MEMORY") :container-instance/remaining-resources))
(def remaining-cpu (comp :integer-value (partial find-resource "CPU") :container-instance/remaining-resources))
