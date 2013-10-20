(ns com.palletops.cli.command.help-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.cli.resolve :refer [reset-commands!]]
   [com.palletops.cli.command.help :refer [help]]))

(deftest help-test
  (reset-commands!)
  (is (= "Unnamed Unknown version - Unknown SHA\n\ndesc\n\n"
         (with-out-str
           (help {:cli/config {:project-description "desc"}
                  :option-descriptor []}))))
  (is (= "Unnamed Unknown version - Unknown SHA\n\ndesc\n\n
 Switches       Default  Desc        \n --------       -------  ----        \n -a, --aswitch           Some switch \n\n"
         (with-out-str
           (help {:cli/config {:project-description "desc"}
                  :option-descriptor [["-a" "--aswitch" "Some switch"]]}))))
  (is (= "Unnamed Unknown version - Unknown SHA\n\ndesc\n
Available commands:
cmd1                 cmd1 ns doc\n
 Switches       Default  Desc        \n --------       -------  ----        \n -a, --aswitch           Some switch \n\n"
         (with-out-str
           (reset-commands!)
           (help {:cli/config
                  {:project-description "desc"
                   :ns-prefixes ["com.palletops.cli.command.help_test."]}
                  :option-descriptor [["-a" "--aswitch" "Some switch"]]})))))
