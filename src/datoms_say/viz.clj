(ns datoms-say.viz
  (:require [clojure.data :as data]
            [clojure.spec.alpha :as s]
            [datoms-say.dot :as dot]
            [datoms-say.render :as r]
            [datoms-say.spec :as spec]
            [viz.core :as viz]))

(def ^:private colors
  {:fill     "#D0D2D9"
   :retract  "#73515D"
   :assert   "#63A660"
   :disjoint "#595454"
   :comment  "#595454"})

(def ^:private color-for-change
  {[true true]   :fill
   [true false]  :assert
   [false true]  :retract
   [false false] :disjoint})

(defn- set-changes
  [before after]
  (let [[gone added same] (data/diff before after)]
    (color-for-change [(nil? gone) (nil? added)])))

(defn- attributes-for-attribute
  "Return the Dot-HTML attributes to use for this Datomic attribute."
  [attribute]
  (merge
   {:port (str (.entity-id attribute))}
   (cond
     (some? (.before-set attribute))
     {:bgcolor (get colors (set-changes (.before-set attribute) (.after-set attribute)))}

     (some? (.retracted-value attribute))
     {:bgcolor (:retract colors)}

     (some? (.asserted-value attribute))
     {:bgcolor (:assert colors)})))

(deftype Attribute [ident entity-id value asserted-value retracted-value before-set after-set]
  r/RenderAtom
  (render-atom [this]
;    (println "render-atom Attribute             " this)
    (str ident))

  r/Render
  (render [this level]
;    (println "render      Attribute             " this " in scope " level  "--> before-set " before-set)
    (when (= :table level)
      (r/row {}
             [(r/cell {} (r/render-atom ident))
              (r/cell (attributes-for-attribute this)
                      (apply str (r/render-atom value)
                             [(when (and before-set (not= before-set after-set))
                                 (str "<br/>was: " (r/render-atom before-set)))
                              (when (and (not before-set) asserted-value (not= asserted-value value))
                                (str "<br/>asserted: " (r/render-atom asserted-value)))
                              (when (and (not before-set) retracted-value)
                                (str "<br/>retracted: " (r/render-atom retracted-value)))]))]))))

(defn- transform-attribute
  [{:keys [ident value asserted-value retracted-value entity-id before-set after-set] :as attr}]
  (Attribute. ident entity-id value asserted-value retracted-value before-set after-set))

(deftype Eid [entity-id partition meta]
  clojure.lang.IMeta
  (meta [this] meta)

  clojure.lang.IObj
  (withMeta [this m] (new Eid entity-id partition m))

  r/RenderAtom
  (render-atom [this]
;    (println "render-atom Eid                   " this)
    (str "<B>" entity-id "</B><BR/>" partition)))

(defn- transform-entity
  [analyzed-entity]
  (into
   [[(Eid. (:entity-id analyzed-entity) (:partition analyzed-entity)
           {:colspan 2 :port "id"})]]
   (mapv transform-attribute (:attributes analyzed-entity))))

(defn- node-dot-attributes
  [analyzed-entity]
  [(:entity-id analyzed-entity)
   (str "label=<"
        \newline
        (r/render (transform-entity analyzed-entity) nil)
        \newline
        ">")])

(defn- dot-edge
  [e]
;  (println "dot-edge " e)
  (let [[[from-eid from-attr] to-eid] e]
    [(str from-eid ":" from-attr) (str to-eid ":id")]))

(defn transform
  [analyzed]
  {:pre (s/valid? ::spec/analyzed analyzed)}
  (def a* analyzed)
  {:nodes (->> analyzed :entities   (map node-dot-attributes))
   :edges (->> analyzed :references (map dot-edge))})

(s/fdef transform
        :args (s/cat :analyzed ::spec/analyzed)
        :ret  ::spec/graph)

(def ^:private graph-defaults
  {:name       "Transaction Effects"
   :attributes (str "rankdir=LR; node [shape=none, style=\"rounded,filled\", fillcolor=\"" (:fill colors) "\"];")})

(defn dotify-tx
  [analyzed-data]
  (doto (dot/graph->dot
         (merge
          graph-defaults
          (transform analyzed-data)))
    (println)))

(defn visualize-tx
  [analyzed-data]
  (viz/image (dotify-tx analyzed-data)))

(s/fdef visualize-tx
        :args (s/cat :analyzed-data ::spec/analyzed)
        :ret  string?)
