(ns com.palletops.cli.command.server.pid
  (:require
   [com.palletops.cli.command :refer [def-command-fn]])
  (:import
   java.lang.management.ManagementFactory))

(defn ^String pid
  "Return the pid of the current process."
  []
  (second (re-find #"(\d+)@.*"
                   (.. (ManagementFactory/getRuntimeMXBean) getName))))

(def-command-fn pid
  "Display the PID of the current NREPL server session." [] []
  [_ _]
  (println (pid)))
