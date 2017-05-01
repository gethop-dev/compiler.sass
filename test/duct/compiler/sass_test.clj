(ns duct.compiler.sass-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer :all]
            [duct.compiler.sass :as sass]
            [integrant.core :as ig]))

(defmacro with-temp-files [fs & body]
  `(let [fs# ~fs]
     (try (doseq [f# fs#] (io/delete-file f# true))
          ~@body
          (finally (doseq [f# fs#] (io/delete-file f# true))))))

(def config
  {:duct.compiler/sass
   {:source-paths ["test/sass"]
    :output-path  "target/test/output"}})

(deftest init-test
  (testing "normal output"
    (let [expected (io/file "test/sass/test.css")
          actual   (io/file "target/test/output/test.css")]
      (with-temp-files [actual]
        (ig/init config)
        (is (.exists actual))
        (is (= (slurp expected) (slurp actual))))))

  (testing "compressed output"
    (let [expected (io/file "test/sass/test.compressed.css")
          actual   (io/file "target/test/output/test.css")]
      (with-temp-files [actual]
        (ig/init (assoc-in config [:duct.compiler/sass :output-style] :compressed))
        (is (.exists actual))
        (is (= (slurp expected) (slurp actual))))))

  (testing "different indent"
    (let [expected (io/file "test/sass/test.indent.css")
          actual   (io/file "target/test/output/test.css")]
      (with-temp-files [actual]
        (ig/init (assoc-in config [:duct.compiler/sass :indent] "    "))
        (is (.exists actual))
        (is (= (slurp expected) (slurp actual))))))

  (testing "sass input"
    (let [expected (io/file "test/sass/test.css")
          actual   (io/file "target/test/output/test3.css")]
      (with-temp-files [actual]
        (ig/init config)
        (is (.exists actual))
        (is (= (slurp expected) (slurp actual))))))

  (testing "imports"
    (let [expected (io/file "test/sass/test4.css")
          actual   (io/file "target/test/output/test4.css")]
      (with-temp-files [actual]
        (ig/init config)
        (is (not (.exists (io/file "target/test/output/_reset.css"))))
        (is (.exists actual))
        (is (= (slurp expected) (slurp actual)))))))

(deftest resume-test
  (.mkdirs (io/file "target/test/temp"))

  (testing "modified file"
    (let [source (io/file "target/test/temp/test.scss")
          output (io/file "target/test/output/test.css")]
      (with-temp-files [source output]
        (io/copy (io/file "test/sass/test.scss") source)
        (let [config (assoc-in config
                               [:duct.compiler/sass :source-paths]
                               ["target/test/temp"])
              system (ig/init config)]
          (Thread/sleep 1000)
          (io/copy (io/file "test/sass/test2.scss") source)
          (ig/resume config system)
          (is (.exists output))
          (is (= (slurp output) (slurp (io/file "test/sass/test2.css"))))))))

  (testing "new file"
    (let [source1 (io/file "target/test/temp/test.scss")
          source2 (io/file "target/test/temp/test2.scss")
          output1 (io/file "target/test/output/test.css")
          output2 (io/file "target/test/output/test2.css")]
      (with-temp-files [source1 source2 output1 output2]
        (io/copy (io/file "test/sass/test.scss") source1)
        (let [config (assoc-in config
                               [:duct.compiler/sass :source-paths]
                               ["target/test/temp"])
              system (ig/init config)]
          (Thread/sleep 1000)
          (io/copy (io/file "test/sass/test2.scss") source2)
          (ig/resume config system)
          (is (.exists output1))
          (is (.exists output2))
          (is (= (slurp output1) (slurp (io/file "test/sass/test.css"))))
          (is (= (slurp output2) (slurp (io/file "test/sass/test2.css")))))))))
