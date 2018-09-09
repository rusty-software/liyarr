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
  (is (game/larger-bid? {:quantity 2 :rank 3}
                        {:quantity 2 :rank 4}))
  (is (game/larger-bid? {:quantity 2 :rank 3}
                        {:quantity 3 :rank 2}))
  (is (not (game/larger-bid? {:quantity 3 :rank 3}
                             {:quantity 3 :rank 2})))
  (is (not (game/larger-bid? {:quantity 3 :rank 1}
                             {:quantity 2 :rank 6}))))

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
  (let [valid-player {:liyarr.game/name "Player Name" :liyarr.game/dice [1 2 3 4 5]}
        missing-name {:liyarr.game/dice [1 2 3 4 5]}
        missing-dice {:liyarr.game/name "Player Name"}
        too-many-dice {:liyarr.game/name "Player Name" :liyarr.game/dice [1 2 3 4 5 6]}]
    (is (s/valid? ::game/player valid-player) (s/explain ::game/player valid-player))
    (is (not (s/valid? ::game/player missing-name)) "Name should be required.")
    (is (not (s/valid? ::game/player missing-dice)) "Dice should be required.")
    (is (not (s/valid? ::game/player too-many-dice)) "Should not allow more than 5 dice.")))

(deftest test-initialize-player
  (let [player (game/initialize-player "foo")]
    (is (s/valid? ::game/player player) (s/explain ::game/player player))))

