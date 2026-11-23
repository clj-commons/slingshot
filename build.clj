(ns build
  "Slingshot's build script. Entirely driven by `bb`."
  (:require [babashka.deps-deploy :as dd]
            [babashka.tasks :refer [shell]]
            [clojure.tools.build.api :as b]
            [clojure.tools.deps.edn :as deps]))

(def lib 'org.clj-commons/slingshot)
(def version (-> (deps/project-deps) :aliases :neil :project :version))
(def snapshot
  (let [[_ m-m rel] (re-find #"(.*)\.(\d+)$" version)]
    (str m-m "." (inc (parse-long rel)) "-SNAPSHOT")))
(def class-dir "target/classes")

(defn- pom-template [version]
  [[:description "Enhanced throw, try, leveraging Clojure's capabilities."]
   [:url "https://github.com/clj-commons/slingshot"]
   [:licenses
    [:license
     [:name "Eclipse Public License"]
     [:url "http://www.eclipse.org/legal/epl-v10.html"]]]
   [:developers
    [:developer
     [:name "Stephen Gilardi"]]]
   [:scm
    [:url "https://github.com/clj-commons/slingshot"]
    [:connection "scm:git:https://github.com/clj-commons/slingshot.git"]
    [:developerConnection "scm:git:ssh:git@github.com:clj-commons/slingshot.git"]
    [:tag (str "v" version)]]])

(defn- jar-opts [opts]
  (let [version (if (:snapshot opts) snapshot version)]
    (assoc opts
           :lib lib   :version version
           :jar-file  (format "target/%s-%s.jar" lib version)
           :basis     (b/create-basis {})
           :class-dir class-dir
           :target    "target"
           :src-dirs  ["src"]
           :pom-data  (pom-template version))))

(defn jar "Build the JAR file."
  {:org.babashka/cli {:spec {:snapshot {:coerce :boolean}}}}
  [opts]
  (let [opts (jar-opts opts)]
    (b/delete {:path "target"})
    (println "\nWriting pom.xml...")
    (b/write-pom opts)
    (println "\nCopying source...")
    (b/copy-dir {:src-dirs ["resources" "src"] :target-dir class-dir})
    (println "\nBuilding" (:jar-file opts) "...")
    (b/jar opts))
  opts)

(defn deploy "Deploy the JAR to Clojars."
  {:org.babashka/cli {:spec {:snapshot {:coerce :boolean}}}}
  [opts]
  (let [{:keys [jar-file] :as opts} (jar-opts opts)]
    (dd/deploy {:installer :remote :artifact (b/resolve-path jar-file)
                :pom-file (b/pom-path (select-keys opts [:lib :class-dir]))}))
  opts)

;; test-related utilities and tasks:

(defn- get-versions [opts]
  (if (:all-versions opts) ["1.8" "1.9" "1.10" "1.11" "1.12" "1.13"] ["1.8"]))

(defn run-tests "Run tests for the specified Clojure versions."
  {:org.babashka/cli {:spec {:all-versions {:coerce :boolean}}}}
  [opts]
  (let [versions (get-versions opts)]
    (doseq [v versions]
      (println (str "\nTesting Clojure " v))
      (shell (str "clojure -M"
                  ":" v
                  ":test:runner")))))
