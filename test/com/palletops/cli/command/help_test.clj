(ns com.palletops.cli.command.help-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.cli.resolve :refer [reset-commands!]]
   [com.palletops.cli.command.help :refer [help]]))

(defn main
  {:cli/description "desc"}
  [])

(defn main2
  {:cli/description "desc"
   :cli/option-descriptors [["-a" "--aswitch" "Some switch"]]}
  [])

(deftest help-test
  (reset-commands!)
  (is (= "Unnamed Unknown version - Unknown SHA\n\ndesc\n\n     main \n\n"
         (with-out-str
           (help {:cli/config {:main-var #'main}
                  :option-descriptor []}
                 []))))
  (is (= "Unnamed Unknown version - Unknown SHA\n\ndesc\n\n     main2 \n
  -a, --aswitch  Some switch\n\n"
         (with-out-str
           (help {:cli/config {:main-var #'main2}}
                 [])))))
