(ns liyarr.game-test
  (:require [clojure.test :refer [deftest testing is]]
            [liyarr.game :as game]
            [clojure.spec.alpha :as s]))

(deftest test-player-spec
  (let [valid-player {::game/name "Player Name" ::game/dice [1 2 3 4 5]}
        missing-name {::game/dice [1 2 3 4 5]}
        missing-dice {::game/name "Player Name"}
        too-many-dice {::game/name "Player Name" ::game/dice [1 2 3 4 5 6]}]
    (is (s/valid? ::game/player valid-player) #_(s/explain ::game/player valid-player))
    (is (not (s/valid? ::game/player missing-name)) "Name should be required.")
    (is (not (s/valid? ::game/player missing-dice)) "Dice should be required.")
    (is (not (s/valid? ::game/player too-many-dice)) "Should not allow more than 5 dice.")))

(deftest test-bid-spec
  (let [valid-bid {::game/quantity 4 ::game/rank 2}
        missing-quantity {::game/rank 2}
        missing-rank {::game/quantity 4}
        rank-too-high {::game/quantity 4 ::game/rank 7}
        rank-too-low {::game/quantity 4 ::game/rank 0}
        quantity-not-int {::game/quantity "4" ::game/rank 2}]
    (is (s/valid? ::game/bid valid-bid) #_(s/explain ::game/bid valid-bid))
    (is (not (s/valid? ::game/bid missing-quantity)) "Quantity should be required.")
    (is (not (s/valid? ::game/bid missing-rank)) "Rank should be required.")
    (is (not (s/valid? ::game/bid rank-too-high)) "Rank must be less than 7.")
    (is (not (s/valid? ::game/bid rank-too-low)) "Rank must be greater than 0.")
    (is (not (s/valid? ::game/bid quantity-not-int)) "Quantity must be an integer.")))

(deftest test-players-spec
  (let [valid-players [{::game/name "Player 1" ::game/dice [1 2 3 4 5]}
                       {::game/name "Player 2" ::game/dice [1 2 3 4]}]
        invalid-players [{::game/name "Player 1" ::game/dice [1 2 3 4 5]}
                         {::game/name "Player 2" ::game/dices [1 2 3 4]}]]
    (is (s/valid? ::game/players valid-players))
    (is (not (s/valid? ::game/players invalid-players)))))

(deftest test-initialize-player
  (let [player (game/initialize-player {:name "foo" :photo-url "bar" :display-name "baz"})]
    (is (s/valid? ::game/player player) #_(s/explain ::game/player player))
    (is (= 5 (count (::game/dice player))))))

(deftest test-roll
  (let [legit-value? (fn [dice] (every? #(and (< 0 %) (> 7 %)) dice))
        five-dice (game/roll 5)
        three-dice (game/roll 3)]
    (is (= 5 (count five-dice)))
    (is (legit-value? five-dice))
    (is (= 3 (count three-dice)))
    (is (legit-value? three-dice))))

(deftest test-larger-bid?
  (is (game/larger-bid? {::game/quantity 2 ::game/rank 3}
                        {::game/quantity 2 ::game/rank 4}))
  (is (game/larger-bid? {::game/quantity 2 ::game/rank 3}
                        {::game/quantity 3 ::game/rank 2}))
  (is (not (game/larger-bid? {::game/quantity 3 ::game/rank 3}
                             {::game/quantity 3 ::game/rank 2})))
  (is (not (game/larger-bid? {::game/quantity 3 ::game/rank 1}
                             {::game/quantity 2 ::game/rank 6}))))

(deftest test-bid-satisfied?
  (is (game/bid-satisfied? {::game/quantity 2 ::game/rank 5}
                           [[1 2 3 4 5]
                            [2 3 4 5 6]]))
  (is (game/bid-satisfied? {::game/quantity 2 ::game/rank 5}
                           [[1 2 3 5 5]
                            [2 3 4 4 6]]))
  (is (not (game/bid-satisfied? {::game/quantity 2 ::game/rank 6}
                                [[1 2 3 4 5]
                                 [2 3 4 5 6]]))))

(deftest test-ordered-indexes
  (is (= [2 0 1] (game/ordered-indexes 1 3))))

(deftest test-previous-player-idx-all-active-players
  (let [players [{::game/name "Player 1" ::game/dice [1]}
                 {::game/name "Player 2" ::game/dice [2 3 4 5]}
                 {::game/name "Player 3" ::game/dice [1 2 3 4 5]}]]
    (is (= 1 (game/previous-player-idx players 2)))
    (is (= 2 (game/previous-player-idx players 0)))))

(deftest test-next-player-idx-all-active-players
  (let [players [{::game/name "Player 1" ::game/dice [1]}
                 {::game/name "Player 2" ::game/dice [2 3 4 5]}
                 {::game/name "Player 3" ::game/dice [1 2 3 4 5]}]]
    (is (= 2 (game/next-player-idx players 1)))
    (is (= 0 (game/next-player-idx players 2)))))

(deftest test-next-player-idx-inactive-player
  (let [players [{::game/name "Player 1" ::game/dice []}
                 {::game/name "Player 2" ::game/dice [2 3 4 5]}
                 {::game/name "Player 3" ::game/dice [1 2 3 4 5]}]]
    (is (= 1 (game/next-player-idx players 2)))))

(deftest test-challenge-success
  (let [players [{::game/name "Player 1" ::game/dice [1 1 1 1 1]}
                 {::game/name "Player 2" ::game/dice [2 2 2 2 2]}
                 {::game/name "Player 3" ::game/dice [3 3 3 3 3]}]
        current-bid {::game/quantity 1 ::game/rank 4}
        current-player-idx 1
        challenge-result (game/challenge players current-bid current-player-idx)]
    (is (s/valid? ::game/challenge-result challenge-result) #_(s/explain ::game/challenge-result challenge-result))
    (is (::game/succeeded? challenge-result))
    (is (= 0 (::game/penalized-player-idx challenge-result)))
    (is (= [{::game/name "Player 1" ::game/dice [1 1 1 1]}
            {::game/name "Player 2" ::game/dice [2 2 2 2 2]}
            {::game/name "Player 3" ::game/dice [3 3 3 3 3]}]
           (::game/players challenge-result)))))

(deftest test-challenge-failure
  (let [players [{::game/name "Player 1" ::game/dice [1 1 1 1 1]}
                 {::game/name "Player 2" ::game/dice [2 2 2 2 2]}
                 {::game/name "Player 3" ::game/dice [3 3 3 3 4]}]
        current-bid {::game/quantity 1 ::game/rank 4}
        current-player-idx 1
        challenge-result (game/challenge players current-bid current-player-idx)]
    (is (not (::game/succeeded? challenge-result)))
    (is (= 1 (::game/penalized-player-idx challenge-result)))
    (is (= [{::game/name "Player 1" ::game/dice [1 1 1 1 1]}
            {::game/name "Player 2" ::game/dice [2 2 2 2]}
            {::game/name "Player 3" ::game/dice [3 3 3 3 4]}]
           (::game/players challenge-result)))))

(deftest test-game-over?
  (let [players-go [{::game/name "Player 1" ::game/dice []}
                    {::game/name "Player 2" ::game/dice []}
                    {::game/name "Player 3" ::game/dice [4]}]
        players-ngo [{::game/name "Player 1" ::game/dice []}
                     {::game/name "Player 2" ::game/dice [2]}
                     {::game/name "Player 3" ::game/dice [4]}]]
    (is (game/game-over? players-go))
    (is (not (game/game-over? players-ngo)))))

(deftest test-initialize-round-bid
  (let [players [{::game/name "Player 1" ::game/dice [1 2 3 4 5]}
                 {::game/name "Player 2" ::game/dice [2 3 4 5]}
                 {::game/name "Player 3" ::game/dice [1 2 3 4 5]}]
        game-state {:players players
                    :current-player-idx 1}
        game-state-0 {:players players
                        :current-player-idx 2}
        new-state (game/initialize-round game-state)
        new-state-0 (game/initialize-round game-state-0)]
    (is (= 2 (:current-player-idx new-state)))
    (is (= 0 (:current-player-idx new-state-0)))
    ;; TODO: the following might have false failures; clean up when it matters
    (is (not= [1 2 3 4 5] (get-in new-state [:players 0 ::game/dice]) (get-in new-state [:players 2 ::game/dice])))
    (is (not= [2 3 4 5] (get-in new-state [:players 1 ::game/dice])))))

(deftest test-initialize-round-challenge
  (let [players [{::game/name "Player 1" ::game/dice [1 2 3 4 5]}
                 {::game/name "Player 2" ::game/dice [2 3 4 5]}
                 {::game/name "Player 3" ::game/dice [1 2 3 4 5]}]
        game-state {:players players
                    :current-player-idx 1
                    :penalized-player-idx 0}
        new-state (game/initialize-round game-state)]
    (is (= 0 (:current-player-idx new-state)))
    (is (nil? (:penalized-player-idx new-state)))))

(deftest test-initialize-round-loser-eliminated
  (let [players [{::game/name "Player 1" ::game/dice []}
                 {::game/name "Player 2" ::game/dice [2 3 4 5]}
                 {::game/name "Player 3" ::game/dice [1 2 3 4 5]}]
        game-state {:players players
                    :current-player-idx 1
                    :penalized-player-idx 0}
        new-state (game/initialize-round game-state)]
    (is (= 1 (:current-player-idx new-state)))
    (is (nil? (:penalized-player-idx new-state)))))

(deftest test-initialize-game
  (let [raw-players {:players [{:name "Player 1"} {:name "Player 2"} {:name "Player 3"}]}
        game-state (game/initialize-game raw-players)]
    (is (= 3 (count (:players game-state))))
    (is (s/valid? ::game/player (get-in game-state [:players 0])))
    (is (s/valid? ::game/player (get-in game-state [:players 1])))
    (is (s/valid? ::game/player (get-in game-state [:players 2])))
    (is (= "Player 1" (get-in game-state [:players 0 ::game/name])))
    (is (= "Player 3" (get-in game-state [:players 2 ::game/name])))
    (is (= 0 (:current-player-idx game-state)))
    (is (not (nil? (:game-over? game-state))))
    (is (not (:game-over? game-state)))))
