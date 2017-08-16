(ns datoms-say-what
  (:require [clojure.spec.alpha :as s]
            [datomic.api :as d]))

(def byte-array-class (type (byte-array 0)))

(s/def ::long      #(instance? Long %))
(s/def ::entity-id ::long)

;; Value types. See http://docs.datomic.com/schema.html
(s/def ::keyword keyword?)
(s/def ::string  string?)
(s/def ::boolean boolean?)
(s/def ::bigint  #(instance? BigInteger %))
(s/def ::float   #(instance? Float %))
(s/def ::double  #(instance? Double %))
(s/def ::bigdec  #(instance? BigDecimal %))
(s/def ::ref     ::entity-id)
(s/def ::instant #(instance? java.util.Date %))
(s/def ::uuid    uuid?)
(s/def ::uri     #(instance? java.net.URI %))
(s/def ::bytes   #(instance? byte-array-class %))

(s/def ::attribute-id       ::entity-id)
(s/def ::transaction-id     ::entity-id)
(s/def ::value              (s/alt :keyword ::keyword :string ::string :boolean ::boolean :long
                                   ::long :bigint ::bigint :float ::float :double ::double
                                   :bigdec ::bigdec :ref ::ref :instant ::instant
                                   :uuid ::uuid :uri ::uri :bytes ::bytes))

(s/def ::datoms-in          (s/+ (s/tuple ::entity-id ::attribute-id ::value ::transaction-id boolean?)))

(s/def ::asserted-value     ::value)
(s/def ::retracted-value    ::value)
(s/def ::singlevalue-change (s/keys :opt-un [::asserted-value ::retracted-value]))
(s/def ::valset             (s/coll-of ::value :kind set?))
(s/def ::before-set         ::valset)
(s/def ::after-set          ::valset)
(s/def ::multivalue-change  (s/keys :opt-un [::before-set ::after-set]))
(s/def ::ident              ::keyword)
(s/def ::value-type         ::keyword)
(s/def ::cardinality        #{:db.cardinality/one :db.cardinality/many})
(s/def ::attribute          (s/keys :req-un [::entity-id ::value-type ::ident ::cardinality] :opt-un [::value ::singlevalue-change ::multivalue-change]))
(s/def ::attributes         (s/* ::attribute))
(s/def ::partition          ::keyword)
(s/def ::entity             (s/keys :req-un [::entity-id ::attributes] :opt-un [::partition]))
(s/def ::entities           (s/* ::entity))
(s/def ::analyzed           (s/keys :req-un [::entities]))

(defn partition-ident
  [db eid]
  (:db/ident (d/entity db (d/part eid))))

(defn analyze-entity-attribute
  [db-before db-after eid aid]
  (let [e-after (d/entity db-after eid)
        a       (d/entity db-after aid)
        ident   (:db/ident a)
        card    (:db/cardinality a)]
    (merge
     {:entity-id   aid
      :ident       ident
      :value-type  (:db/valueType a)
      :cardinality card
      :value       (get e-after ident)}
     (when (= :db.cardinality/many card)
       {:before-set (get (d/entity db-before eid) ident)
        :after-set  (get e-after ident)}))))

(defn analyze-entity-attributes
  [db-before db-after eid]
  (map
   #(apply analyze-entity-attribute db-before db-after %)
   (d/q '[:find ?e ?a
          :in $ ?e
          :where [?e ?a]]
        db-after eid)))

(defn analyze-entity
  [db-before db-after eid]
  {:entity-id  eid
   :partition  (partition-ident db-after eid)
   :attributes (analyze-entity-attributes db-before db-after eid)})

(defn- update-when
  [pred coll f arg]
  (keep
   #(if (pred % arg) (f % arg) %)
   coll))

(defn- co-eq
  [f a1 a2]
  (= (f a1) (f a2)))

(def ids-match (partial co-eq :entity-id))

(defn- inspect-datom
  [datom]
  {:entity-id (:a datom) (if (:added datom) :asserted-value :retracted-value) (:v datom)})

(defn- merge-changes
  [attributes db-before db-after entity tx-data]
  (reduce
   #(update-when ids-match %1 merge %2)
   attributes
   (map inspect-datom tx-data)))

(defn- analyze-changes
  [db-before db-after entity tx-data]
  (update entity :attributes merge-changes db-before db-after entity tx-data))

(defn analyze-tx-data
  [db-before db-after tx-data]
  (let [eids-touched (distinct (map :e tx-data))]
    (map
     #(analyze-changes
       db-before db-after
       (analyze-entity db-before db-after %)
       tx-data)
     eids-touched)))

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
  )
