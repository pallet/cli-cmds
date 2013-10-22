(ns com.palletops.cli.help
  "Functions to generate help messages."
  (:require
   [clojure.string :as string]
   [clojure.java.io :refer [file resource]]
   [clojure.tools.cli :refer [cli]]
   [com.palletops.cli.context
    :refer [config push-command push-context version-string]]
   [com.palletops.cli.resolve
    :refer [commands filter-command-prefix resolve-context resolve-main]]))


;;; # Static Help Files
(defn static-help [context names]
  (when (seq names)
    (if-let [static-help-path (-> context config :static-help-path)]
      (let [rel-path (str (apply file names))
            path (if (.contains static-help-path "%s")
                   (format static-help-path rel-path)
                   (str (file static-help-path rel-path)))]
        (if-let [resource (resource path)]
          (slurp resource))))))


;;; # Commmand Based Help

;;; ## Formatting
(def ^{:private true
       :doc "Width of cmd name column in list of cmds produced by help cmd."}
  cmd-name-column-width 20)

(defn format-column-value
  [s]
  (apply str (repeat (- cmd-name-column-width (count s)) " ")))

(def ^:private help-padding 3)

(defn- formatted-docstring [command docstring padding]
  (apply str
         (replace
          {\newline
           (apply str
                  (cons \newline (repeat (+ padding (count command)) \space)))}
          docstring)))

(defn- formatted-help [command docstring longest-key-length]
  (let [padding (+ longest-key-length help-padding (- (count command)))]
    (format (str "%1s" (apply str (repeat padding \space)) "%2s")
            command
            (formatted-docstring command docstring padding))))


(defn cmd-ns-doc [context cmd-ns cmd-name]
  (or (:doc (meta (find-ns cmd-ns)))
      (first (string/split-lines (:cli/description context)))))

(defn help-summary-for [context [cmd-name cmd-ns]]
  (try
    (require cmd-ns)
    (str (string/replace cmd-name "." " ")
         (format-column-value cmd-name)
         " " (cmd-ns-doc context cmd-ns cmd-name))
    (catch Exception e
      (binding [*out* *err*]
        (str cmd-ns " failed to load: " (.getMessage e))))))


;;; ## Option Descriptor Formatting
(defn doc-string
  "Return the doc string for the given description and options descriptor."
  [description option-descriptor]
  (if (seq option-descriptor)
    (last (apply cli nil description option-descriptor))
    description))


;;; ## Argument Descriptor Formatting

;; TODO - remove duplication of function in api
(defn ^:internal arg-descriptor-map
  [[name description & {:keys [optional valid-fn] :as options}]]
  (merge options {:arg-name name :description description}))

(defn arg-spec
  [arg-descriptor]
  (let [{:keys [arg-name description optional vararg]}
        (arg-descriptor-map arg-descriptor)]
    (str (if (and optional (not vararg)) "[")
         arg-name
         (if (and optional (not vararg)) "]")
         (if vararg "*"))))

(defn args-spec
  [arg-descriptors]
  (string/join " " (map arg-spec arg-descriptors)))

(defn arg-description
  [arg-descriptor]
  (str "  " (first arg-descriptor) ">"))

(defn args-description
  [arg-descriptors]
  (string/join \newline (map arg-description arg-descriptors)))

;;; ## Command filtering
(defn commands-for
  "Return commands available at the given command path."
  [commands cmd-path]
  (let [prefix (string/join "." cmd-path)
        n (inc (count prefix))]
    (->> commands
         (filter #(.startsWith (key %) prefix))
         (filter #(and
                   (> (count (key %)) n)
                   (not (.contains (subs (key %) n) ".")))))))

;;; ## Help Context
(defn ^:internal meta->args
  "Convert meta data to args for push-context"
  [m]
  [(vary-meta (:name m) assoc :cli/main (:cli/main m))
   (:cli/description m) (:cli/arg-descriptors m) (:cli/option-descriptors m)])

(defn resolve-meta
  "Resolve meta for a command"
  [context cmd]
  (if-let [v (resolve-context context cmd)]
    (meta v)))

(defn help-context
  "Builds a context for the specified commands"
  [context commands]
  (let [context (dissoc context :cli/option-descriptors)
        main (resolve-main context)
        main-meta (meta main)]
    (assert main)
    (reduce
     (fn [cntxt cmd]
       (let [m (resolve-meta cntxt cmd )]
         (apply push-context cntxt (meta->args m))))
     (apply push-context context (meta->args main-meta))
     commands)))


;;; ## Help Messages
(defn context-help-message
  "Return a help string for a given command."
  [context]
  (let [{:keys [self-name]} (config context)]
    (doc-string
     (str
      (version-string context) \newline \newline
      (:cli/description context) \newline \newline
      "    " self-name " "
      (string/join " " (:cli/cmd-path context)) " "
      (string/join " " (map arg-spec (:cli/arg-descriptors context)))
      \newline

      (when-let [cmds (seq (commands-for
                            (filter-command-prefix
                             (commands context) (:cmd-path context))
                            (:cli/cmd-path context)))]
        (str
         \newline
         "Available commands:" \newline
         (string/join \newline
                      (for [cmd cmds]
                        (help-summary-for context cmd))))))
     (:cli/option-descriptors context))))


(defn help-message
  "Return a help message for the given commands."
  [context cmds]
  (or
   (static-help context cmds)
   (context-help-message (help-context context cmds))))
