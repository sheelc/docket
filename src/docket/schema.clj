(ns docket.schema)

(def schema {:cluster-arn {:db/unique :db.unique/identity}
             :ec2instance-id {:db/unique :db.unique/identity}})
