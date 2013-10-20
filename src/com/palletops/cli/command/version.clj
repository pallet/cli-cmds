(ns com.palletops.cli.command.version
  "Display the project version"
  (:require
   [com.palletops.cli.context :refer [version-string]]))

(defn version
  [context]
  (println (version-string context)))
