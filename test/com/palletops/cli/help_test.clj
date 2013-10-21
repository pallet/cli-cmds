(ns com.palletops.cli.help-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.cli.help :refer [doc-string static-help]]))

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
  (is (= "desc\n
 Switches       Default  Desc        \n --------       -------  ----        \n -a, --aswitch           Some switch \n"
         (doc-string "desc" [["-a" "--aswitch" "Some switch"]]))))
