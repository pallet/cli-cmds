(ns com.palletops.cli.command
  "Functions for declaring cli commands"
  (:require
   [clojure.string :as string]
   [clojure.tools.cli :refer [cli]]
   [clojure.tools.macro :refer []]
   [com.palletops.cli.api
    :refer [cli-non-strict execute-subcommand handle-exceptions
            report-exceptions options-and-args]]
   [com.palletops.cli.command.help :refer [help]]
   [com.palletops.cli.context :refer [initial-context]]
   [com.palletops.cli.resolve :refer [resolve-context]]))

(defmacro def-command-fn
  "Defines a cli command function, taking context and argument vector arguments.

    (def-command-fn name description arg-descriptors option-descriptors [args]
      fn body)

This is a convenience macro to add the expected metadata to an
ordinary function."
  [name description arg-descriptors option-descriptors args & body]
  (let [d (gensym "description")
        a (gensym "arg-descriptors")
        o (gensym "option-descriptors")
        m {:cli/description d
           :cli/arg-descriptors a
           :cli/option-descriptors o}]
    (when (not= 2 (count args))
      (throw
       (ex-info (str "Invalid argument count for " name
                     ". Must take context and argument vector arguments.")
                {:type :cli/invalid-command-fn
                 :name name
                 :args args})))
    `(let [~d ~description
           ~a ~arg-descriptors
           ~o ~option-descriptors]
       (defn ~(vary-meta name merge m) [context# args#]
         (let [[options# args#] (options-and-args args# ~a ~o)
               ~(first args) (merge context# options#)
               ~(second args) args#]
           ~@body)))))

(defmacro def-command
  "Defines a command that will dispatch to sub-commands."
  [name description option-descriptors]
  (let [d (gensym "description")
        o (gensym "option-descriptors")]
    `(let [~d ~description
           ~o ~option-descriptors
           a# [["command" "Command name" :vararg true]]]
       (def-command-fn ~name ~d a# ~o
         [context# args#]
         (execute-subcommand context# args# '~name ~d a# ~o)))))

(defmacro def-main
  "Defines a main entry point to the cli."
  [config description option-descriptors]
  (let [main (gensym "cli-main")]
    `(do
       (def-command ~(vary-meta main assoc :cli/main true)
         ~description ~option-descriptors)
       (defn ~'-main [& args#]
         (report-exceptions
          (~main
           (initial-context (merge {:main-var (var ~main) } ~config))
           args#))))))
