(ns liyarr.events
  (:require
    [re-frame.core :as rf]
    [liyarr.config :as config]
    [liyarr.db :as db]))

(rf/reg-event-db
 :initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-fx
 :sign-in
 (fn [_ _] {:firebase/google-sign-in {:sign-in-method (if config/debug?
                                                        :popup
                                                        :redirect)}}))

(rf/reg-event-fx
 :sign-out
 (fn [_ _] {:firebase/sign-out nil}))

(rf/reg-event-db
 :set-user
 (fn [db [_ user]]
   (assoc db :user user)))

(rf/reg-event-db
 :firebase-error
 (fn [db [_ error]]
   (assoc db :firebase-error (pr-str error))))
