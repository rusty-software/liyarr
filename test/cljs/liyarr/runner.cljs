(ns liyarr.runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [liyarr.game]))

(doo-tests 'liyar.game-test)
