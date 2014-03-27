{:dev {:plugins [[codox/codox.leiningen "0.6.4"]
                 [lein-marginalia "0.7.1"]
                 [lein-pallet-release "0.1.3"]]
       :pallet-release
       {:url "https://pbors:${GH_TOKEN}@github.com/pallet/cli-cmds.git",
        :branch "master"}}
 :doc {:dependencies [[com.palletops/pallet-codox "0.1.0"]]

       :codox {:writer codox-md.writer/write-docs
               :output-dir "doc/0.1/api"
               :src-dir-uri "https://github.com/pallet/cli-cmds/blob/develop"
               :src-linenum-anchor-prefix "L"}
       :aliases {"marg" ["marg" "-d" "doc/0.1/annotated"]
                 "codox" ["doc"]
                 "doc" ["do" "codox," "marg"]}}
 :release
 {:set-version
  {:updates [{:path "README.md" :no-snapshot true}]}}
 :no-checkouts {:checkout-deps-shares ^:replace []}} ; disable checkouts
