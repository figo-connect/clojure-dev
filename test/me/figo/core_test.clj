(ns me.figo.core-test
  (:use clojure.test)
  (:require [me.figo.core :refer :all]))


(deftest credential-login-test
  (testing "token by user name "
    (let [client-id "CPocl5egXH1XQwV4XFGb5KGAVI5XihrmNC9ZKMm3Dyjc"
          secret    "Sl7mrzkzYprH7D5gdxiKyVMtyKF_xEtIOBsVsZ4VqbZ0"
          user-name  "mamuninfo@gmail.com"
          password  "letmein"
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


(comment
  (credential-login-test)
  )