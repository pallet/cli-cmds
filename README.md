[Repository](https://github.com/pallet/cli-cmds) &#xb7;
[Issues](https://github.com/pallet/cli-cmds/issues) &#xb7;
[API docs](http://palletops.com/cli-cmds/0.1/api) &#xb7;
[Annotated source](http://palletops.com/cli-cmds/0.1/annotated/uberdoc.html) &#xb7;
[Release Notes](https://github.com/pallet/cli-cmds/blob/develop/ReleaseNotes.md)

# cli-cmds

A Clojure library for writing command line interfaces, where
commands can have sub-commands, arguments and options.

## Usage

Each command and sub-command is defined in a namespace. A sub-command
is defined in a namespace below the command namespace.

Basic usage is via `def-main`, `def-command` and `def-command-fn`.

To define a main entry point for a CLI, use the `def-main` macro.
This defines the `-main` function and a command that is called by it.
A configuration map is passed to def-main, with configuration for the
`cli-cmds` library.


```clj
(ns my.cli
  (:require [com.palletops.cli.command :refer [def-main]]))

(def-main
  {:project-name "My Project"
   :self-name "mycli"
   :ns-prefixes ["my.command."]
   :commands ['com.palletops.cli.command.help]}
  "Shared911 ops command line tool."
  [["-v" "--verbose" "Show verbose output" :flag true]])
```

The `def-command` macro is used to define a command that dispatches to
sub-commands.  You can add options that are common to all sub-commands
here.

```clj
(ns my.command.db
  (:require [com.palletops.cli.command :refer [def-command]]))

(def-command db
  "Database commands"
  [["-u" "--user" "Username"]])
```

The `def-command-fn` macro is used to define a function that
implements a command.  Add command specific options and arguments
here.

```clj
(ns my.command.db.tables
  (:require [com.palletops.cli.command :refer [def-command-fn]]))

(def-command-fn table
  "List DB tables"
  [["table" "Table Name" :optional true]]
  [["-l" "--long" "Show full column defintions" :flag true]]
  [context args]
  (...))
```

## API Usage

It is possible to use a lower level api.

### Command Dispatch

The `options-and-args` function returns parsed options and arguments.

The `execute-subcommand` function can be used to dispatch a command to
sub-commands.

### Help Information

Help information is looked up from function vars.  For the built in
help functions to work, the command functions must have metadata.

To use a plain function for a commmand, the function var must have
`:cli/description`, `:cli/arg-descriptors` and
`:cli/option-descriptors` metadata.

To use a plain function for the cli main command, ensure the function var has
:cli/main metadata, with a `true` value.

## Concepts

### Option Descriptor

Option descriptors use the same format as [`tools.cli`][tools.cli],
which is used to process them.

### Argument Descriptor

Similar to option descriptors, `cli-cmds` introduces argument
descriptors for describing the arguments a command accepts.

The format is a sequence of
`[name description :optional true :valid-fn f]`.  Once an argument
is declared optional, all further arguments must be optional.  Default values
for flags are:

```clj
{:optional false
 :vararg false
 :valid-fn nil
 :parse-fn nil}
```

### Context

A context map is used to configure the library and pass information to
tasks.  An initial context is created based on the argumetns to
`main`, or by calling the `initial-context` API function.

The `:cli/config` key is used to configure the library.

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

## Install

### lein project.clj

```clojure
:dependencies [[com.palletops/cli-cmds "0.1.0"]]
```

### maven pom.xml

```xml
<dependencies>
  <dependency>
    <groupId>com.palletops</groupId>
    <artifactId>cli-cmds</artifactId>
    <version>0.1.0</version>
  </dependency>
<dependencies>

<repositories>
  <repository>
    <id>clojars</id>
    <url>http://clojars.org/repo</url>
  </repository>
</repositories>
```

## License

Copyright © 2013 Hugo Duncan, Antoni Batchelli

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

[tools.cli]: https://github.com/clojure/tools.cli
