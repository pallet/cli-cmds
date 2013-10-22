(ns com.palletops.cli.command.server.kill
  (:require
   [com.palletops.cli.command :refer [def-command-fn]]))

(def-command-fn kill
  "Kill the current NREPL server session." [] []
  [_ _]
  (do (shutdown-agents)
      (System/exit 0)))
