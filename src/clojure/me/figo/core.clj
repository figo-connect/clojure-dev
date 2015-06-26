(ns me.figo.core
  (:require [base64-clj.core :as base64]
            [clj-http.client :as client]
            [clojure.data.json :as json])
  (:import [java.net URLEncoder]))


(def ^:dynamic figo-api-end-point "https://api.figo.me/")
(def ^:dynamic debug false )
(def ^:dynamic timeout 5000)


(defn as-url-encode
  [v]
  (URLEncoder/encode v "ISO-8859-1" ))


(defn assoc-debug
  [req]
  (if-not debug
    req
    (-> req
        (assoc :debug true)
        (assoc :debug-body true))))


(defn make-auth-header
  ;:todo Documentation
  [client-id client-secret]
  (let [basic (base64/encode (str client-id ":" client-secret))
        headers {:headers {"Authorization: Basic " basic
                           "username"              client-id
                           "password"              client-secret}
                 :body-encoding "UTF-8"
                 :socket-timeout timeout  ;; in milliseconds
                 :conn-timeout timeout    ;; in milliseconds
                 :accept :json}]
    (assoc-debug headers)))


(defn make-token-header
  ;:todo Documentation
  [token]
  (let [headers {:headers  {"Authorization" (str "Bearer " token)
                            "Accept"        "application/json"
                            "Content-Type"  "application/json"}
                 :body-encoding "UTF-8"
                 :socket-timeout timeout  ;; in milliseconds
                 :conn-timeout timeout    ;; in milliseconds
                 :accept :json}]
    (assoc-debug headers)))


(defn get-login-url
  ;:todo Documentation
  [client-id redirect-url scope state ]
  (try
    (str figo-api-end-point
         "/auth/code?response_type=code&client_id=" (as-url-encode client-id)
         "&redirect_uri=" (as-url-encode redirect-url)
         "&scope=" (as-url-encode scope)
         "&state=" (as-url-encode state))
    (catch Exception e
      nil)))


