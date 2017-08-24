(ns datoms-say-what
  (:require [datomic.api :as d]
            [datoms-say-what.analysis :as analysis]
            [datoms-say-what.viz :as viz]))

(defn visualize-tx-results
  [tx-results]
  (viz/visualize-tx
   (analysis/analyze-tx
    (:db-before tx-results)
    (:db-after tx-results)
    (:tx-data tx-results))))

(comment

  (def uri "datomic:mem://example2")
  (d/create-database uri)
  (def conn (d/connect uri))

  ;; basic transaction
  (d/transact conn [{:db/id (d/tempid :db.part/user) :db/ident :enum}])

  ;; adding schema
  (def schema
    [{:db/id (d/tempid :db.part/db) :db/ident :user/name :db/valueType :db.type/string :db/cardinality :db.cardinality/one}
     {:db/id (d/tempid :db.part/db) :db/ident :user/tokens :db/valueType :db.type/long :db/cardinality :db.cardinality/many}
     {:db/id (d/tempid :db.part/db) :db/ident :user/active? :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}])
  (def result (d/transact conn schema))

  (def users
    [{:db/id (d/tempid :db.part/user) :user/name "alice@example.com" :user/tokens #{100 101 102} :user/active? true}
     {:db/id (d/tempid :db.part/user) :user/name "bob@fast.co"       :user/tokens #{}            :user/active? true}
     {:db/id (d/tempid :db.part/user) :user/name "eve@attacker.zz"   :user/tokens #{100}         :user/active? false}])

  (def result2 (d/transact conn users))

  (analyze-tx-data (:db-before @result2) (:db-after @result2) (:tx-data @result2))

  (spit "tx.svg"
        (viz/visualize-tx
         (analysis/analyze-tx
          (:db-before @result2)
          (:db-after @result2)
          (:tx-data @result2))))


  )
