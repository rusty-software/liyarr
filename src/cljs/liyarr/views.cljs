(ns liyarr.views
  (:require
    [clojure.string :as str]
    [cljs.pprint :as pprint]
    [re-frame.core :as rf]
    [liyarr.config :as config]
    [liyarr.subs]))

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
   (let [code (listen :game-code)]
     [:div
      (when code
        [:h4
         (str "Game code: " code)])
      (let [game-state (listen :game-state)]
        (when (or (= :over game-state)
                  (and code (= :not-started game-state)))
          [start-button]))])
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

(defn msg-display [msg action-result]
  (let [type (if (= "failure" action-result)
               "danger"
               "success")]
    [:div
     {:class (str "row alert alert-" type)}
     msg]))

(defn current-bid-display [{:keys [quantity rank] :as current-bid}]
  (when current-bid
    [:div
     {:class "row"}
     [:div
      {:class "three columns"}
      [:img {:src "../img/pirate_flag.jpeg"
             :height "75px"
             :width "75px"
             :style {:vertical-align "middle"
                     :margin "5px 5px"}}]]
     [:div
      {:class (str "six columns")}
      [:span
       {:style {:font-weight "300"
                :font-size "2.4rem"
                :line-height "1.5"
                :letter-spacing "-.05rem"
                :vertical-align "middle"}}
       (str "Current Bid: " quantity " X ")]
      [:img {:src (str "../img/dice-" rank ".png")
             :height "75px"
             :width "75px"
             :style {:vertical-align "middle"
                     :margin "5px 5px"}}]]
     [:div
      {:class "three columns"}
      [:img {:src "../img/dice_and_cup.jpeg"
             :height "75px"
             :width "125px"
             :style {:vertical-align "middle"
                     :margin "5px 5px"}}]]]))

(defn challenge-result-display [current-player action-result {:keys [rank]}]
  (let [rank-quantity-total (listen :rank-quantity-total)
        [result-desc class] (if (= "failure" action-result)
                              ["failed!" "danger"]
                              ["succeeded!" "success"])
        display (str (:display-name current-player) "'s challenge " result-desc
                     " There be " rank-quantity-total " o' the " rank "'s.")]
    [:div
     {:class (str "row alert alert-" class)}
     display]))

(defn action-inputs [current-bid challenged?]
  (if challenged?
    [:div
     [:button
      {:class "button-primary"
       :on-click #(rf/dispatch [:initialize-round])}
      "Next Round"]]

    [:div
     [:div
      [:div
       {:class "row"}
       [:label
        {:for "quantityInput"}
        "Quantity"]
       [:input
        {:id "quantityInput"}]
       [:label
        {:for "rankInput"}
        "Rank"]
       [:input
        {:id "rankInput"}]]
      [:button
       {:on-click #(rf/dispatch [:new-bid
                                 (.-value (.getElementById js/document "quantityInput"))
                                 (.-value (.getElementById js/document "rankInput"))])}
       "New Bid"]]
     (when current-bid
       [:div
        [:div
         [:strong "... OR ..."]]
        [:div
         [:button
          {:class "button-primary"
           :on-click #(rf/dispatch [:challenge-bid])}
          "CHALLENGE!"]]])]))

(defn current-player-display [{:keys [photo-url display-name dice]}
                              my-turn?
                              msg
                              action-result
                              current-bid
                              challenged?]
  [:div
   {:class "boxed"}
   (if my-turn?
     [:div
      (when msg
        (msg-display msg action-result))
      [:div
       {:class "row"}

       [:div
        {:class "four columns"}
        [:div
         {:class "row"}
         [:h5 "YER DICE!"]]
        (for [d dice]
          ^{:key (rand-int 1000000)}
          [:div
           {:class (str "row dice dice-" d)}
           ])]
       [:div
        {:class "eight columns"}
        [:div
         {:class "row"}
         [:h5 "YER ACTION?"]]
        (action-inputs current-bid challenged?)]]]
     [:div
      {:class "row"}
      [:img {:src photo-url :width "50px"}]
      [:h5 (str display-name "'s turn!")]])])

(defn player-class [player current-player-name]
  (cond
    (zero? (count (:dice player)))
    " alert alert-secondary"

    (= (:name player) current-player-name)
    " alert alert-warning"

    :else
    ""))

(defn player-list-display [user players current-player-idx action]
  [:div
   {:class "boxed"}
   [:div
    {:class "row"}
    [:h5 "YER PLAYERS"]]
   (for [player players
         :let [current-player-name (get-in players [current-player-idx :name])
               row-class (player-class player current-player-name)]]
     ^{:key (rand-int 1000000)}
     [:div
      {:class (str "row" row-class)}
      [:div
       {:class "two columns"}
       [:img {:src (:photo-url player) :width "50px"}]]
      [:div
       {:class "ten columns"}
       [:div
        {:class "row"}
        [:strong (:display-name player)]]
       [:div
        {:class "row"}
        [:em (:name player)]]
       (when (or (= (:email user) (:name player))
                 (= "challenge-bid" action))
         [:div
          {:class "row"}
          (for [d (:dice player)]
            ^{:key (rand-int 1000000)}
            [:div
             {:class (str "two columns tinydice dice-" d)}])])
       ]])])

(defn active-game []
  (let [user (listen :user)
        my-turn? (listen :my-turn?)
        players (listen :players)
        msg (listen :msg)
        action (listen :action)
        challenged? (= action "challenge-bid")
        action-result (listen :action-result)
        current-bid (listen :current-bid)
        current-player-idx (listen :current-player-idx)
        current-player (get players current-player-idx)]
    [:div
     (current-bid-display current-bid)
     (when (= "challenge-bid" action)
       (challenge-result-display current-player action-result current-bid))
     [:div
      {:class "row"}
      [:div
       {:class "seven columns"}
       (current-player-display current-player my-turn? msg action-result current-bid challenged?)]
      [:div
       {:class "five columns"}
       (player-list-display user players current-player-idx action)]]]))

(defn game-over []
  (let [players (listen :players)
        current-player-idx (listen :current-player-idx)
        player (get players current-player-idx)]
    [:div
     [:div
      {:class "row"}
      [:h3 "GAME OVER! Well played, me hearties! Yer winner be:"]]
     [:div
      {:class (str "row")}
      [:div
       {:class "two columns"}
       [:img {:src (:photo-url player)
              :width "100px"}]]
      [:div
       {:class "ten columns"}
       [:div
        {:class "row"}
        [:strong (:display-name player)]]
       [:div
        {:class "row"}
        [:em (:name player)]]
       [:div
        {:class "row"}
        (for [d (:dice player)]
          ^{:key (rand-int 1000000)}
          [:div
           {:class (str "two columns dice dice-" d)}])]]]]))

(defn game []
  (condp = (listen :game-state)
    :over [game-over]
    :playing [active-game]
    :not-started [pending-game]))

(defn main-panel []
  [:div
   {:class "u-full-width"
    :style {:width "75%"
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
      {:style {:text-align "left"}}
      [:hr]
      [:pre
       (with-out-str (pprint/pprint @(rf/subscribe [:db])))]])])
