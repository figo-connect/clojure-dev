(ns me.figo.core
  (:require [base64-clj.core :as base64]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [clojure.data.json :as json]))


(def ^:dynamic figo-api-end-point "https://api.figo.me/")


(defn token-by-auth
  [{:keys [auth-code client-id secret debug debug-body]}]
  {:pre [(string? auth-code)
         (string? client-id)
         (string? secret)]}
  (let [basic (base64/encode (str client-id ":" secret))
        params {:form-params {"grant_type" "authorization_code"
                              "code"       auth-code}
                :headers     {"Authorization: Basic " basic
                              "username"              client-id
                              "password"              secret}}
        params (if debug (assoc params :debug true) params)
        params (if debug-body (assoc params :debug-body params) params)]
    (-> (client/post (str figo-api-end-point "auth/token") params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn token-by-user
  [{:keys [user-name password client-id secret debug debug-body]}]
  {:pre [(string? user-name)
         (string? password)
         (string? client-id)
         (string? secret)]}
  (let [basic (base64/encode (str client-id ":" secret))
        params {:form-params {"grant_type" "password"
                              "username"   user-name
                              "password"   password}
                :headers     {"Authorization: Basic " basic}}
        params (if debug (assoc params :debug true) params)
        params (if debug-body (assoc params :debug-body params) params)]
    (-> (client/post (str figo-api-end-point "auth/token") params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn accounts
  [{:keys [access_token debug debug-body]}]
  {:pre [(string? access_token)]}
  (let [acc-token (str "Bearer " access_token)
        params {:headers {"Authorization"  acc-token
                          "Accept" "application/json"
                          "Content-Type" "application/json"}}
        params (if debug (assoc params :debug true) params)
        params (if debug-body (assoc params :debug-body params) params)]
    (-> (client/get (str figo-api-end-point "rest/accounts") params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn account-by-id
  [{:keys [access_token debug debug-body account_id]}]
  {:pre [(string? access_token)
         (string? account_id)]}
  (let [acc-token (str "Bearer " access_token)
        params {:headers {"Authorization"  acc-token
                          "Accept" "application/json"
                          "Content-Type" "application/json"}}
        params (if debug (assoc params :debug true) params)
        params (if debug-body (assoc params :debug-body params) params)]
    (-> (client/get (str figo-api-end-point "rest/accounts/" account_id) params)
        (:body)
        (json/read-str :key-fn keyword))))


