(ns liyarr.game)

(defn roll
  "Given a number of dice to roll, returns a vector of randomly rolled dice."
  [qty]
  (sort
    (into []
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

(defn challenge
  "Given a collection of players, the current bid, and the index of the challenger, returns an updated collection
  of players where the challenge loser's dice collection has been decremented."
  [players current-bid current-player-idx]
  (let [dice-hands (map :dice players)
        rank-frequencies (frequencies (flatten dice-hands))
        rank-quantity-total (get rank-frequencies (:rank current-bid) 0)
        succeeded? (not (>= rank-quantity-total (:quantity current-bid)))
        idx-to-penalize (if succeeded?
                          (previous-player-idx players current-player-idx)
                          current-player-idx)
        player-to-penalize (get players idx-to-penalize)
        penalized-player (update player-to-penalize :dice (comp vec rest))
        players (assoc players idx-to-penalize penalized-player)]
    {:succeeded? succeeded?
     :penalized-player-idx idx-to-penalize
     :rank-quantity-total rank-quantity-total
     :players players}))

(defn game-over?
  "Given a collection of players, returns true if only one player has dice in their collection; otherwise, false."
  [players]
  (let [dice-counts (map count (map :dice players))]
    (= 1 (count (filter #(< 0 %) dice-counts)))))

(defn initialize-player
  "Given a raw player, returns a map representing the initial player state."
  [{:keys [name photo-url display-name]}]
  {:name name
   :dice (roll 5)
   :photo-url photo-url
   :display-name display-name})

(defn initialize-round
  "Given a game state, updates the game state with a new round.

  * All players' dice are shuffled
  * Current player index is incremented in the case of a bid
  * Current player index is set to the loser's index in the case of a challenge"
  [{:keys [players current-player-idx penalized-player-idx] :as game-state}]
  (let [next-idx (if penalized-player-idx
                   (next-player-idx players (dec penalized-player-idx))
                   (next-player-idx players current-player-idx))
        updated-players (for [player players]
                          (update player :dice (comp roll count)))]
    (-> game-state
        (assoc :current-player-idx next-idx
               :players updated-players)
        (dissoc :penalized-player-idx))))

(defn initialize-game
  "Given a map containing player info, initializes a game state using that info."
  [{:keys [players]}]
  {:players (into []
                  (for [player players]
                    (-> (initialize-player player))))
   :current-bid {:quantity 0 :rank 1}
   :current-player-idx 0
   :game-over? false})

(defn new-bid
  "Given a game-state, quantity, and rank, returns a new game state with either the updated bid or error message."
  [{:keys [current-bid current-player-idx players] :as game-state} quantity rank]
  (let [new-bid {:quantity quantity :rank rank}]
    (if (larger-bid? current-bid new-bid)
      (-> game-state
          (dissoc :msg)
          (assoc :action-result :new-bid
                 :current-bid new-bid
                 :current-player-idx (next-player-idx players current-player-idx)))
      (assoc game-state :action-result :failure
                        :msg "Yer new bid must be bigger'n the current one!"
                        :current-bid current-bid))))

(defn challenge-bid
  "Given a game-state, returns a new game state with the challenge result and messaging."
  [{:keys [players current-bid current-player-idx] :as game-state}]
  (let [challenge-result (challenge players current-bid current-player-idx)
        action-msg (if (:succeeded? challenge-result)
                     {:action-result :challenge
                      :msg "Well played, ye salty sea dog!"}
                     {:action-result :failure
                      :msg "'Twas a foolish challenge! Cost ye a dice!"})]
    (merge game-state
           challenge-result
           action-msg)))
