(ns datoms-say
  (:require [datomic.api :as d]
            [datoms-say.analysis :as analysis]
            [datoms-say.viz :as viz]))

(defn visualize-tx-results
  [tx-results]
  (viz/visualize-tx
   (analysis/analyze-tx
    (:db-before tx-results)
    (:db-after tx-results)
    (:tx-data tx-results))))

(def what? visualize-tx-results)

(comment

  (def uri "datomic:mem://example4")
  (d/create-database uri)
  (def conn (d/connect uri))

  ;; basic transaction
  (d/transact conn [{:db/id (d/tempid :db.part/user) :db/ident :enum}])

  ;; adding schema
  (def schema
    [{:db/ident :user/name    :db/valueType :db.type/string  :db/cardinality :db.cardinality/one}
     {:db/ident :user/tokens  :db/valueType :db.type/long    :db/cardinality :db.cardinality/many}
     {:db/ident :user/active? :db/valueType :db.type/boolean :db/cardinality :db.cardinality/one}
     {:db/ident :account/id   :db/valueType :db.type/long    :db/cardinality :db.cardinality/one}
     {:db/ident :account/tier :db/valueType :db.type/ref     :db/cardinality :db.cardinality/one}
     {:db/ident :user/account :db/valueType :db.type/ref     :db/cardinality :db.cardinality/many}
     {:db/ident :account.tier/free}
     {:db/ident :account.tier/bronze}
     {:db/ident :account.tier/silver}
     {:db/ident :account.tier/gold}
     {:db/ident :account.tier/enterprise}])
  (def result (d/transact conn schema))

  (def users
    [{:user/name "alice@example.com" :user/tokens #{100 101 102} :user/active? true  :user/account #{"alice"}}
     {:user/name "bob@fast.co"       :user/tokens #{}            :user/active? true  :user/account #{"bob" "bob2"}}
     {:user/name "eve@attacker.zz"   :user/tokens #{100}         :user/active? false :user/account #{"eve"}}
     {:db/id "alice" :account/id 1 :account/tier :account.tier/free}
     {:db/id "eve"   :account/id 2 :account/tier :account.tier/enterprise}
     {:db/id "bob"   :account/id 3 :account/tier :account.tier/free}
     {:db/id "bob2"  :account/id 4 :account/tier :account.tier/silver}])

  (def result2 (d/transact conn users))

  (analyze-tx-data (:db-before @result2) (:db-after @result2) (:tx-data @result2))

  (spit "schema.svg" (datoms-say/what? @result))
  (spit "tx.svg" (datoms-say/what? @result2))


  )
