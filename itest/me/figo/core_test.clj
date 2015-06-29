(ns me.figo.core-test
  (:use clojure.test)
  (:require [me.figo.core :refer :all]))


(def token (atom nil))

(defn load-token []
  (let [client-id "CaESKmC8MAhNpDe5rvmWnSkRE_7pkkVIIgMwclgzGcQY"
        secret    "STdzfv0GXtEj_bwYn7AgCVszN1kKq5BdgEIKOM_fzybQ"
        user-name  "demo@figo.me"
        password  "demo1234"
        {:keys [access_token]} (credential-login client-id secret user-name password)]
    (reset! token access_token)))

(use-fixtures :once load-token)


(deftest get-account-test
  (testing "get-account all "
    (let [actual-result (get-account @token )]
      (is (not= nil actual-result))))
  (testing "get-account with id"
    (let [actual-result (get-account @token "A1.2")]
      (is (not= nil actual-result)))))


(deftest get-account-balance-test
  (testing "get-account-balance"
    (let [actual-result (get-account-balance @token "A1.2")]
      (is (not= nil actual-result)))))


(deftest get-transaction-test
  (testing "get-account-transaction"
    (let [actual-result (get-transactions @token)]
      (is (not= nil actual-result))))
  (testing "get-account-transaction"
    (let [actual-result (get-transactions @token :account_id "A1.2")]
      (is (not= nil actual-result)))))


(deftest get-account-payment-test
  (testing "get-account-payment"
    (let [actual-result (get-payments @token )]
      (is (not= nil actual-result))))
  (testing "get-account-payment with account "
    (let [actual-result (get-payments @token :account_id "A1.2")]
      (is (not= nil actual-result)))))









