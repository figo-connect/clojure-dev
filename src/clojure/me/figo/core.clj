(ns me.figo.core
  (:require [base64-clj.core :as base64]
            [clojure.tools.logging :as log]
            [clj-http.client :as client]
            [clojure.data.json :as json]))


(def ^:dynamic figo-api-end-point "https://api.figo.me/")
(def ^:dynamic debug false )
(def ^:dynamic debug-body false )


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
        params (if debug-body (assoc params :debug-body params) params)
        url (str figo-api-end-point "auth/token") ]
    (-> (client/post url params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn make-token-header
  "todo: Documentation "
  [token]
  (let [headers {:headers {"Authorization" (str "Bearer " token)
                   "Accept"        "application/json"
                   "Content-Type"  "application/json"}
                 :body-encoding "UTF-8"}
        headers (if debug
                  (assoc headers :debug true)
                  headers)
        headers (if debug-body
                  (assoc headers :debug-body true)
                  headers)]
    headers))


(defn get-accounts
  "todo: Documentation "
  [token]
  {:pre [(string? token)]}
  (let [params (make-token-header token)
        url (str figo-api-end-point "rest/accounts")]
    (-> (client/get url params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn get-account
  "todo: Documentation "
  [token id]
  {:pre [(string? token)
         [string? id]]}
  (let [params (make-token-header token)
        url (str figo-api-end-point "rest/accounts/" id)]
    (-> (client/get url params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-account
  "todo: Documentation "
  [token a-map]
  {:pre [(string? token)
         [map? a-map]]}
  (let [select-ks [:account_id :name :owner :preferred_tan_scheme :auto_sync]
        a-map (select-keys a-map select-ks)
        id (:account_id a-map)
        p (-> (make-token-header token)
              (assoc :form-params a-map))
        url (str figo-api-end-point "rest/accounts/" id)]
    (client/put url p)
    nil))


(defn remove-account
  "todo: Documentation "
  [token id]
  {:pre [(string? token)
         [string? id]]}
  (let [header (make-token-header token)
        url (str figo-api-end-point "rest/accounts/" id)]
    (client/delete url  header )
    nil))



(defn get-account-balance
  "todo: Documentation "
  [token id]
  (let [params (make-token-header token)
        url (str figo-api-end-point "rest/accounts/"  id "/balance")]
    (-> (client/get url params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-account-balance
  "todo: Documentation "
  [token b-map]
  (let [select-ks [:account_id :credit_line :monthly_spending_limit :cents  ]
        account-id (:account_id b-map)
        b-map (select-keys b-map select-ks)
        p (-> (make-token-header token)
              (assoc :form-params b-map))
        url (str figo-api-end-point "rest/accounts/" account-id "/balance")]
    (client/put url p)
    nil))


(defn set-account-orders
  "todo: Documentation "
  [token & a-coll]
  (let [v-coll (mapv (fn [v] {:account_id v}) a-coll)
        a-coll (json/json-str {:accounts v-coll})
        p (-> (make-token-header token)
              (assoc :body a-coll))
        url (str figo-api-end-point "rest/accounts")]
    (client/put url p)
    nil))


(defn get-transactions
  "todo: Documentation "
  [token & {:keys [account_id] :as s-t-map}]
  (let [url (if account_id
              (str figo-api-end-point "rest/accounts/"  account_id  "/transactions?")
              (str figo-api-end-point "rest/transactions?" ))
        p (-> (make-token-header token)
              (assoc :query-params (or s-t-map {})))]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))



(defn modify-transaction
  "todo: Documentation "
  [token account_id transaction_id & visited ]
  (let [header (make-token-header token)
        url   (str figo-api-end-point "rest/accounts/"  account_id  "/transactions/" transaction_id)
        p (if visited
            (assoc header :form-params {:visited visited})
            header)]
    (client/put url p)))


(comment



  (json/json-str {:accounts [{:account_id "A1117215.2" }
                             {:account_id "A1117215.1" }
                             {:account_id "A1117215.3" }]})

  (binding [debug-body false
            debug false]
    (let [m {:client-id "CPocl5egXH1XQwV4XFGb5KGAVI5XihrmNC9ZKMm3Dyjc"
             :secret    "Sl7mrzkzYprH7D5gdxiKyVMtyKF_xEtIOBsVsZ4VqbZ0"
             :user-name "mamuninfo@gmail.com"
             :password  "letmein"}
          {:keys [access_token]} (token-by-user m)
          w (:accounts (get-accounts access_token))
          amap (map :account_id w)
          _ (println amap)
          a (get-in w [0 :account_id])
          ;         r (get-account access_token a)
          ; r (assoc r :auto_sync "false"  )
          ;  _ (clojure.pprint/pprint r)
          ;r (update-account access_token (dissoc r :preferred_tan_scheme :auto_sync))
          ;_ (println a)
          r (get-account-balance access_token a)
          ;_ (println r)
          ;r (update-account-balance access_token a (assoc r :credit_line 300)  )

          r {:accounts [{:account_id "A1117215.2" }
                        {:account_id "A1117215.1" }
                        {:account_id "A1117215.3" }]}
;          r (set-account-orders access_token r)
          ]
      (-> (get-transactions access_token :account_id "A1117215.2")

          (clojure.pprint/pprint))
      ))

  )

;accountId, String since, Integer count, Integer offset, Boolean include_pending




