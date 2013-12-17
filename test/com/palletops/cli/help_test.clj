(ns com.palletops.cli.help-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.cli.command :refer [def-command]]
   [com.palletops.cli.help :refer [doc-string help-message static-help]]))

(deftest static-help-test
  (testing "path"
    (is (= "test help\n"
           (static-help {:cli/config {:static-help-path "help"}} ["test"]))))
  (testing "format string"
    (is (= "test help\n"
           (static-help {:cli/config {:static-help-path "help/%s"}} ["test"]))))
  (testing "sub-help"
    (is (= "sub-test text\n"
           (static-help
            {:cli/config {:static-help-path "help"}}
            ["sub" "test"])))))

(deftest doc-string-test
  (is (= "desc" (doc-string "desc" nil)))
  (is (= "desc" (doc-string "desc" [])))
  (is (= "desc\n  -a, --aswitch  Some switch\n"
         (doc-string "desc" [["-a" "--aswitch" "Some switch"]]))))

(def-command ^:cli/main test-main "Test main." [])

(deftest help-message-test
  (is
   (= "Unnamed Unknown version - Unknown SHA\n\nTest main.\n\n      command*\n"
      (help-message
          {:cli/config {:main-var #'test-main}}
          [])))
  (is (= "Project 1.2.3 - 123\n\nTest main.\n\n      command*\n"
         (help-message
          {:cli/config {:project-name "Project"
                        :version "1.2.3"
                        :sha "123"
                        :main-var #'test-main}}
          [])))
  (is (= "Project 1.2.3 - 123\n\n\n\n     cmd1 \n"
         (help-message
          {:cli/config {:project-name "Project"
                        :version "1.2.3"
                        :sha "123"
                        :main-var #'test-main
                        :ns-prefixes ["com.palletops.cli.command.help-test."]}}
          ["cmd1"])))
  (is (= "Project 1.2.3 - 123\n\n\n\n     help command*\n"
         (help-message
          {:cli/config {:project-name "Project"
                        :version "1.2.3"
                        :sha "123"
                        :main-var #'test-main
                        :ns-prefixes ["com.palletops.cli.command.help-test."]
                        :commands ['com.palletops.cli.command.help
                                   'com.palletops.cli.command.version]}}
          ["help"]))))
