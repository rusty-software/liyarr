(ns liyarr.views
  (:require
    [re-frame.core :as rf]
    [liyarr.config :as config]
    [liyarr.subs :as subs]

    [cljs.pprint :as pprint]))

(defn listen [query]
  @(rf/subscribe [query]))

(defn start-button []
  [:button
   {:class "btn btn-primary"
    :style {:margin-right "10px"}
    :on-click #(rf/dispatch [:start-game])}
   "Start Game"])

(defn header []
  [:div
   {:class "u-full-width"
    :style {:width "75%"
            :margin "0 auto"
            :text-align "center"}}
   [:h2 "Liyarr!"]
   [:p
    "Read the "
    [:a
     {:href "https://en.wikipedia.org/wiki/Liar%27s_dice"
      :target "_blank"}
     "wiki"]
    " for information on how to play."]
   (when-let [code (listen :game-code)]
     [:h4
      (str "Game code: " code)])
   (let [game-state (listen :game-state)]
     (when (or (= :over game-state)
               (= :not-started game-state)))
     [start-button])
   (if (listen :logged-in?)
     [:button
      {:class "btn btn-warning"
       :on-click #(rf/dispatch [:sign-out])}
      "Sign Out"]
     [:div
      [:button
       {:class "button-primary"
        :on-click #(rf/dispatch [:sign-in])}
       "Sign in"]
      [:br]
      [:span
       {:class "small"}
       "If you click Sign In and nothing happens, check your pop-up blocker!"]])
   [:hr]])

(defn not-signed-in [])

(defn no-game [])

(defn game []
  [:div "hi game"])

(defn main-panel []
  [:div
   [header]
   [:div
    (if (= :not-signed-in (listen :game-state))
      [not-signed-in]
      (let [view (listen :view)]
        (if (= :no-game view)
          [no-game]
          [game])))]
   #_(when config/debug?
     [:div
      [:hr]
      [:pre
       (with-out-str (pprint/pprint @(rf/subscribe [:db])))]])
   ]
  )
