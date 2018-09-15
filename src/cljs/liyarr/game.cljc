(ns liyarr.game
  (:require [clojure.spec.alpha :as s]))

(s/def ::quantity int?)
(s/def ::rank #{1 2 3 4 5 6})
(s/def ::bid (s/keys :req [::quantity ::rank]))

(s/def ::name string?)
(s/def ::dice #(and vector? (< (count %) 6)))
(s/def ::player (s/keys :req [::name ::dice]))

(s/def ::challenge-result #{:success :failure})

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

(defn previous-player
  "Given a collection of players and a player index, returns the player at the previous position."
  [players idx]
  (if (zero? idx)
    (get players (dec (count players)))
    (get players (dec idx))))

(defn challenge
  "Given a collection of players, the current bid, and the index of the challenger, returns an updated collection
  of players where the challenge loser's dice collection has been decremented."
  [players current-bid current-player-idx]
  (let [dice-hands (map ::dice players)
        satisfied? (bid-satisfied? current-bid dice-hands)
        result (if satisfied?
                 :failure
                 :success)]
    {::challenge-result result}))

