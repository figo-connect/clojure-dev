(ns me.figo.core
  (:require [clj-http.client :as client]
            [clojure.data.json :as json]
            [me.figo.util :as u]))


(def ^:dynamic figo-api-end-point "https://api.figo.me/")
(def ^:dynamic debug false )
(def ^:dynamic timeout 5000)

(defn assoc-default
  [req]
  (-> req
      (u/assoc-debug debug)
      (u/assoc-timeout timeout)))

(defn login-url
  "The URL a user should open in his/her web browser to start the login process. When the process is completed, the user is redirected to the URL provided
   to the constructor and passes on an authentication code. This code can be converted into an access token for data access."
  [^String client-id ^String redirect-url ^String scope ^String state]
  (try
    (str figo-api-end-point
         "/auth/code?response_type=code&client_id=" (u/as-url-encode client-id)
         "&redirect_uri=" (u/as-url-encode redirect-url)
         "&scope=" (u/as-url-encode scope)
         "&state=" (u/as-url-encode state))
    (catch Exception e
      nil)))


(defn convert-authentication-code
  "Convert the authentication code received as result of the login process into an access token usable for data access."
  [^String client-id ^String client-secret ^String auth-code]
  {:pre [(string? auth-code)
         (string? client-id)
         (string? client-secret)]}
  (let [req (-> (u/auth-req client-id client-secret)
                (assoc-default)
                (assoc :form-params {"grant_type" "authorization_code"
                                     "code"       auth-code}))]
    (-> (client/post (str figo-api-end-point "auth/token") req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn convert-refresh-token
  "Convert a refresh token (granted for offline access and returned by `convert_authentication_code`) into an access token usabel for data acccess."
  [^String client-id ^String client-secret ^String refresh-token]
  {:pre [(string? refresh-token)
         (string? client-id)
         (string? client-secret)]}
  (let [req (-> (u/auth-req client-id client-secret)
                (assoc-default)
                (assoc :form-params {"grant_type" "refresh_token"
                                     "code"       refresh-token}))]
    (-> (client/post (str figo-api-end-point "auth/token") req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn revoke-token
  "Revoke a granted access or refresh token and thereby invalidate it. Note: this action has immediate effect, i.e. you will not be able use that token
   anymore after this call."
  [^String client-id ^String client-secret ^String refresh-token]
  {:pre [(string? refresh-token)
         (string? client-id)
         (string? client-secret)]}
  (let [req (-> (u/auth-req client-id client-secret)
                (assoc-default))]
    (client/get (str figo-api-end-point "/auth/revoke?token=" (u/as-url-encode refresh-token)) req)
    nil))


(defn credential-login
  "Login an user with his figo username and password credentials"
  [^String client-id ^String client-secret ^String user-name ^String password]
  {:pre [(string? user-name)
         (string? password)
         (string? client-id)
         (string? client-secret)]}
  (let [req (-> (u/auth-req client-id client-secret)
                (assoc-default)
                (assoc :form-params {"grant_type" "password"
                                     "username"   user-name
                                     "password"   password}))
        url (str figo-api-end-point "auth/token") ]
    (-> (client/post url req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn add-user*
  ;:todo Documentation
  [^String client-id ^String client-secret ^String user]
  (let [req (-> (u/auth-req client-id client-secret)
                (assoc-default)
                (assoc :form-params user))]
    (-> (client/post (str figo-api-end-point "auth/user") req)
        (:body)
        (json/read-str :key-fn keyword)
        (:recovery_password))))



(defn add-user
  "Create a new figo Account "
  [^String client-id ^String client-secret & {:as user}]
  {:pre [(string? client-id)
         (string? client-secret)]}
  (add-user* client-id client-secret user))


(defn add-user-and-login
  "Creates a new figo User and returns a login token"
  [^String client-id ^String client-secret & {:keys [email password] :as user}]
  {:pre [(string? client-id)
         (string? client-secret)]}
  (do
    (add-user* client-id client-secret user)
    (credential-login client-id client-secret email password)))


(defn get-user
  "Get the current figo Account"
  [^String token]
  {:pre [(string? token)]}
  (let [header (-> (u/token-req token)
                   (assoc-default))
        url (str figo-api-end-point "rest/user")]
    (-> (client/get url header)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-user
  "Modify figo Account"
  [^String token & {:as user}]
  {:pre [(string? token)]}
  (let [req  (-> (u/token-req token)
                 (assoc-default)
                 (assoc :body (json/json-str user)))
        url (str figo-api-end-point "rest/user")]
    (-> (client/put url req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn remove-user
  "Delete figo Account"
  [^String token]
  {:pre [(string? token)]}
  (let [req (-> (u/token-req token)
                (assoc-default))
        url (str figo-api-end-point "rest/user")]
    (client/delete url req)
    nil))


(defn get-supported-service
  "Returns a list of all supported credit cards and payment services for a country"
  [^String token ^String c-code]
  {:pre [(string? token)
         (string? c-code)]}
  (let [req (-> (u/token-req token)
                (assoc-default))
        url (str figo-api-end-point "/rest/catalog/services/" c-code )]
    (-> (client/get url req)
        (:body)
        (json/read-str :key-fn keyword))))

(defn get-login-settings
  "Returns the login settings for a specified banking or payment service"
  [^String token ^String c-code ^String b-code]
  {:pre [(string? token)
         (string? c-code)
         (string? b-code)]}
  (let [req (-> (u/token-req token)
                (assoc-default))
        url (str figo-api-end-point "/rest/catalog/banks/" c-code "/" b-code )]
    (-> (client/get url req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn setup-new-account
  "Returns a TaskToken for a new account creation task"
  [^String token & {:as a-request}]
  {:pre [(string? token)]}
  (let [req (-> (u/token-req token)
                (assoc-default)
                (assoc :from-params a-request))
        url (str figo-api-end-point "/rest/accounts")]
    (-> (client/post url req)
        (:body)
        (json/read-str :key-fn keyword))))

;(setup-new-account 3 )

(defn setup-and-sync-account
  "Setups an account an starts the initial syncronization directly"
  [^String token & {:as a-request}]
  {:pre [(string? token)]}
  ; :todo Need to implement
  )




(defn get-account
  "All accounts the user has granted your App access to"
  [^String token & id]
  {:pre [(string? token)]}
  (let [params (-> (u/token-req token)
                   (assoc-default))
        url (if id
              (str figo-api-end-point "rest/accounts/" id)
              (str figo-api-end-point "rest/accounts"))]
    (-> (client/get url params)
        (:body)
        (json/read-str :key-fn keyword))))



(defn update-account
  "Modify an account"
  [^String token & {:keys [account_id] :as params}]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default)
              (assoc :form-params params))
        url (str figo-api-end-point "rest/accounts/" account_id)]
    (->
      (client/put url p)
      (:body)
      (json/read-str :key-fn keyword))))


(defn remove-account
  "Remove an account"
  [^String token id]
  {:pre [(string? token)
         (string? id)]}
  (let [header (-> (u/token-req token)
                   (assoc-default))
        url (str figo-api-end-point "rest/accounts/" id)]
    (client/delete url  header )
    nil))



(defn get-account-balance
  "Returns the balance details of the account with he specified ID"
  [^String token id]
  {:pre [(string? token)]}
  (let [params (-> (u/token-req token)
                   (assoc-default))
        url (str figo-api-end-point "rest/accounts/"  id "/balance")]
    (-> (client/get url params)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-account-balance
  "Modify balance or account limits"
  [^String token & {:keys [account_id] :as params}]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default)
              (assoc :form-params params))
        url (str figo-api-end-point "rest/accounts/" account_id "/balance")]
    (->
      (client/put url p)
      (:body)
      (json/read-str :key-fn keyword))))


(defn set-account-orders
  "Set new bank account sorting order"
  [^String token & a-coll]
  {:pre [(string? token)]}
  (let [v-coll (mapv (fn [v] {:account_id v}) a-coll)
        a-coll (json/json-str {:accounts v-coll})
        p (-> (u/token-req token)
              (assoc-default)
              (assoc :body a-coll))
        url (str figo-api-end-point "rest/accounts")]
    (client/put url p)
    nil))


(defn get-transactions
  "All transactions on all account of the user"
  [^String token & {:keys [account_id] :as s-t-map}]
  {:pre [(string? token)]}
  (let [url (if account_id
              (str figo-api-end-point "rest/accounts/"  account_id  "/transactions?")
              (str figo-api-end-point "rest/transactions?" ))
        p (-> (u/token-req token)
              (assoc-default)
              (assoc :query-params (or s-t-map {})))]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))



(defn modify-transaction
  "Modifies the visited field of a specific transaction"
  [^String token visited & {:keys [account_id transaction_id]}]
  {:pre [(string? token)]}
  (let [header (-> (u/token-req token)
                   (assoc-default))
        url (if account_id
              (if transaction_id
                (str figo-api-end-point "rest/accounts/" account_id  "/transactions/" transaction_id)
                (str figo-api-end-point "rest/accounts/" account_id "/transactions"))
              (str figo-api-end-point "rest/transactions" ))
        p (if visited
            (assoc header :form-params {:visited visited})
            header)]
    (client/put url p)
    nil))


(defn remove-transaction
  "Removes a Transaction"
  [^String token account_id transaction_id]
  {:pre [(string? token)]}
  (let [header (-> (u/token-req token)
                   (assoc-default))
        url   (str figo-api-end-point "rest/accounts/"  account_id  "/transactions/" transaction_id)]
    (client/delete url header)
    nil))



;;;;;;;;;;;;;;;;;, Security API



(defn get-security
  "Retrieves a specific security"
  [^String token account_id security_id & cents]
  {:pre [(string? token)]}
  (let [header (-> (u/token-req token)
                   (assoc-default))
        url (str figo-api-end-point "rest/accounts/"  account_id  "/securities/" security_id)
        p (if cents
            (assoc header :query-params {:cents cents})
            header)]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn get-securities
  "Retrieves all securities of the current user"
  [^String token & {:keys [account_id] :as s-t-map}]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default)
              (assoc :query-params (or s-t-map {})))
        url (if account_id
              (str figo-api-end-point "rest/accounts/"  account_id  "/securities")
              (str figo-api-end-point "rest/securities" ))]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn modify-securities
  "Modifies the visited field of a specific security"
  [^String token visited & {:keys [account_id]}]
  {:pre [(string? token)]}
  (let [req (-> (u/token-req token)
                (assoc-default)
                (assoc :form-params {:visited visited}))
        url (if account_id
              (str figo-api-end-point "rest/accounts/"  account_id  "/securities/")
              (str figo-api-end-point "rest/securities"))]
    (client/put url req)
    nil))



;;;;;;;;;;;;;;; Bank


(defn get-bank
  "Get bank"
  [^String token ^String bank-id]
  {:pre [(string? token)
         (string? bank-id)]}
  (let [req   (-> (u/token-req token)
                  (assoc-default))
        url (str figo-api-end-point "rest/bank/" bank-id)]
    (-> (client/get url req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-bank
  "Modify a bank"
  [^String token & {:keys [bank_id] :as s-t-map}]
  {:pre [(string? token)]}
  (let [req (-> (u/token-req token)
                (assoc-default)
                (assoc :form-params (or s-t-map {})))
        url (str figo-api-end-point "rest/banks/" bank_id)]
    (-> (client/put url req)
        (:body)
        (json/read-str :key-fn keyword))))


(defn remove-bank-pin
  "Remove the stored PIN for a bank (if there was one)"
  [^String token bank-id]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default))
        url (str figo-api-end-point "rest/banks/" bank-id "/remove_pin")]
    (client/post url p)
    nil))


(defn get-notification
  "All notifications registered by this client for the user"
  [^String token notification-id]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default))
        url (str figo-api-end-point "rest/notifications/" notification-id )]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn add-notification
  "Register a new notification on the server for the user"
  [^String token & { :as s-t-map}]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default)
              (assoc :form-params (or s-t-map {})))
        url (str figo-api-end-point "rest/notifications" )]
    (-> (client/post url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-notification
  "Update a stored notification"
  [^String token & {:keys [notification_id] :as noti} ]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default)
              (assoc :body noti))
        url (str figo-api-end-point "rest/notifications/" notification_id )]
    (-> (client/put url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn delete-notification
  "Update a stored notification"
  [^String token notification-id]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default))
        url (str figo-api-end-point "rest/notifications/" notification-id )]
    (client/delete url p)
    nil))

