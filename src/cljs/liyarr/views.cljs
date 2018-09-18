(ns liyarr.views
  (:require
    [clojure.string :as str]
    [re-frame.core :as rf]
    [liyarr.config :as config]
    [liyarr.subs :as subs]

    [cljs.pprint :as pprint]
    ))

(defn listen [query]
  @(rf/subscribe [query]))

(defn start-button []
  [:button
   {:class "button-primary"
    :style {:margin-right "10px"}
    :on-click #(rf/dispatch [:start-game])}
   "Start Game"])

(defn header []
  [:div
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
      {:on-click #(rf/dispatch [:sign-out])}
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

(defn not-signed-in []
  [:div
   {:class "text-center"}
   [:h3 "Welcome to Liyarr!"]
   [:p "Sign in with a Google account by clicking above to join the game."]])

(defn no-game []
  [:div
   {:class "text-center"}
   [:div
    {:class "row"}
    [:div
     {:class "six columns"}
     [:form
      {:on-submit (fn [e]
                    (.preventDefault e)
                    (rf/dispatch [:join-game (str/upper-case (.-value (.getElementById js/document "gameCodeInput")))]))}
      [:div
       [:label
        {:for "gameCodeInput"}
        "Enter a code to join a game:"]
       [:input
        {:class "text-uppercase form-control mx-1"
         :id "gameCodeInput"
         :type "text"
         :name "game"
         :placeholder "Game Code"
         :required true}]
       [:br]
       [:button
        {:class "button-primary"
         :type "submit"}
        "Join Game"]]]]
    [:div
     {:class "six columns"}
     [:label
      {:for "createGameBtn"}
      "Or, create a new game code:"]
     [:button
      {:id "createGameBtn"
       :on-click #(rf/dispatch [:create-game])} "Create new game"]]]])

(defn pending-game []
  (let [players (listen :players)]
    [:div
     [:div
      {:class "alert alert-primary"}
      "Players"]
     (doall
      (for [[idx player] (map-indexed vector players)]
        [:div
         {:key idx}
         (:name player)]))]))

(defn game []
  (condp = (listen :game-state)
    #_#_:over [game-over]
    #_#_:playing [active-game]
    :not-started [pending-game]))

(defn main-panel []
  [:div
   {:class "u-full-width"
    :style {:width "90%"
            :margin "0 auto"
            :text-align "center"}}
   [header]
   [:div
    (if (= :not-signed-in (listen :game-state))
      [not-signed-in]
      (let [view (listen :view)]
        (if (= :no-game view)
          [no-game]
          [game])))]
   (when config/debug?
     [:div
      [:hr]
      [:pre
       (with-out-str (pprint/pprint @(rf/subscribe [:db])))]])
   ]
  )
