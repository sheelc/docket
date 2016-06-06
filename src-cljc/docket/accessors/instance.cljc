(ns docket.accessors.instance)

(defn find-resource [name resources]
  (first (filter (comp (partial = name) :name) resources)))

(def remaining-memory (comp :integer-value (partial find-resource "MEMORY") :container-instance/remaining-resources))
(def remaining-cpu (comp :integer-value (partial find-resource "CPU") :container-instance/remaining-resources))

;; Proper used ports comes out of remaining resources for some reason
(def used-ports (comp :string-set-value (partial find-resource "PORTS") :container-instance/remaining-resources))
