(ns liyarr.subs
  (:require
   [re-frame.core :as rf])
  (:require-macros
    [reagent.ratom :refer [reaction]]))

(rf/reg-sub
  :db
  (fn [db]
    db))

(rf/reg-sub-raw
  :game
  (fn [_ _]
    (reaction
      (let [game-code @(rf/subscribe [:game-code])]
        (if game-code
          @(rf/subscribe [:firebase/on-value {:path [game-code]}])
          nil)))))

(rf/reg-sub
  :user
  (fn [db _] (:user db)))

(rf/reg-sub
  :game-code
  (fn [db]
    (:game-code db)))

(rf/reg-sub
 :game-state
 (fn [_ _]
   [(rf/subscribe [:game])
    (rf/subscribe [:user])])
 (fn [[game user] _]
   (if (nil? user)
     :not-signed-in
     (condp = (:game-over? game)
       true :over
       false :playing
       nil :not-started))))

(rf/reg-sub
 :logged-in?
 (fn [_ _]
   (rf/subscribe [:user]))
 (fn [user _]
   (boolean user)))


