(ns liyarr.game)

(defn roll
  "Given a number of dice to roll, returns a vector of randomly rolled dice."
  [qty]
  (sort
    (into []
          (for [_ (range qty)]
            (inc (rand-int 6))))))

(defn larger-bid?
  "Given a first bid, returns true if the second bid is larger; otherwise, fasle."
  [first-bid second-bid]
  (let [{first-quantity :quantity first-rank :rank} first-bid
        {second-quantity :quantity second-rank :rank} second-bid]
    (or (> second-quantity first-quantity)
        (and (= second-quantity first-quantity)
             (> second-rank first-rank)))))
