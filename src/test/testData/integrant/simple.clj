(ns integrant.simple
  (:require [integrant.core :as ig]))

(def config
  {<caret>:hello/hello {:name "Alice"}})

(defmethod ig/init-key :hello/hello [_ {:keys [name]}]
           (println name))
