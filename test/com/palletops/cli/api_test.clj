(ns com.palletops.cli.api-test
  (:require
   [clojure.test :refer :all]
   [com.palletops.cli.api :refer :all]))

(deftest options-and-args-test
  (is (= [{:option-a 1} [1]]
         (options-and-args
          ["-a" "1" "1"]
          [["arg1" "Arg1" :parse-fn #(Integer/parseInt %)]]
          [["-a" "--option-a" "Option" :parse-fn #(Integer/parseInt %)]]))))
