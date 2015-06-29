(ns me.figo.core-credential-test
  (:use clojure.test)
  (:require [me.figo.core :refer :all]))


(deftest login-url-test
  (testing "Testing login url "
    (let [client-id "CPocl5egXH1XQwV4XFGb5KGAVI5XihrmNC9ZKMm3Dyjc"
          redirect-url "http//localhost:9000"
          scope "web"
          state "hello"
          expected-result "https://api.figo.me//auth/code?response_type=code&client_id=CPocl5egXH1XQwV4XFGb5KGAVI5XihrmNC9ZKMm3Dyjc&redirect_uri=http%2F%2Flocalhost%3A9000&scope=web&state=hello"
          actual-result (login-url client-id redirect-url scope state)]
      (is (= actual-result expected-result)))))


(deftest credential-login-test
  (testing "token by user name "
    (let [client-id "CaESKmC8MAhNpDe5rvmWnSkRE_7pkkVIIgMwclgzGcQY"
          secret    "STdzfv0GXtEj_bwYn7AgCVszN1kKq5BdgEIKOM_fzybQ"
          user-name  "demo@figo.me"
          password  "demo1234"
          {:keys [access_token
                  expires_in
                  refresh_token
                  scope
                  token_type]} (credential-login client-id secret user-name password)]
      (is (not= nil access_token))
      (is (not= nil expires_in))
      (is (not= nil refresh_token))
      (is (not= nil scope))
      (is (not= nil token_type)))))


(deftest convert-authentication-code-test
  (testing "convert-authentication-code "
    (let [client-id "CaESKmC8MAhNpDe5rvmWnSkRE_7pkkVIIgMwclgzGcQY"
          secret    "STdzfv0GXtEj_bwYn7AgCVszN1kKq5BdgEIKOM_fzybQ"
          auth-code  ""
          actual-result (convert-authentication-code client-id secret auth-code)]
      (is (not= nil actual-result)))))