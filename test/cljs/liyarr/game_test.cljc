(ns liyarr.game-test
  (:require [clojure.test :refer [deftest testing is]]
            [liyarr.game :as game]))

(deftest test-initialize-player
  (let [player (game/initialize-player {:name "foo" :photo-url "bar" :display-name "baz"})]
    (is (= 5 (count (:dice player))))))

(deftest test-roll
  (let [legit-value? (fn [dice] (every? #(and (< 0 %) (> 7 %)) dice))
        five-dice (game/roll 5)
        three-dice (game/roll 3)]
    (is (= 5 (count five-dice)))
    (is (legit-value? five-dice))
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

(deftest test-ordered-indexes
  (is (= [2 0 1] (game/ordered-indexes 1 3))))

(deftest test-previous-player-idx-all-active-players
  (let [players [{:name "Player 1" :dice [1]}
                 {:name "Player 2" :dice [2 3 4 5]}
                 {:name "Player 3" :dice [1 2 3 4 5]}]]
    (is (= 1 (game/previous-player-idx players 2)))
    (is (= 2 (game/previous-player-idx players 0)))))

(deftest test-next-player-idx-all-active-players
  (let [players [{:name "Player 1" :dice [1]}
                 {:name "Player 2" :dice [2 3 4 5]}
                 {:name "Player 3" :dice [1 2 3 4 5]}]]
    (is (= 1 (game/next-player-idx players 0)))
    (is (= 2 (game/next-player-idx players 1)))
    (is (= 0 (game/next-player-idx players 2)))))

(deftest test-next-player-idx-inactive-player
  (let [players [{:name "Player 1" :dice []}
                 {:name "Player 2" :dice [2 3 4 5]}
                 {:name "Player 3" :dice [1 2 3 4 5]}]]
    (is (= 1 (game/next-player-idx players 2)))))

(deftest test-challenge-success
  (let [players [{:name "Player 1" :dice [1 1 1 1 1]}
                 {:name "Player 2" :dice [2 2 2 2 2]}
                 {:name "Player 3" :dice [3 3 3 3 3]}]
        current-bid {:quantity 1 :rank 4}
        current-player-idx 1
        challenge-result (game/challenge players current-bid current-player-idx)]
    (is (:succeeded? challenge-result))
    (is (= 0 (:penalized-player-idx challenge-result)))
    (is (= 0 (:rank-quantity-total challenge-result)))
    (is (= [{:name "Player 1" :dice [1 1 1 1]}
            {:name "Player 2" :dice [2 2 2 2 2]}
            {:name "Player 3" :dice [3 3 3 3 3]}]
           (:players challenge-result)))))

(deftest test-challenge-failure
  (let [players [{:name "Player 1" :dice [1 1 1 1 1]}
                 {:name "Player 2" :dice [2 2 2 2 2]}
                 {:name "Player 3" :dice [3 3 3 3 4]}]
        current-bid {:quantity 1 :rank 4}
        current-player-idx 1
        challenge-result (game/challenge players current-bid current-player-idx)]
    (is (not (:succeeded? challenge-result)))
    (is (= 1 (:penalized-player-idx challenge-result)))
    (is (= [{:name "Player 1" :dice [1 1 1 1 1]}
            {:name "Player 2" :dice [2 2 2 2]}
            {:name "Player 3" :dice [3 3 3 3 4]}]
           (:players challenge-result)))))

(deftest test-game-over?
  (let [players-go [{:name "Player 1" :dice []}
                    {:name "Player 2" :dice []}
                    {:name "Player 3" :dice [4]}]
        players-ngo [{:name "Player 1" :dice []}
                     {:name "Player 2" :dice [2]}
                     {:name "Player 3" :dice [4]}]]
    (is (game/game-over? players-go))
    (is (not (game/game-over? players-ngo)))))

(deftest test-initialize-round-bid
  (let [players [{:name "Player 1" :dice [1 2 3 4 5]}
                 {:name "Player 2" :dice [2 3 4 5]}
                 {:name "Player 3" :dice [1 2 3 4 5]}]
        game-state {:players players
                    :current-player-idx 1}
        game-state-0 {:players players
                        :current-player-idx 2}
        new-state (game/initialize-round game-state)
        new-state-0 (game/initialize-round game-state-0)]
    (is (= 2 (:current-player-idx new-state)))
    (is (= 0 (:current-player-idx new-state-0)))
    ;; TODO: the following might have false failures; clean up when it matters
    (is (not= [1 2 3 4 5] (get-in new-state [:players 0 :dice]) (get-in new-state [:players 2 :dice])))
    (is (not= [2 3 4 5] (get-in new-state [:players 1 :dice])))))

(deftest test-initialize-round-challenge
  (let [players [{:name "Player 1" :dice [1 2 3 4 5]}
                 {:name "Player 2" :dice [2 3 4 5]}
                 {:name "Player 3" :dice [1 2 3 4 5]}]
        game-state {:players players
                    :current-player-idx 1
                    :penalized-player-idx 0}
        new-state (game/initialize-round game-state)]
    (is (= 0 (:current-player-idx new-state)))
    (is (nil? (:penalized-player-idx new-state)))))

(deftest test-initialize-round-loser-eliminated
  (let [players [{:name "Player 1" :dice []}
                 {:name "Player 2" :dice [2 3 4 5]}
                 {:name "Player 3" :dice [1 2 3 4 5]}]
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
    (is (= "Player 1" (get-in game-state [:players 0 :name])))
    (is (= "Player 3" (get-in game-state [:players 2 :name])))
    (is (= 0 (:current-player-idx game-state)))
    (is (not (nil? (:game-over? game-state))))
    (is (not (:game-over? game-state)))))

(deftest test-new-bid-success
  (let [players [{:name "Player 1" :dice [1 2 3 4 5]}
                 {:name "Player 2" :dice [2 3 4 5]}
                 {:name "Player 3" :dice [1 2 3 4 5]}]
        game-state {:players players
                    :current-player-idx 1
                    :current-bid {:quantity 0 :rank 1}
                    :penalized-player-idx 0}
        new-state (game/new-bid game-state 6 4)]
    (is (= :new-bid (:action-result new-state)))
    (is (= {:quantity 6 :rank 4} (:current-bid new-state)))
    (is (= 2 (:current-player-idx new-state)))))

(deftest test-new-bid-failure
  (let [players [{:name "Player 1" :dice [1 2 3 4 5]}
                 {:name "Player 2" :dice [2 3 4 5]}
                 {:name "Player 3" :dice [1 2 3 4 5]}]
        game-state {:players players
                    :current-player-idx 1
                    :current-bid {:quantity 5 :rank 4}
                    :penalized-player-idx 0}
        new-state (game/new-bid game-state 5 3)]
    (is (= :failure (:action-result new-state)))
    (is (= "Yer new bid must be bigger'n the current one!" (:msg new-state)))
    (is (= {:quantity 5 :rank 4} (:current-bid new-state)))
    (is (= 1 (:current-player-idx new-state)))))

(deftest test-challenge-bid-success
  (let [players [{:name "Player 1" :dice [1 1 1 1 1]}
                 {:name "Player 2" :dice [2 2 2 2 2]}
                 {:name "Player 3" :dice [3 3 3 3 3]}]
        game-state {:players players
                    :current-bid {:quantity 1 :rank 4}
                    :current-player-idx 1}
        {:keys [succeeded? penalized-player-idx players action-result]} (game/challenge-bid game-state)]
    (is succeeded?)
    (is (= :challenge action-result))
    (is (= 0 penalized-player-idx))
    (is (= [{:name "Player 1" :dice [1 1 1 1]}
            {:name "Player 2" :dice [2 2 2 2 2]}
            {:name "Player 3" :dice [3 3 3 3 3]}]
           players))))

(deftest test-challenge-bid-failure
  (let [players [{:name "Player 1" :dice [1 1 1 1 1]}
                 {:name "Player 2" :dice [2 2 2 2 2]}
                 {:name "Player 3" :dice [3 3 3 3 4]}]
        game-state {:players players
                    :current-bid {:quantity 1 :rank 4}
                    :current-player-idx 1}
        {:keys [succeeded? penalized-player-idx players action-result]} (game/challenge-bid game-state)]
    (is (not succeeded?))
    (is (= :failure action-result))
    (is (= 1 penalized-player-idx))
    (is (= [{:name "Player 1" :dice [1 1 1 1 1]}
            {:name "Player 2" :dice [2 2 2 2]}
            {:name "Player 3" :dice [3 3 3 3 4]}]
           players))))
