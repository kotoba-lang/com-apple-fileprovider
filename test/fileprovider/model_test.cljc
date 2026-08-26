(ns fileprovider.model-test
  (:require [clojure.test :refer [deftest is testing]]
            [fileprovider.model :as model]))

(deftest modes-are-independent
  (let [base model/defaults]
    (is (model/valid? base))
    (is (= :manual (:sync/schedule (model/set-schedule base :manual))))
    (is (= :pinned (:sync/residency (model/set-residency base :pinned))))
    (is (false? (model/should-poll? (model/set-schedule base :manual))))
    (is (= [:scan-local :fetch-remote]
           (model/command (model/set-schedule base :manual) :sync-now)))
    (is (= [:fail-paused]
           (model/command (model/set-schedule base :paused) :sync-now)))))

(deftest open-materialises-placeholder
  (is (= [:download] (model/command model/defaults :open)))
  (is (= :materialized
         (:sync/local-state
          (model/transition
           (model/transition model/defaults :download-started)
           :download-succeeded)))))

(deftest eviction-is-lossless
  (testing "verified clean automatic materialisation may be evicted"
    (let [clean (assoc model/defaults :sync/local-state :materialized)]
      (is (model/can-evict? clean))
      (is (= [:evict] (model/command clean :disk-pressure)))))
  (testing "dirty, unverified and pinned bytes are never evicted"
    (doseq [item [(assoc model/defaults :sync/local-state :dirty
                        :sync/pending-local? true)
                  (assoc model/defaults :sync/local-state :materialized
                        :sync/remote-verified? false)
                  (assoc model/defaults :sync/local-state :materialized
                        :sync/residency :pinned)]]
      (is (false? (model/can-evict? item)))
      (is (empty? (model/command item :disk-pressure))))))

(deftest badges-expose-real-state
  (is (= :cloud (model/badge model/defaults)))
  (is (= :available-offline
         (model/badge (assoc model/defaults
                             :sync/local-state :materialized
                             :sync/residency :pinned))))
  (is (= :pending-upload
         (model/badge (model/transition model/defaults :local-edited))))
  (is (= :conflict
         (model/badge (model/transition model/defaults :remote-conflict)))))
