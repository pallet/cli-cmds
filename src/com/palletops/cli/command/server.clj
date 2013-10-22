(ns com.palletops.cli.command.server
  "Control the NREPL server."
  (:require
   [com.palletops.cli.command :refer [def-command]]))

(def-command server
  "Control The NREPL server session"
  [])
