(ns datoms-say-what.viz
  (:require [datoms-say-what.render :as r]
            [datoms-say-what.dot :as dot]
            [datoms-say-what.spec :as spec]
            [viz.core :as viz]
            [clojure.spec.alpha :as s]))

(defn- transform-attribute
  [{:keys [ident value]}]
  [ident value])

(deftype Eid [entity-id partition meta]
  clojure.lang.IMeta
  (meta [this] meta)

  clojure.lang.IObj
  (withMeta [this m] (new Eid entity-id partition m))

  r/RenderAtom
  (render-atom [this] (str "<B>" entity-id "</B><BR/>" partition)))

(defn- transform-entity
  [analyzed-entity]
  (into
   [[(Eid. (:entity-id analyzed-entity) (:partition analyzed-entity)
           {:colspan 2 :port (:entity-id analyzed-entity)})]]
   (mapv transform-attribute (:attributes analyzed-entity))))

(defn- node-dot-attributes
  [analyzed-entity]
  (str "label=<"
       \newline
       (r/render (transform-entity analyzed-entity) nil)
       \newline
       ">"))

(defn- transform
  [analyzed]
  {:pre (s/valid? ::spec/analyzed analyzed)}
  {:nodes (map (juxt :entity-id node-dot-attributes) analyzed)
   :edges [["A" "B"] ["B" "A"]]})

(s/fdef transform
        :args (s/cat :analyzed ::spec/analyzed)
        :ret  ::spec/graph)

(defn visualize-tx
  [analyzed-data]
  (viz/image
   (dot/graph->dot
    (transform analyzed-data))))

(s/fdef visualize-tx
        :args (s/cat :analyzed-data ::spec/analyzed)
        :ret  string?)