;;;;;;;;;;;;;;Payment


(defn get-payments
  "Retrieve all payments"
  [^String token & {:keys [account_id payment_id]}]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default))
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
  "add payment"
  [^String token account_id & {:as payment}]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default)
              (assoc :form-params (or payment {})))
        url (str figo-api-end-point "rest/accounts/" account_id "/payments")]
    (-> (client/post url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn add-container-payment
  "add payment"
  [^String token & {:keys [account_id] :as payment}]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default)
              (assoc :form-params (or payment {})))
        url (str figo-api-end-point "rest/accounts/" account_id "/payments")]
    (-> (client/post url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn get-payment-proposal
  "Returns a list of PaymentProposals"
  [^String token ]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default))
        url (str figo-api-end-point "/rest/adress_book" )]
    (-> (client/get url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn update-payment
  "Update a stored payment"
  [^String token & {:keys [account_id payment_id] :as payment}]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default)
              (assoc :form-params (or payment {})))
        url (str figo-api-end-point "rest/accounts/" account_id "/payments/" payment_id)]
    (-> (client/put url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn remove-payment
  "Remove a stored payment from the server"
  [^String token account_id payment_id]
  {:pre [(string? token)]}
  (let [p (-> (u/token-req token)
              (assoc-default))
        url (str figo-api-end-point "rest/accounts/" account_id "/payments/" payment_id)]
    (-> (client/delete url p))
    nil))


(defn submit-payment
  "Submit payment to bank server"
  [^String token account_id payment_id tanSchemeId state & redirect-uri]
  {:pre [(string? token)]}
  (let [p {:account_id    account_id
           :payment_id    payment_id
           :state         state
           :tan_scheme_id tanSchemeId}
        p (if redirect-uri
            (assoc p :redirect_uri redirect-uri)
            p)
        p (-> (u/token-req token)
              (assoc-default)
              (assoc :form-params p))
        url (str figo-api-end-point "rest/accounts/" account_id "/payments/" payment_id)

        resp (-> (client/post url p)
            (:body)
            (json/read-str :key-fn keyword))
        task-token (:task_token resp)]
    (str figo-api-end-point "/task/start?id=" task-token)))


(defn get-sync-url
  "URL to trigger a synchronisation. The user should open this URL in a web browser to synchronize his/her accounts with the respective bank servers. When
   the process is finished, the user is redirected to the provided URL."
  [^String token state redirect-uri]
  {:pre [(string? token)]}
  (let [p {:state         state
           :redirect_uri redirect-uri}
        h (->(u/token-req token)
             (assoc-default)
             (assoc :form-params p))
        url (str figo-api-end-point "rest/sync")
        resp (-> (client/post url h)
                 (:body)
                 (json/read-str :key-fn keyword))
        task-token (:task_token resp)]
    (str figo-api-end-point "/task/start?id=" task-token)))



(defn get-task-state
  "Get the current status of a Task"
  [^String token  & {:keys [id] :as token-resp}]
  {:pre [(string? token)]}
  (let [p (->(u/token-req token)
             (assoc-default)
             (assoc :form-params (or token-resp {})))
        url (str figo-api-end-point "task/progress?id=" id)]
    (-> (client/post url p)
        (:body)
        (json/read-str :key-fn keyword))))


(defn submit-response-to-task
  "Response to a running Task."
  [^String token id response type]
  {:pre [(string? token)]}
  (let [header (-> (u/token-req token)
                   (assoc-default))
        req {:id id}
        req (condp = type
              :pin (assoc req :pin response)
              :save-pin (assoc req :save_pin response)
              :continue (assoc req :continue response)
              :challenge (assoc req :response response)
              req)
        req (-> header
                (assoc :form-params req))
        url (str figo-api-end-point "task/progress?id=" id)]
    (-> (client/post url req)
        (:body)
        (json/read-str :key-fn keyword))))



(defn start-task
  "Start communication with bank server."
  [^String token id]
  {:pre [(string? token)]}
  (let [header (-> (u/token-req token)
                   (assoc-default))
        url (str figo-api-end-point "task/start?id=" id)]
    (-> (client/get url header))
    nil))


(defn cancel-task
  "Cancels a given task if possible"
  [^String token id]
  {:pre [(string? token)]}
  (let [header (-> (u/token-req token)
                   (assoc-default))
        url (str figo-api-end-point "task/cancel?id=" id)]
    (client/post url header)
    nil))


(defn start-process
  [^String token process-token ]
  {:pre [(string? token)]}
  (let [req (-> (u/token-req token)
                (assoc-default))
        url (str figo-api-end-point "process/start?id=" process-token)]
    (-> (client/post url req))
    nil))

(defn create-process
  ;:todo Documentation
  [^String token & {:as b-process}]
  {:pre [(string? token)]}
  (let [req (-> (u/token-req token)
                (assoc-default)
                (assoc :form-params b-process))
        url (str figo-api-end-point "client/process" )]
    (-> (client/post url req)
        (:body)
        (json/read-str :key-fn keyword))))