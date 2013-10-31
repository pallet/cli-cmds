(ns com.palletops.cli.api
  "API functions for the cli lib."
  (:require
   [clojure.stacktrace :refer [print-cause-trace]]
   [clojure.string :as string]
   [clojure.tools.cli :refer [cli *allow-unknown-opts?*]]
   [com.palletops.cli.context :refer [push-context]]
   [com.palletops.cli.help :refer [context-help-message]]
   [com.palletops.cli.resolve :refer [resolve-context]]))

(def ^:dynamic *debug* false)

;;; ## Exit Handling

;; This Var controls whether exit will exit the JVM process or return
;; and integer code
(def ^:dynamic *exit-process?* false)

(defn exit
  "Exit the JVM process if *exit-process?* is true, or return the exit-code."
  [exit-code]
  (flush)
  (if *exit-process?*
    (do
      (flush)
      (shutdown-agents)
      (System/exit exit-code))
    exit-code))

;;; ## Exception Handling
(defn handle-exceptions*
  "Executes a function with handling of exceptions containing :exit-code."
  [f]
  (try
    (f)
    (catch Exception e
      (let [{:keys [exit-code suppress-msg]} (ex-data e)]
        (if exit-code
          (do
            (when *debug*
              (binding [*out* *err*]
                (print-cause-trace e)))
            (when (or *debug* (not suppress-msg))
              (binding [*out* *err*]
                (println (.getMessage e))))
            (exit exit-code))
          (throw e))))))

(defmacro handle-exceptions
  "Provides a scope in which all exceptions are handled."
  [& body]
  `(handle-exceptions* (fn report-exceptions-fn [] ~@body)))

(defn report-exceptions*
  "Executes a function with reporting of all throwables."
  [f]
  (try
    (f)
    (catch Throwable e
      (let [{:keys [exit-code] :or {exit-code 1}} (ex-data e)]
        (binding [*out* *err*]
          (println "Internal Error: " (.getMessage e))
          (print-cause-trace e)
          (flush))
        (exit exit-code)))))

(defmacro report-exceptions
  "Provides a scope in which all throwables are reported."
  [& body]
  `(report-exceptions*
    (fn report-exceptions-fn [] ~@body)))

;;; ## tools.cli wrappers
(defn cli-non-strict
  "Process the args with the specified options.
  Return a tuple vector containing options map, args and help string. "
  [args option-descriptors]
  {:pre [(or (nil? option-descriptors) (sequential? option-descriptors))]}
  (binding [*allow-unknown-opts?* true]
    (apply cli args option-descriptors)))

;;; ## Arg validation
(defn ^:internal validate-arg
  "Validate a parsed argument."
  [arg {:keys [optional vararg valid-fn] :as arg-descriptor}]
  (cond
   (map? arg) arg              ; propagate any error from the parse-fn

   (and (= arg ::missing) (not (or optional vararg)))
   {:invalid-args (assoc arg-descriptor :value arg :reason :missing)}

   (and valid-fn (not (valid-fn arg)))
   {:invalid-args (assoc arg-descriptor :value arg :reason :invalid)}

   :else
   {:valid-args arg}))

(defn ^:internal parse-arg
  "Parse an argument, unless ::missing."
  [arg {:keys [parse-fn]}]
  (if parse-fn
    (parse-fn arg)
    arg))

(defn ^:internal arg-descriptor-map
  [[name description & {:keys [optional valid-fn parse-fn] :as options}]]
  (merge options {:arg-name name :description description}))

(defn- reduce-arg
  [[res args] arg-descriptor-map]
  [(merge-with conj res
               (validate-arg
                (if (seq args)
                  (try (parse-arg (first args) arg-descriptor-map)
                       (catch Exception e
                         {:invalid-args (assoc arg-descriptor-map
                                          :value (first args)
                                          :reason :not-parseable
                                          :error (.getMessage e))}))
                  ::missing)
                arg-descriptor-map))
   (rest args)])

(defn validate-args
  [cmd-args arg-descriptors]
  (let [arg-descriptors (map arg-descriptor-map arg-descriptors)
        ;; copy last arg descriptor as many times as required
        ;; if it is a :vararg
        arg-descriptors (if (:vararg (last arg-descriptors))
                          (concat
                           arg-descriptors
                           (repeat
                            (max 0
                                 (- (count cmd-args)
                                    (count arg-descriptors)))
                            (last arg-descriptors)))
                          arg-descriptors)
        n (count arg-descriptors)

        [args extra-args] [(take n cmd-args) (drop n cmd-args)]

        [{:keys [valid-args invalid-args]} _]
        (reduce
         reduce-arg
         [{:valid-args [] :invalid-args []} args]
         arg-descriptors)]
    [(remove #{::missing} valid-args) invalid-args extra-args]))

(defn invalid-args-message
  [invalid-args]
  (reduce
   (fn [s {:keys [arg-name value reason error value]}]
     (str s "Invalid argument for '" arg-name "'"
          (when value (str " with value '" value "'"))
          ". "
          (condp = reason
            :missing "Argument missing"
            :invalid "Argument invalid"
            :not-parseable "Argument wrong format")
          "."
          (when error (str " " error))))
   ""
   invalid-args))

(defn throw-on-invalid-args
  [invalid-args]
  (when (seq invalid-args)
    (throw
     (ex-info
      (invalid-args-message invalid-args)
      {:type :cli/invalid-args
       :invalid-args invalid-args
       :exit-code 1}))))

(defn throw-on-extra-args
  [extra-args]
  (when (seq extra-args)
    (throw
     (ex-info
      (str "Extra arguments provided: " (string/join ", " extra-args))
      {:type :cli/extra-args
       :extra-args extra-args
       :exit-code 1}))))

(defn valid-args [cmd-args arg-descriptors]
  (let [[valid-args invalid-args extra-args]
        (validate-args cmd-args arg-descriptors)]
    (throw-on-invalid-args invalid-args)
    (throw-on-extra-args extra-args)
    valid-args))

(defn options-and-args
  "Return a vector tuple of options and arguments."
  [args arg-descriptors option-descriptors]
  {:pre [(or (nil? args) (sequential? args))
         (or (nil? arg-descriptors) (sequential? arg-descriptors))
         (or (nil? option-descriptors) (sequential? option-descriptors))]}
  (let [[options args] (cli-non-strict args option-descriptors)
        valid-args (valid-args args arg-descriptors)]
    [options valid-args]))

;;; ## Command execution
(defn execute-subcommand
  "Execute a sub-command from args passed to the CLI."
  [context [cmd & cmd-args] cmd-name description arg-descriptors
   option-descriptors]
  (handle-exceptions
   (let [context (push-context context cmd-name description arg-descriptors
                               option-descriptors)]
     (if cmd
       (let [cmd-f (resolve-context context cmd)]
         (cmd-f context cmd-args))
       (println (context-help-message context))))))
