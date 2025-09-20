(ns clj-commons.slingshot.issue31-test
  "See https://github.com/scgilardi/slingshot/issues/31 for context."
  (:require [clj-commons.slingshot :refer [try+ throw+]]
            [clojure.test :refer [deftest is]]))

(defn stacktrace-depth
  ([t] (stacktrace-depth 1 t))
  ([i ^Throwable t]
   (if (.getCause t)
     (recur (inc i) (.getCause t))
     i)))

(defn stacking-up []
  (try+
   (try+
    (try+
     (try+
      (throw (RuntimeException. "Something went wrong"))
      (catch RuntimeException _e
        (throw+ {:some-info "here"
                 :type 1}
                (:cause &throw-context)
                (:message &throw-context))))
     (catch [:type 1] {:as error-stuff}
       (throw+ (assoc error-stuff :more-info "over there")
               (:cause &throw-context)
               (:message &throw-context))))
    (catch [:type 1] {:as error-stuff}
      (throw+ (assoc error-stuff :one-last "thing")
              (:cause &throw-context)
              (:message &throw-context))))
   (catch (constantly true) {:as err-ctx}
     [(stacktrace-depth (:throwable &throw-context))
      (.getMessage (:throwable &throw-context))
      err-ctx])))

(comment
  (stacking-up)
  )

(deftest test-stacking-up
  (let [[depth msg err-map] (stacking-up)]
    (is (= 1 depth))
    (is (= "Something went wrong" msg))
    (is (= {:one-last "thing"
            :more-info "over there"
            :some-info "here"
            :type 1} err-map))))
