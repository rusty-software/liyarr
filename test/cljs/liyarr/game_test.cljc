(ns liyarr.game-test
  (:require [clojure.test :refer [deftest testing is]]
            [liyarr.game :as game]))

(deftest test-roll
  (let [legit-value? (fn [dice] (every? #(and (< 0 %) (> 7 %)) dice))
        six-dice (game/roll 5)
        three-dice (game/roll 3)]
    (is (= 5 (count six-dice)))
    (is (legit-value? six-dice))
    (is (= 3 (count three-dice)))
    (is (legit-value? three-dice))))

(deftest test-bid)

(deftest test-challenge)

(deftest test-winner?)
