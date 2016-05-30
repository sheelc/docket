(ns docket.schema)

(def schema {:cluster/cluster-arn {:db/unique :db.unique/identity}

             :container-instance/container-instance-arn {:db/unique :db.unique/identity}
             :container-instance/cluster {:db/valueType :db.type/ref}

             :task/task-arn {:db/unique :db.unique/identity}
             :task/cluster {:db/valueType :db.type/ref}
             :task/container-instance {:db/valueType :db.type/ref}})
