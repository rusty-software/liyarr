(ns liyarr.ahoy
  (:require
    [reagent.core :as reagent]
    [re-frame.core :as rf]
    [liyarr.config :as config]
    [liyarr.events]
    [liyarr.firebase :as firebase]
    [liyarr.views :as views]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (rf/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (rf/dispatch-sync [:initialize-db])
  (dev-setup)
  (firebase/init)
  (mount-root))
