(ns com.palletops.cli.utils-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.cli.utils :refer :all]))

(deftest doc-string-test
  (is (= "desc" (doc-string "desc" nil)))
  (is (= "desc" (doc-string "desc" [])))
  (is (= "desc\n
 Switches       Default  Desc        \n --------       -------  ----        \n -a, --aswitch           Some switch \n"
         (doc-string "desc" [["-a" "--aswitch" "Some switch"]]))))
