(ns clj-commons.slingshot.test-test
  (:require
   [clj-commons.slingshot :refer [throw+]]
   [clj-commons.slingshot.test]
   [clojure.test :refer [deftest is]]))

(deftest test-slingshot-test-macros
  (is (thrown+? string? (throw+ "test")))
  (is (thrown+-with-msg? string? #"th" (throw+ "test" "hi there"))))
