(ns docket.readers
  (:require [cljs-time.format :as f]
            [cljs.reader :as reader]))

(def iso8601-formatter (f/formatters :date-time))

(defn- reader-str->datetime [s]
  (f/parse iso8601-formatter s))

(defn- reader-object->map [o]
  (reader/read-string (str "#" (first o) " \"" (last o) "\"")))

(reader/register-tag-parser! "org.joda.time.DateTime" reader-str->datetime)
(reader/register-tag-parser! "object" reader-object->map)
