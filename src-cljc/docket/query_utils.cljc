(ns docket.query-utils
  (:require [datascript.core :as d]))

;; Convenience helpers for working with datascript from https://github.com/tonsky/datascript-chat/blob/gh-pages/src/datascript_chat/util.cljs

(defn qe
  "If queried entity id, return single entity of first result"
  [q db & sources]
  (->> (apply d/q q db sources)
       ffirst
       (d/entity db)))

(defn qes
  "If queried entity ids, return all entities of result"
  [q db & sources]
  (->> (apply d/q q db sources)
       (map #(d/entity db (first %)))))

(defn qe-by
  "Return single entity by attribute existence or specific value"
  ([db attr]
   (qe '[:find ?e :in $ ?a :where [?e ?a]] db attr))
  ([db attr value]
   (qe '[:find ?e :in $ ?a ?v :where [?e ?a ?v]] db attr value)))

(defn qes-by
  "Return all entities by attribute existence or specific value"
  ([db attr]
   (qes '[:find ?e :in $ ?a :where [?e ?a]] db attr))
  ([db attr value]
   (qes '[:find ?e :in $ ?a ?v :where [?e ?a ?v]] db attr value)))
