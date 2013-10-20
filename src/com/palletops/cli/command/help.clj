(ns com.palletops.cli.command.help
  "A help command"
  (:require
   [clojure.string :as string]
   [clojure.java.io :refer [resource]]
   [clojure.tools.cli :refer [cli]]
   [com.palletops.cli.help :refer [help-message]]))

(defn help
  "Display a list of cmds or help for a given cmd or subcmd."
  {:cli/arg-descriptors
   [["command" "Command to display help for" :vararg true]]}
  [context commands]
  (println (help-message context commands)))
