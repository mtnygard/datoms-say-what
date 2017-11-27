(ns datoms-say.render
  (:require [clojure.string :as str]
            [datomic.api :as d]))

(defprotocol RenderAtom
  (render-atom [this] "Render to an atomic string"))

(defn attributes
  [as]
  (interpose
   " "
   (map (fn [[k v]] [(str/upper-case (name k)) "=\"" (render-atom v) "\""]) as)))

(defn- delimited [start contents end]
  (str start
       (apply str (interpose " " (map render-atom contents)))
       end))

(def ^:private pound "&#35;")
(def ^:private lset "&#35;&#123;")
(def ^:private rset "&#125;")
(def ^:private lvec "&#91;")
(def ^:private rvec "&#93;")
(def ^:private lmap "&#123;")
(def ^:private rmap "&#125;")

(extend-protocol RenderAtom
  Object
  (render-atom [this]
;    (println "render-atom Object                " this)
    (str this))

  clojure.lang.IPersistentSet
  (render-atom [this]
;    (println "render-atom IPersistentSet        " this)
    (delimited lset this rset))

  clojure.lang.IPersistentVector
  (render-atom [this]
;    (println "render-atom IPersistentVector     " this)
    (delimited lvec this rvec))

  clojure.lang.IPersistentMap
  (render-atom [this]
;    (println "render-atom IPersistentMap        " this)
    (delimited lmap this rmap))

  clojure.lang.MapEntry
  (render-atom [this]
;    (println "render-atom MapEntry              " this)
    (str (key this) " " (val this)))

  datomic.db.DbId
  (render-atom [this]
;    (println "render-atom DbId                  " this)
    (str pound "db/id[" (.part this) " " (.idx this) "]"))

  datomic.query.EntityMap
  (render-atom [this]
;    (println "render-atom EntityMap             " this)
    (render-atom (:db/id this))))

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

(defn row
  [_ xs]
  ;; Graphviz doesn't allow TR to have attributes
  (as-str
   ["<TR>"
    xs
    "</TR>"]))

(defn cell
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
;    (println "render      Object                " this " in scope " level)
    (case level
      nil    (render-atom this)
      :table (row nil (cell (meta this) (render-atom this)))
      :row   (cell    (meta this) (render-atom this))
      :cell  (render-atom this)))

  clojure.lang.IPersistentCollection
  (render [this level]
;    (println "render      IPersistentCollection " this " in scope " level)
    (case level
      nil    (table (meta this) (map #(render % :table) this))
      :table (row   (meta this) (map #(render % :row)   this))
      :row   (cell  (meta this) (render-atom this))
      :cell  (render-atom this)))

  clojure.lang.MapEntry
  (render [this level]
;    (println "render      MapEntry              " this " in scope " level)
    (case level
      nil    (str   (render (key this) nil) " "
                    (render (val this) nil))
      :table (row   (meta this) [(render (key this) :row)
                                 (render (val this) :row)])
      :row   (cell  (meta this) [(render (key this) :cell) " "
                                 (render (val this) :cell)])
      :cell  [(render-atom (key this))
              (render-atom (val this))])))
