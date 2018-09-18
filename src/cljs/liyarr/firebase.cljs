(ns liyarr.firebase
  (:require
    [com.degel.re-frame-firebase :as firebase]
    [re-frame.core :as rf]
    [iron.re-utils :as re-utils]
    [clojure.string :as str]))

(defn fb-ref [path]
  (.ref (js/firebase.database)
        (str/join "/" (clj->js path))))

(defn init []
  (firebase/init :firebase-app-info {:apiKey "AIzaSyAYLxAdesv9VvaZs4pJXuWEyhI-kavrdtg"
                                     :authDomain "liyarrs-dice.firebaseapp.com"
                                     :databaseURL "https://liyarrs-dice.firebaseio.com"
                                     :projectId "liyarrs-dice"
                                     :storageBucket "liyarrs-dice.appspot.com"
                                     :messagingSenderId "851773010925"}
                 :get-user-sub [:user]
                 :set-user-event [:set-user]
                 :default-error-handler [:firebase-error]))

(defn success-failure-wrapper [on-success on-failure]
  (let [on-success (and on-success (re-utils/event->fn on-success))
        on-failure (and on-failure (re-utils/event->fn on-failure))]
    (fn [err]
      (cond (nil? err)
            (when on-success (on-success))

            on-failure (on-failure err)

            :else      ;; [TODO] This should use default error handler
            (js/console.error "Firebase error:" err)))))

(defn- js->clj-tree [x]
  (-> x
      js->clj
      clojure.walk/keywordize-keys))

(defn firebase-transaction-effect [{:keys [path function on-success on-failure]}]
  (.transaction (fb-ref path)
                (fn [data] (-> data
                               js->clj-tree
                               function
                               clj->js))
                (success-failure-wrapper on-success on-failure)))

(rf/reg-fx :firebase/swap! firebase-transaction-effect)
