(ns datoms-say.dot
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(defn graph->dot
  [{:keys [nodes edges attributes name] :or {name "G" attributes ""}}]
  (def n* nodes)
  (str/join
   (flatten
    ["digraph \"" name "\" {"
     attributes

     (map
      (fn [[id text]]
        (str id " [" text "];"))
      nodes)

     (map
      (fn [[from to]]
        (str from " -> " to ";"))
      edges)

     "}"])))

(s/fdef graph->dot
        :args (s/cat :graph ::graph)
        :ret  string?)
