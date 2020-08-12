(ns liyarr.game)

(defn str->int [s]
  #?(:clj (java.lang.Integer/parseInt s)
     :cljs (js/parseInt s)))

(defn roll
  "Given a number of dice to roll, returns a vector of randomly rolled dice."
  [qty]
  (vec
    (sort
      (for [_ (range qty)]
        (inc (rand-int 6))))))

(defn larger-bid?
  "Given a first bid, returns true if the second bid is larger; otherwise, false."
  [first-bid second-bid]
  (let [{first-quantity :quantity first-rank :rank} first-bid
        {second-quantity :quantity second-rank :rank} second-bid]
    (or (> second-quantity first-quantity)
        (and (= second-quantity first-quantity)
             (> second-rank first-rank)))))

(defn ordered-indexes
  "Given a current index and a count of players, returns the next indexes of all future players."
  [current-player-idx players-count]
  (vec (concat (range (inc current-player-idx) players-count) (range 0 (inc current-player-idx)))))

(defn first-idx-with-dice
  "Given a collection of players and an ordered index collection, returns the index of the first player with dice."
  [players idxs]
  (loop [idx (first idxs)
         idxs idxs]
    (if (< 0 (count (get-in players [idx :dice])))
      idx
      (recur (first idxs) (rest idxs)))))

(defn previous-player-idx
  "Given a current player idx, returns the previous player's idx."
  [players current-player-idx]
  (let [idxs (concat (reverse (butlast (ordered-indexes current-player-idx (count players)))) [current-player-idx])]
    (first-idx-with-dice players idxs)))

(defn next-player-idx
  "Give a collection of players and current player idx, returns the next active player's idx."
  [players current-player-idx]
  (let [idxs (ordered-indexes current-player-idx (count players))]
    (first-idx-with-dice players idxs)))

(defn exact
  [{:keys [players current-bid current-player-idx]}]
  (let [dice-hands (map :dice players)
        rank-frequencies (frequencies (flatten dice-hands))
        rank-quantity-total (get rank-frequencies (:rank current-bid) 0)
        succeeded? (= rank-quantity-total (:quantity current-bid))
        reward-or-penalize (if succeeded? :rewarded-player-idx :penalized-player-idx)]
    {:succeeded? succeeded?
     reward-or-penalize current-player-idx
     :rank-quantity-total rank-quantity-total}))

(defn challenge
  "Given a collection of players, the current bid, and the index of the challenger, returns a map indicating the
  success/failure of the attempt, which player idx should be penalized, and the total of bid rank."
  [players current-bid current-player-idx]
  (let [dice-hands (map :dice players)
        rank-frequencies (frequencies (flatten dice-hands))
        rank-quantity-total (get rank-frequencies (:rank current-bid) 0)
        succeeded? (not (>= rank-quantity-total (:quantity current-bid)))
        idx-to-penalize (if succeeded?
                          (previous-player-idx players current-player-idx)
                          current-player-idx)]
    {:succeeded? succeeded?
     :penalized-player-idx idx-to-penalize
     :rank-quantity-total rank-quantity-total}))

