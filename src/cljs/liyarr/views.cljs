(ns liyarr.views
  (:require
   [re-frame.core :as re-frame]
   [liyarr.subs :as subs]
   ))

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])]
    [:div
     [:h1 "Heya from " @name]
     ]))
