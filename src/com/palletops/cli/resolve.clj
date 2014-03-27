(ns com.palletops.cli.resolve
  "Functions for resolving and finding cli commands"
  (:require
   [chiba.plugin :refer [plugins]]
   [clojure.string :as string]
   [clojure.tools.cli :refer [cli]]))

(defonce ^{:doc "A sequence of command namespace symbols."}
  command-map (atom nil))

(defn reset-commands! []
  (reset! command-map nil))

(defn prefix->ns-string [prefix]
  (string/replace prefix "_" "-"))

(defn prefix-plugins
  "Return a sequence of [command, namespace symbol] tuples."
  [prefix]
  (let [n (inc (.lastIndexOf prefix "."))]
    (map #(vector (subs (name %) n) %)
         (plugins prefix))))

(defn ns-plugins
  "Return a sequence of [command, namespace symbol] tuples."
  [ns-sym]
  (require ns-sym)
  [(last (string/split (name ns-sym) #"\.")) ns-sym])

(defn commands
  "Return a sequence of the available command namespace symbols.
  This does not load the namespaces."
  [context]
  (let [{:keys [ns-prefixes commands]} (:cli/config context)]
    (try
      (or @command-map
          (reset! command-map
                  (into {} (concat
                            (map ns-plugins commands)
                            (mapcat prefix-plugins ns-prefixes)))))
      (catch Exception e
        (throw
         (ex-info
          "Invalid command on classpath"
          {:type :cli/invalid-command}
          e))))))

(defn filter-command-prefix
  "Filter a map of commands for commands with the given prefix."
  [cmds cmd-path]
  (if cmd-path
    (into {} (filter #(.startsWith (key %)
                                   (str (string/join "." cmd-path) "."))
                     cmds))
    cmds))



(defn- distance                         ; taken verbatim from leiningen
  "String distance"
  [s t]
  (letfn [(iters [n f start]
            (take n (map second
                         (iterate f start))))]
    (let [m (inc (count s)), n (inc (count t))
          first-row (vec (range m))
          matrix (iters n (fn [[j row]]
                            [(inc j)
                             (vec (iters m (fn [[i col]]
                                             [(inc i)
                                              (if (= (nth s i)
                                                     (nth t j))
                                                (get row i)
                                                (inc (min (get row i)
                                                          (get row (inc i))
                                                          col)))])
                                         [0 (inc j)]))])
                        [0 first-row])]
      (last (last matrix)))))

(defn suggestions                       ; modified from leiningen
  "Suggest possible misspellings for command from list of commands."
  [cmd cmds]
  (when (seq cmds)
    (let [suggestions (for [t cmds]
                        [t (distance t cmd)])
          min (reduce min (map second suggestions))]
      (if (< min 5)
        (map first (filter #(= min (second %)) suggestions))))))

(defn resolve-command-from-ns
  "Require the given namespace symbol, and then resolve the cmd symbol."
  [ns-sym cmd-path cmd]
  {:pre [(symbol? ns-sym)
         (or (nil? cmd-path) (symbol? cmd-path))
         (symbol? cmd)
         (not= (symbol "") cmd)]}
  (when (.endsWith (name ns-sym)
                   (str (if cmd-path (str "." cmd-path ".")) cmd))
    (when-not (find-ns ns-sym)
      (try (require ns-sym)
           (catch java.io.FileNotFoundException _)))
    (if (find-ns ns-sym)
      (ns-resolve ns-sym cmd))))

(defn resolve-command-from-ns-prefix
  [ns-prefix cmd-path cmd]
  {:pre [(string? ns-prefix)
         (or (nil? cmd-path) (symbol? cmd-path))
         (not= (symbol "") cmd-path)
         (symbol? cmd)
         (not= (symbol "") cmd)]}
  (let [s (prefix->ns-string ns-prefix)]
    ;; if the prefix ends without a ".", it can only match the
    ;; specified command directly, or a subcommand (with the
    ;; addition of the ".")
    (resolve-command-from-ns
     (symbol (str s
                  (if (and cmd-path (not (.endsWith s "."))) ".")
                  (if (and cmd-path (not (.endsWith s (name cmd-path))))
                    (str cmd-path "."))
                  (if (or cmd-path (.endsWith s ".")) cmd)))
     cmd-path
     cmd)))

(defn resolve-command
  "Return the command function for the given context and cmd name.
  Returns nil if the command can not be resolved.
  `cmd-path` is a dotted namespace path for sub-commands."
  ([context cmd-path cmd]
     {:pre [cmd-path cmd]}
     (let [cmd-path (if-not (string/blank? cmd-path) (symbol cmd-path))
           cmd (symbol cmd)
           {:keys [commands ns-prefixes]} (:cli/config context)]
       (or
        (->> commands
             (map #(resolve-command-from-ns % cmd-path cmd))
             (remove nil?)
             first )
        (->> ns-prefixes
             (map #(resolve-command-from-ns-prefix % cmd-path cmd))
             (remove nil?)
             first))))
  ([context cmd]
     (resolve-command context cmd cmd)))

(defn command-not-found
  [context cmd]
  (binding [*out* *err*]
    (println (str "'" cmd "' is not a known command. See 'help'."))
    (when-let [suggestions (suggestions cmd (keys (commands context)))]
      (println)
      (println "Did you mean this?")
      (doseq [suggestion suggestions]
        (println "        " suggestion))))
  (throw
   (ex-info (str "Command \"" cmd "\" not found")
            {:exit-code 1
             :suppress-msg true})))

(defn find-command
  "Return the command function for the given context and cmd name."
  [context cmd-path cmd & {:keys [not-found-f]
                           :or {not-found-f command-not-found}}]
  {:pre [cmd-path cmd]}
  (if-let [f (resolve-command context cmd-path cmd)]
    f
    (not-found-f context cmd-path)))

(defn resolve-context [context cmd]
  (let [cmd-path (:cli/cmd-path context)]
    (find-command context (string/join "." cmd-path) cmd)))

(defn resolve-main
  "Resolve a main entry point."
  [context]
  (let [{:keys [main-var] :as config} (:cli/config context)]
    main-var))
