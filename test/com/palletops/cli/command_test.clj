(ns com.palletops.cli.command-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.cli.command :refer :all]))

(def-command-fn cmd1 "cmd1" [] [] [context args]
  args)

(def-command cmd2 "" [])

(def-main {} "" [])
