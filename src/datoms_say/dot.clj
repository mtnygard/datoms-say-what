(ns datoms-say.dot
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str]))

(defn graph->dot
  [{:keys [nodes edges]}]
  (str/join
   (flatten
    ["digraph G {"
     "rankdir=LR;"
     "node [shape=Mrecord, style=\"rounded,filled\", fillcolor=\"#FAF0E6\"];"

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
