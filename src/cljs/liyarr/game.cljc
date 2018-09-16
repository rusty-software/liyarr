(ns liyarr.game
  (:require [clojure.spec.alpha :as s]))

(s/def ::quantity int?)
(s/def ::rank #{1 2 3 4 5 6})
(s/def ::bid (s/keys :req [::quantity ::rank]))

(s/def ::name string?)
(s/def ::dice #(and vector? (< (count %) 6)))
(s/def ::player (s/keys :req [::name ::dice]))

(s/def ::succeeded? boolean?)
(s/def ::penalized-player-idx int?)
(s/def ::players (s/coll-of ::player :kind vector?))
(s/def ::challenge-result (s/keys :req [::succeeded? ::penalized-player-idx ::players]))

(defn initialize-player
  "Given a player name, returns a map representing the initial player state."
  [name]
  {::name name
   ::dice [1 1 1 1 1]})

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
  (let [{first-quantity ::quantity first-rank ::rank} first-bid
        {second-quantity ::quantity second-rank ::rank} second-bid]
    (or (> second-quantity first-quantity)
        (and (= second-quantity first-quantity)
             (> second-rank first-rank)))))

(defn bid-satisfied?
  "Given a bid, returns true if the quantity and rank are covered by the collection of dice; otherwise, false."
  [{:keys [::quantity ::rank]} dice-hands]
  (let [rank-frequencies (frequencies (flatten dice-hands))
        rank-quantity (get rank-frequencies rank 0)]
    (>= rank-quantity quantity)))

(defn ordered-indexes
  "Given a current index and a count of players, returns the next indexes of all future players."
  [current-player-idx players-count]
  (vec (concat (range (inc current-player-idx) players-count) (range 0 (inc current-player-idx)))))

;; TODO: refactor out loop that's common to next and previous
(defn previous-player-idx
  "Given a current player idx, returns the previous player's idx."
  [players current-player-idx]
  (let [idxs (concat (reverse (butlast (ordered-indexes current-player-idx (count players)))) [current-player-idx])]
    (loop [idx (first idxs)
           idxs idxs]
      (if (< 0 (count (get-in players [idx ::dice])))
        idx
        (recur (first idxs) (rest idxs))))))

(defn next-player-idx
  "Give a collection of players and current player idx, returns the next active player's idx."
  [players current-player-idx]
  (let [idxs (ordered-indexes current-player-idx (count players))]
    (loop [idx (first idxs)
           idxs idxs]
      (if (< 0 (count (get-in players [idx ::dice])))
        idx
        (recur (first idxs) (rest idxs))))))

(defn challenge
  "Given a collection of players, the current bid, and the index of the challenger, returns an updated collection
  of players where the challenge loser's dice collection has been decremented."
  [players current-bid current-player-idx]
  (let [dice-hands (map ::dice players)
        succeeded? (not (bid-satisfied? current-bid dice-hands))
        idx-to-penalize (if succeeded?
                          (previous-player-idx players current-player-idx)
                          current-player-idx)
        player-to-penalize (get players idx-to-penalize)
        penalized-player (update player-to-penalize ::dice (comp vec rest))
        players (assoc players idx-to-penalize penalized-player)]
    {::succeeded? succeeded?
     ::penalized-player-idx idx-to-penalize
     ::players players}))

(defn game-over?
  "Given a collection of players, returns true if only one player has dice in their collection; otherwise, false."
  [players]
  (let [dice-counts (map count (map ::dice players))]
    (= 1 (count (filter #(< 0 %) dice-counts)))))

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
                          (update player ::dice (comp roll count)))]
    (-> game-state
        (assoc :current-player-idx next-idx
               :players updated-players)
        (dissoc :penalized-player-idx))))
