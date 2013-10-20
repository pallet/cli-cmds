(ns com.palletops.cli.help-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.cli.help :refer [static-help]]))

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
