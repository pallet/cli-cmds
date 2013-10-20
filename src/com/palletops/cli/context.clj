(ns com.palletops.cli.context
  "Functions for accessing and manipulating the cli context."
  (:require
   [clojure.java.io :refer [resource]]
   [clojure.string :as string]))

(defn initial-context
  "Provides a basic context.

`:project-name`
: the name of the project.  Appears in version messages, etc.

`:self-name`
: the name under which the command line is invoked.

`:ns-prefixes`
: a sequence of namespace prefix strings, for runtime command discovery

`:commands`
: a sequence of symbols for statically available commands

`:project-ns`
: a namespace symbol for a namespace containing a `project` var with a
  leiningen project map (maintained, for example, by configleaf)

`:sha-resoure`
: a resource path string for a file containg a git sha for the project.

`:static-help-path`
: a path (or format string), to the location of static help files
"
  [{:keys [project-name project-ns self-name sha-resource
           commands ns-prefixes version main-var]
    :as options}]
  {:cli/config
   (as->
    {:main-fn 'main} config
    (merge config {:project-name project-name
                   :project (when project-ns
                              (try
                                (require project-ns)
                                (when-let [v (ns-resolve project-ns 'project)]
                                  @v)
                                (catch Exception e
                                  (throw
                                   (ex-info
                                    (str "Could not require project namespace "
                                         project-ns)
                                    {:type :cli/invalid-config
                                     :exit-code 1}
                                    e)))))
                   :sha (when sha-resource
                          (try (slurp (resource sha-resource))
                               (catch Exception _)))})
    (assoc config :version (or version (:version (:project config))))
    (assoc config :project-name (or project-name (:name (:project config))))
    (assoc config :self-name (or self-name (-> (:project-name config)
                                               string/lower-case
                                               (string/replace " " "-"))))
    (merge config (select-keys
                   options
                   [:main-var :commands :ns-prefixes :static-help-path])))})

(defn ^:internal push-command
  "Push a command name onto the context."
  [context cmd]
  (update-in context [:cli/cmd-path] (fnil conj []) cmd))

(defn push-context
  "Push a command onto the context."
  [context cmd description arg-descriptors option-descriptors]
  (let [context (->
                 context
                 (update-in [:cli/option-descriptors] concat option-descriptors)
                 (assoc :cli/description description)
                 (assoc :cli/arg-descriptors arg-descriptors))]
    ;; push the cmd onto cmd-path only if this is not the main cli command
    (if (:cli/main (meta cmd))
      context
      (push-command context cmd))))

(defn config
  "Return the cli configuration from the context."
  [context]
  (:cli/config context))

(defn version-string
  "Return a version string based on the context."
  [context]
  (let [{:keys [project-name sha version]} (config context)]
    (format "%s %s - %s"
            (or project-name "Unnamed")
            (or version "Unknown version")
            (or sha "Unknown SHA"))))
