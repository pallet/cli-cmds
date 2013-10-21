(ns com.palletops.cli.resolve-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.cli.resolve
    :refer [commands reset-commands! resolve-command suggestions]]))

(deftest commands-test
  (testing "ns-prefixes"
    (is (= {"cmd1" 'com.palletops.cli.command.help-test.cmd1
            "cmd1.cmd2" 'com.palletops.cli.command.help-test.cmd1.cmd2}
           (do
             (reset-commands!)
             (commands
              {:cli/config
               {:ns-prefixes ["com.palletops.cli.command.help_test."]}})))))
  (testing "commands"
    (is (= {"help" 'com.palletops.cli.command.help}
           (do
             (reset-commands!)
             (commands {:cli/config
                        {:commands ['com.palletops.cli.command.help]}}))))))

(deftest resolve-command-test
  (testing "ns-prefixes"
    (let [v (do
              (reset-commands!)
              (resolve-command
               {:cli/config
                {:ns-prefixes ["com.palletops.cli.command.help_test."]}}
               "" "cmd1"))]
      (is (var? v))
      (is (= 'cmd1 (:name (meta v))))
      (is (= 'com.palletops.cli.command.help-test.cmd1
             (ns-name (:ns (meta v))))))
    (testing "sub-command"
      (let [v (do
                (reset-commands!)
                (resolve-command
                 {:cli/config
                  {:ns-prefixes ["com.palletops.cli.command.help_test."]}}
                 "cmd1" "cmd2"))]
        (is (var? v))
        (is (= 'cmd2 (:name (meta v))))
        (is (= 'com.palletops.cli.command.help-test.cmd1.cmd2
               (ns-name (:ns (meta v))))))))
  (testing "commands"
    (let [v (do
              (reset-commands!)
              (resolve-command
               {:cli/config
                {:commands ['com.palletops.cli.command.help]}}
               "" "help"))]
      (is (var? v))
      (is (= 'help (:name (meta v))))
      (is (= 'com.palletops.cli.command.help
             (ns-name (:ns (meta v)))))))
  )

(deftest suggestions-test
  (reset-commands!)
  (is (= ["help"]
         (suggestions
          "halp"
          (keys (commands {:cli/config
                           {:ns-prefixes ["com.palletops.cli.command."]}}))))))