(defn convert-authentication-code
  ;:todo Documentation
  [client-id client-secret auth-code]
  {:pre [(string? auth-code)
         (string? client-id)
         (string? client-secret)]}
  (let [req (-> (make-auth-header client-id client-secret)
                (assoc :form-params {"grant_type" "authorization_code"
                                     "code"       auth-code}))]
    (-> (client/post (str figo-api-end-point "auth/token") req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn convert-refresh-token
  ;:todo Documentation
  [client-id client-secret refresh-token]
  (let [req (-> (make-auth-header client-id client-secret)
                (assoc :form-params {"grant_type" "refresh_token"
                                     "code"       refresh-token}))]
    (-> (client/post (str figo-api-end-point "auth/token") req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn revoke-token
  ;:todo Documentation
  [client-id client-secret refresh-token]
  (let [req (make-auth-header client-id client-secret)]
    (client/get (str figo-api-end-point "/auth/revoke?token=" (as-url-encode refresh-token)) req)
    nil))


(defn credential-login
  ;:todo Documentation
  [client-id client-secret user-name password]
  {:pre [(string? user-name)
         (string? password)
         (string? client-id)
         (string? client-secret)]}
  (let [req (-> (make-auth-header client-id client-secret)
                (assoc :form-params {"grant_type" "password"
                                     "username"   user-name
                                     "password"   password}))
        url (str figo-api-end-point "auth/token") ]
    (-> (client/post url req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn add-user*
  ;:todo Documentation
  [client-id client-secret user]
  (let [req (-> (make-auth-header client-id client-secret)
                (assoc :form-params user))]
    (-> (client/post (str figo-api-end-point "auth/user") req)
        (:body)
        (json/read-str :key-fn keyword)
        (:recovery_password))))



(defn add-user
  ;:todo Documentation
  [client-id client-secret & {:as user}]
  (add-user* client-id client-secret user))


(defn add-user-and-login
  ;:todo Documentation
  [client-id client-secret & {:keys [email password] :as user}]
  (do
    (add-user* client-id client-secret user)
    (credential-login client-id client-secret email password)))


(defn get-user
  ;:todo Documentation
  [token ]
  (let [header (make-token-header token)
        url (str figo-api-end-point "rest/user")]
    (-> (client/get url header)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-user
  ;:todo Documentation
  [token & {:as user}]
  (let [req  (-> (make-token-header token)
                 (assoc :form-params user))
        url (str figo-api-end-point "rest/user")]
    (-> (client/put url req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn remove-user
  ;:todo Documentation
  [token]
  (let [req (make-token-header token)
        url (str figo-api-end-point "rest/user")]
    (client/delete url req)
    nil))


(defn get-supported-service
  ;:todo Documentation
  [token c-code]
  (let [req (make-token-header token)
        url (str figo-api-end-point "/rest/catalog/services/" c-code )]
    (-> (client/get url req)
        (:body)
        (json/read-str :key-fn keyword))))

(defn get-login-settings
  ;:todo Documentation
  [token c-code b-code]
  (let [req (make-token-header token)
        url (str figo-api-end-point "/rest/catalog/banks/" c-code "/" b-code )]
    (-> (client/get url req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn setup-new-account
  ;:todo Documentation
  [token & {:as a-request}]
  (let [req (make-token-header token)
        req (assoc req :from-params a-request)
        url (str figo-api-end-point "/rest/accounts")]
    (-> (client/post url req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn setup-and-sync-account
  [token & {:as a-request}]
  ; :todo Need to implement
  )




(defn get-accounts
  ;:todo Documentation
  [token]
  {:pre [(string? token)]}
  (let [params (make-token-header token)
        url (str figo-api-end-point "rest/accounts")]
    (-> (client/get url params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn get-account
  ;:todo Documentation
  [token id]
  {:pre [(string? token)
         [string? id]]}
  (let [params (make-token-header token)
        url (str figo-api-end-point "rest/accounts/" id)]
    (-> (client/get url params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-account
  ;:todo Documentation
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
  ;:todo Documentation
  [token id]
  {:pre [(string? token)
         [string? id]]}
  (let [header (make-token-header token)
        url (str figo-api-end-point "rest/accounts/" id)]
    (client/delete url  header )
    nil))



(defn get-account-balance
  ;:todo Documentation
  [token id]
  (let [params (make-token-header token)
        url (str figo-api-end-point "rest/accounts/"  id "/balance")]
    (-> (client/get url params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-account-balance
  ;:todo Documentation
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
  ;:todo Documentation
  [token & a-coll]
  (let [v-coll (mapv (fn [v] {:account_id v}) a-coll)
        a-coll (json/json-str {:accounts v-coll})
        p (-> (make-token-header token)
              (assoc :body a-coll))
        url (str figo-api-end-point "rest/accounts")]
    (client/put url p)
    nil))


(defn get-transactions
  ;:todo Documentation
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
  ;:todo Documentation
  [token account_id transaction_id & visited ]
  (let [header (make-token-header token)
        url   (str figo-api-end-point "rest/accounts/"  account_id  "/transactions/" transaction_id)
        p (if visited
            (assoc header :form-params {:visited visited})
            header)]
    (client/put url p)))


(defn modify-transactions
  ;:todo Documentation
  [token visited & {:keys [account_id]}]
  (let [header (make-token-header token)
        url (if account_id
              (str figo-api-end-point "rest/accounts/"  account_id  "/transactions")
              (str figo-api-end-point "rest/transactions" ))
        p (if visited
            (assoc header :form-params {:visited visited})
            header)]
    (client/put url p)))


(defn remove-transaction
  ;:todo Documentation
  [token account_id transaction_id]
  (let [header  (make-token-header token)
        url   (str figo-api-end-point "rest/accounts/"  account_id  "/transactions/" transaction_id)]
    (client/delete url header)))





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
          {:keys [access_token]} (credential-login m)
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


;;;;;;;;;;;;;;;;;, Security API



(defn get-security
  ;:todo Documentation
  [token account_id security_id & cents 	]
  (let [header (make-token-header token)
        url (str figo-api-end-point "rest/accounts/"  account_id  "/securities/" security_id)
        p (if cents
            (assoc header :query-params {:cents cents})
            header)]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn get-securities
  ;:todo Documentation
  [token & {:keys [account_id] :as s-t-map}]
  (let [p (-> (make-token-header token)
              (assoc :query-params (or s-t-map {})))
        url (if account_id
              (str figo-api-end-point "rest/accounts/"  account_id  "/securities")
              (str figo-api-end-point "rest/securities" ))]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn modify-securities
  ;:todo Documentation
  [token visited & {:keys [account_id]}]
  (let [header (make-token-header token)
        p (assoc header :form-params {:visited visited})
        url (if account_id
              (str figo-api-end-point "rest/accounts/"  account_id  "/securities/")
              (str figo-api-end-point "rest/securities"))]
    (client/put url p)
    nil))



;;;;;;;;;;;;;;; Bank


(defn get-bank
  ;:todo Documentation
  [token bank-id]
  (let [p (-> (make-token-header token))
        url (str figo-api-end-point "rest/bank/" bank-id)]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-bank
  ;:todo Documentation
  [token & {:keys [bank_id] :as s-t-map}]
  (let [p (-> (make-token-header token)
              (assoc :form-params (or s-t-map {})))
        url (str figo-api-end-point "rest/banks/" bank_id)]
    (->
      (client/put url p)
      (:body)
      (json/read-str :key-fn keyword))))


(defn remove-bank-pin
  ;:todo Documentation
  [token bank-id]
  (let [p (make-token-header token)
        url (str figo-api-end-point "rest/banks/" bank-id "/remove_pin")]
    (client/post url p)
    nil))


(defn get-notification
  ;:todo Documentation
  [token notification-id]
  (let [p (make-token-header token)
        url (str figo-api-end-point "rest/notifications/" notification-id )]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn add-notification
  ;:todo Documentation
  [token & { :as s-t-map}]
  (let [p (-> (make-token-header token)
              (assoc :form-params (or s-t-map {})))
        url (str figo-api-end-point "rest/notifications" )]
    (-> (client/post url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-notification
  ;:todo Documentation
  [token notification-id]
  (let [p (make-token-header token)
        url (str figo-api-end-point "rest/notifications/" notification-id )]
    (client/delete url p)
    nil
    ))


;;;;;;;;;;;;;;Payment


(defn get-payments
  ;:todo Documentation
  [token & {:keys [account_id payment_id]}]
  (let [p (make-token-header token)
        url (if account_id
              (if payment_id
                (str figo-api-end-point "rest/accounts/" account_id "/payments/" payment_id)
                (str figo-api-end-point "rest/accounts/" account_id "/payments"))
              (str figo-api-end-point "rest/payments"))
        ]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn add-payment
  ;:todo Documentation
  [token account_id & {:as payment}]
  (let [p (-> (make-token-header token)
              (assoc :form-params (or payment {})))
        url (str figo-api-end-point "rest/accounts/" account_id "/payments")]
    (-> (client/post url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn add-container-payment
  ;:todo Documentation
  [token & {:keys [account_id] :as payment}]
  (let [p (-> (make-token-header token)
              (assoc :form-params (or payment {})))
        url (str figo-api-end-point "rest/accounts/" account_id "/payments")]
    (-> (client/post url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn get-payment-proposal
  ;:todo Documentation
  [token ]
  (let [p (-> (make-token-header token))
        url (str figo-api-end-point "/rest/adress_book" )]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-payment
  ;:todo Documentation
  [token & {:keys [account_id payment_id] :as payment}]
  (let [p (-> (make-token-header token)
              (assoc :form-params (or payment {})))
        url (str figo-api-end-point "rest/accounts/" account_id "/payments/" payment_id)]
    (-> (client/put url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn remove-payment
  ;:todo Documentation
  [token account_id payment_id]
  (let [p (make-token-header token)
        url (str figo-api-end-point "rest/accounts/" account_id "/payments/" payment_id)]
    (-> (client/delete url p))
    nil))


(defn submit-payment
  ;:todo Documentation
  [token account_id payment_id tanSchemeId state & redirect-uri]
  (let [p {:account_id    account_id
           :payment_id    payment_id
           :state         state
           :tan_scheme_id tanSchemeId}
        p (if redirect-uri
            (assoc p :redirect_uri redirect-uri)
            p)
        p (-> (make-token-header token)
              (assoc :form-params p))
        url (str figo-api-end-point "rest/accounts/" account_id "/payments/" payment_id)

        resp (-> (client/post url p)
            (:body)
            (json/read-str :key-fn keyword))
        task-token (:task_token resp)]
    (str figo-api-end-point "/task/start?id=" task-token)))


(defn get-sync-url
  ;:todo Documentation
  [token state redirect-uri]
  (let [p {:state         state
           :redirect_uri redirect-uri}
        h (->(make-token-header token)
             (assoc :form-params p))
        url (str figo-api-end-point "rest/sync")
        resp (-> (client/post url h)
                 (:body)
                 (json/read-str :key-fn keyword))
        task-token (:task_token resp)]
    (str figo-api-end-point "/task/start?id=" task-token)))



(defn get-task-state
  ;:todo Documentation
  [token  & {:keys [id] :as token-resp}]
  (let [p (->(make-token-header token)
             (assoc :form-params (or token-resp {})))
        url (str figo-api-end-point "rest/progress?id=" id)]
    (-> (client/post url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn submit-response-to-task
  ;:todo Documentation
  [token id response type]
  (let [header (make-token-header token)
        req {:id id}
        req (condp = type
              :pin (assoc req :pin response)
              :save-pin (assoc req :save_pin response)
              :continue (assoc req :continue response)
              :challenge (assoc req :response response)
              req)
        req (-> header
                (assoc :form-params req))
        url (str figo-api-end-point "rest/progress?id=" id)]
    (-> (client/post url req)
        (:body)
        (json/read-str :key-fn keyword))))



(defn start-task
  ;:todo Documentation
  [token id]
  (let [header (make-token-header token)
        url (str figo-api-end-point "rest/progress?id=" id)]
    (-> (client/get url header))
    nil))


(defn cancel-task
  ;:todo Documentation
  [token id]
  (let [header (make-token-header token)
        url (str figo-api-end-point "rest/progress?id=" id)]
    (client/post url header)
    nil))


(defn create-process
  ;:todo Documentation
  [token & {:as b-process}]
  (let [header (make-token-header token)
        url (str figo-api-end-point "client/process" )
        req (-> header
                (assoc :form-params b-process))]
    (-> (client/post url req)
        (:body)
        (json/read-str :key-fn keyword))))