(ns me.figo.util
  (:require [base64-clj.core :as base64]
            [clj-http.client :as client]
            [clojure.data.json :as json])
  (:import [java.net URLEncoder]))


(defn as-url-encode
  "encoding string as url "
  [v]
  (URLEncoder/encode v "ISO-8859-1" ))


(defn assoc-debug
  "assoc debug with request "
  [req debug]
  (if-not debug
    req
    (-> req
        (assoc :debug true)
        (assoc :debug-body true))))

(defn assoc-timeout
  "assoc time out to request"
  [req timeout ]
  (-> req
      (assoc :socket-timeout timeout  )
      (assoc :conn-timeout timeout)))


(defn auth-req
  "Build http header with client-id and client-secret "
  [client-id client-secret]
  (let [basic (base64/encode (str client-id ":" client-secret))]
    {:headers {"Authorization: Basic " basic
               "username"              client-id
               "password"              client-secret}
     :body-encoding "UTF-8"
     :accept :json}))


(defn token-req
  "Build http header with authorization token "
  [token]
  {:headers  {"Authorization" (str "Bearer " token)
              "Accept"        "application/json"
              "Content-Type"  "application/json"}
   :body-encoding "UTF-8"
   :accept :json})




