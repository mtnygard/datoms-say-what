(ns datoms-say-what.render
  (:require [clojure.string :as str]))

(defn attributes
  [as]
  (interpose
   " "
   (map (fn [[k v]] [(str/upper-case (name k)) "=\"" (render-atom v) "\""]) as)))

(defprotocol RenderAtom
  (render-atom [this] "Render to an atomic string"))

(defn- delimited [start contents end]
  (str start
       (apply str (interpose " " (map render-atom contents)))
       end))

(def ^:private lset "&#35;&#123;")
(def ^:private rset "&125;")
(def ^:private lvec "&#91;")
(def ^:private rvec "&#93;")
(def ^:private lmap "&#123;")
(def ^:private rmap "&125;")

(extend-protocol RenderAtom
  Object
  (render-atom [this]
    (str this))

  clojure.lang.IPersistentSet
  (render-atom [this]
    (delimited lset this rset))

  clojure.lang.IPersistentVector
  (render-atom [this]
    (delimited lvec this rvec))

  clojure.lang.IPersistentMap
  (render-atom [this]
    (delimited lmap this rmap))

  clojure.lang.MapEntry
  (render-atom [this]
    (str (key this) " " (val this))))

(defprotocol Render
  (render [this level] "Render a potentially composite structure. Level is #{nil :table :row :cell}"))

(defn- as-str
  [strs]
  (apply str (flatten strs)))

(defn- table
  [as xs]
  (as-str
   ["<TABLE BORDER=\"0\" " (attributes as) ">"
    xs
    "</TABLE>"]))

(defn- row
  [as xs]
  (as-str
   ["<TR " (attributes as) ">"
    xs
    "</TR>"]))

(defn- cell
  [as xs]
  (as-str
   ["<TD " (attributes as) ">"
    xs
    "</TD>"]))

(extend-protocol Render
  nil
  (render [this level] "nil")

  Object
  (render [this level]
    (case level
      nil    (render-atom this)
      :table (row nil (cell (meta this) (render-atom this)))
      :row   (cell    (meta this) (render-atom this))
      :cell  (render-atom this)))

  clojure.lang.IPersistentCollection
  (render [this level]
    (case level
      nil    (table (meta this) (map #(render % :table) this))
      :table (row   (meta this) (map #(render % :row)   this))
      :row   (cell  (meta this) (map #(render % :cell)  this))
      :cell  (render-atom this)))

  clojure.lang.MapEntry
  (render [this level]
    (case level
      nil    (str   (render (key this) nil) " "
                    (render (val this) nil))
      :table (row   (meta this) [(render (key this) :row)
                                 (render (val this) :row)])
      :row   (cell  (meta this) [(render (key this) :cell) " "
                                 (render (val this) :cell)])
      :cell  [(render-atom (key this))
              (render-atom (val this))])))
