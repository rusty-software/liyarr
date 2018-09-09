(ns liyarr.game-test
  (:require [clojure.test :refer [deftest testing is]]
            [liyarr.game :as game]
            [clojure.spec.alpha :as s]))

(deftest test-roll
  (let [legit-value? (fn [dice] (every? #(and (< 0 %) (> 7 %)) dice))
        six-dice (game/roll 5)
        three-dice (game/roll 3)]
    (is (= 5 (count six-dice)))
    (is (legit-value? six-dice))
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
  (is (game/bid-satisfied? {:quantity 2 :rank 5}
                           [[1 2 3 4 5]
                            [2 3 4 5 6]]))
  (is (game/bid-satisfied? {:quantity 2 :rank 5}
                           [[1 2 3 5 5]
                            [2 3 4 4 6]]))
  (is (not (game/bid-satisfied? {:quantity 2 :rank 6}
                                [[1 2 3 4 5]
                                 [2 3 4 5 6]]))))

(deftest test-player-spec
  (let [valid-player {::game/name "Player Name" ::game/dice [1 2 3 4 5]}
        missing-name {::game/dice [1 2 3 4 5]}
        missing-dice {::game/name "Player Name"}
        too-many-dice {::game/name "Player Name" ::game/dice [1 2 3 4 5 6]}]
    (is (s/valid? ::game/player valid-player) (s/explain ::game/player valid-player))
    (is (not (s/valid? ::game/player missing-name)) "Name should be required.")
    (is (not (s/valid? ::game/player missing-dice)) "Dice should be required.")
    (is (not (s/valid? ::game/player too-many-dice)) "Should not allow more than 5 dice.")))

(deftest test-initialize-player
  (let [player (game/initialize-player "foo")]
    (is (s/valid? ::game/player player) (s/explain ::game/player player))))

(deftest test-bid-spec
  (let [valid-bid {::game/quantity 4 ::game/rank 2}
        missing-quantity {::game/rank 2}
        missing-rank {::game/quantity 4}
        rank-too-high {::game/quantity 4 ::game/rank 7}
        rank-too-low {::game/quantity 4 ::game/rank 0}
        quantity-not-int {::game/quantity "4" ::game/rank 2}]
    (is (s/valid? ::game/bid valid-bid) (s/explain ::game/bid valid-bid))
    (is (not (s/valid? ::game/bid missing-quantity)) "Quantity should be required.")
    (is (not (s/valid? ::game/bid missing-rank)) "Rank should be required.")
    (is (not (s/valid? ::game/bid rank-too-high)) "Rank must be less than 7.")
    (is (not (s/valid? ::game/bid rank-too-low)) "Rank must be greater than 0.")
    (is (not (s/valid? ::game/bid quantity-not-int)) "Quantity must be an integer.")))
