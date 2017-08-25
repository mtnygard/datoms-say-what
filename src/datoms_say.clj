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

  ;; We need a database. Just use in memory for the commentorial
  (def uri "datomic:mem://commentorial")
  (d/create-database uri)
  (def conn (d/connect uri))

  ;; Create a schema. This isn't a very good data model. It's meant to
  ;; have a representative set of attribute types: multivalued and
  ;; single valued, references and primitives.
  (def schema
    [{:db/ident :user/name    :db/valueType :db.type/string  :db/cardinality :db.cardinality/one  :db/unique :db.unique/identity}
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

  ;; We capture the result of the transaction. You can make an image
  ;; out of this to see the schema's entities
  (def result (d/transact conn schema))
  (spit "doc/commentorial1.svg" (datoms-say/what? @result))

  ;; We can add some data using those attributes.
  (def users
    [{:user/name "alice@example.com" :user/tokens #{100 101 102} :user/active? true  :user/account #{"alice"}}
     {:user/name "bob@fast.co"       :user/tokens #{}            :user/active? true  :user/account #{"bob" "bob2"}}
     {:user/name "eve@attacker.zz"   :user/tokens #{100}         :user/active? false :user/account #{"eve"}}
     {:db/id "alice" :account/id 1 :account/tier :account.tier/free}
     {:db/id "eve"   :account/id 2 :account/tier :account.tier/enterprise}
     {:db/id "bob"   :account/id 3 :account/tier :account.tier/free}
     {:db/id "bob2"  :account/id 4 :account/tier :account.tier/silver}])

  ;; This is where the results start to get interesting.
  (def result2 (d/transact conn users))

  ;; The analyzer walks through the datoms from the transaction and
  ;; returns a map with details about the entities affected and how
  ;; their attributes changed.
  (analysis/analyze-tx (:db-before @result2) (:db-after @result2) (:tx-data @result2))

  ;; The `what?` entry point is a convenient way to go straight from a
  ;; transaction result to SVG
  (spit "doc/commentorial2.svg" (datoms-say/what? @result2))

  ;; `what?` is pretty much equivalent to this sequence of calls. You
  ;; can use this form to evaluate one step at a time when something
  ;; goes wrong.
  (datoms-say.dot/graph->dot
   (viz/transform
    (analysis/analyze-tx
     (:db-before @result2)
     (:db-after @result2)
     (:tx-data @result2))))

  ;; We're going to make some transaction data to change things. We
  ;; need to grab out some of the entity IDs that were just
  ;; created. The "account" entities used strings as tempids so we can
  ;; get the 'eve' account immediately.
  (def eve-account-id (get (:tempids @result2) "eve"))

  ;; The "user" entities don't have such easy tempids so we use lookup refs.
  (def alice-id (:db/id (d/entity (:db-after @result2) [:user/name "alice@example.com"])))
  (def eve-id   (:db/id (d/entity (:db-after @result2) [:user/name "eve@attacker.zz"])))

  ;; This transaction changes multivalued set, removes and entity, and
  ;; adds a new entity
  (def update-users
    [[:db/retract alice-id :user/tokens 100]
     [:db/retractEntity eve-id]
     {:user/name "mallory@attacker.zz" :user/tokens #{100 101} :user/active? true :user/account #{eve-account-id}}])

  ;; Do the transaction
  (def result3 (d/transact conn update-users))

  ;; And visualize the results
  (spit "doc/commentorial3.svg" (datoms-say/what? @result3))

  )
