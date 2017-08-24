(ns datoms-say.analysis
  (:require [datomic.api :as d]
            [clojure.spec.alpha :as s]
            [datoms-say.spec :as spec])
    (:import datomic.db.Db))

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

(defn analyze-tx
  [db-before db-after tx-data]
  (let [eids-touched (distinct (filter #(not= 0 %) (map :e tx-data)))]
    (println (distinct (map :e tx-data)))
    (map
     #(analyze-changes
       db-before db-after
       (analyze-entity db-before db-after %)
       tx-data)
     eids-touched)))

(s/fdef analyze-tx
        :args (s/cat :db-before #(instance? Db %) :db-after #(instance? Db %) :tx-data ::spec/datoms)
        :ret ::spec/analyzed)