(defn game-over?
  "Given a collection of players, returns true if only one player has dice in their collection; otherwise, false."
  [players]
  (let [dice-counts (map count (map :dice players))]
    (= 1 (count (filter #(< 0 %) dice-counts)))))

(defn initialize-player
  "Given a raw player and a number of dice, returns a map representing the initial player state."
  [{:keys [name photo-url display-name]} dice-count]
  {:name name
   :dice (roll dice-count)
   :photo-url photo-url
   :display-name display-name})

(defn unactioned-state
  "Given a game-state, returns a new state with action-related keys removed."
  [game-state]
  (dissoc game-state :action :action-result :msg))

(defn with-penalized-player
  "Given a collection of players and potential penalization index, returns the players collection with the penalty
  applied."
  [{:keys [players penalized-player-idx]}]
  (if penalized-player-idx
    (let [player-to-penalize (get players penalized-player-idx)
          penalized-player (update player-to-penalize :dice (comp vec rest))]
      (assoc players penalized-player-idx penalized-player))
    players))

(defn with-rewarded-player
  "Given a collection of players and a potential penalization index,
  returns the players with the reward applied."
  [{:keys [rewarded-player-idx players starting-dice] :as game-state}]
  (if rewarded-player-idx
    (let [player-to-reward (get players rewarded-player-idx)
          update? (< (count (:dice player-to-reward))
                     starting-dice)
          new-player (if update?
                       (update player-to-reward :dice #(conj % 1))
                       player-to-reward)]
      (assoc-in game-state [:players rewarded-player-idx] new-player))
    game-state))

(defn with-updated-players
  [{:keys [players penalized-player-idx] :as game-state}]
  (-> game-state
      with-rewarded-player
      with-penalized-player))

(defn initialize-round
  "Given a game state, updates the game state with a new round.

  * Current bid is reset
  * All players' dice are shuffled
  * Current player index is incremented in the case of a bid
  * Current player index is set to the loser's index in the case of a challenge"
  [{:keys [players
           current-player-idx
           penalized-player-idx
           rewarded-player-idx]
    :as game-state}]
  (let [players (with-updated-players game-state)
        starting-idx (or penalized-player-idx rewarded-player-idx)
        next-idx (if starting-idx
                   (next-player-idx players (dec starting-idx))
                   (next-player-idx players current-player-idx))
        updated-players (vec
                          (for [player players]
                            (update player :dice (comp roll count))))]
    (if (game-over? players)
      (-> game-state
          (unactioned-state)
          (assoc :game-over? true
                 :current-player-idx next-idx
                 :players players)
          (dissoc :current-bid
                  :penalized-player-idx
                  :rewarded-player-idx))
      (-> game-state
          (unactioned-state)
          (assoc :current-player-idx next-idx
                 :players updated-players
                 :bidding? false)
          (dissoc :current-bid
                  :penalized-player-idx
                  :rewarded-player-idx)))))

(defn initialize-game
  "Given a map containing player info and a number of dice to use, initializes a game state using that info."
  [{:keys [players]} dice-count]
  {:players (vec
              (for [player players]
                (initialize-player player dice-count)))
   :current-player-idx 0
   :starting-dice dice-count
   :game-over? false})

(defn new-bid
  "Given a game-state, quantity, and rank, returns a new game state with either the updated bid or error message."
  [{:keys [current-bid current-player-idx players] :as game-state} quantity rank]
  (let [new-bid {:quantity (str->int quantity) :rank (str->int rank)}]
    (if (larger-bid? current-bid new-bid)
      (-> game-state
          (unactioned-state)
          (assoc :action :new-bid
                 :action-result :success
                 :bidding? false
                 :current-bid new-bid
                 :current-player-idx (next-player-idx players current-player-idx)))
      (-> game-state
          (unactioned-state)
          (assoc :action :new-bid
                 :action-result :failure
                 :msg "Yer new bid must be bigger'n the current one!"
                 :current-bid current-bid)))))

(defn exact-bid
  "Given a game-state, returns a new game state with the challenge result and messaging."
  [{:keys [players current-bid current-player-idx] :as game-state}]
  (let [exact-result (exact game-state)
        action-msg (if (:succeeded? exact-result)
                     {:action :exact-bid
                      :action-result :success
                      :msg "Ahoy, Me Hearties! You won yourself a dice!"}
                     {:action :exact-bid
                      :action-result :failure
                      :msg "You foolish landlubber! Ye lost a dice."})]
    (merge (unactioned-state game-state)
           exact-result
           action-msg)))

(defn challenge-bid
  "Given a game-state, returns a new game state with the challenge result and messaging."
  [{:keys [players current-bid current-player-idx] :as game-state}]
  (let [challenge-result (challenge players current-bid current-player-idx)
        action-msg (if (:succeeded? challenge-result)
                     {:action :challenge-bid
                      :action-result :success
                      :msg "Well played, ye salty sea dog!"}
                     {:action :challenge-bid
                      :action-result :failure
                      :msg "'Twas a foolish challenge! Cost ye a dice!"})]
    (merge (unactioned-state game-state)
           challenge-result
           action-msg)))

(defn boot-player
  "Given a game state and a player idx, re-initializes the current round without the player."
  [{:keys [players] :as game-state} player-idx]
  (let [updated-players (vec (concat (subvec players 0 player-idx) (subvec players (inc player-idx))))
        updated-state (-> game-state
                          (assoc :players updated-players)
                          (dissoc :penalized-player-idx))]
    (initialize-round updated-state)))
