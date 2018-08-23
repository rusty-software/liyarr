(ns liyarr.ahoy
  (:require
   [reagent.core :as reagent]
   [re-frame.core :as re-frame]
   [liyarr.events :as events]
   [liyarr.views :as views]
   [liyarr.config :as config]
   [liyarr.firebase :as firebase]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root []
  (re-frame/clear-subscription-cache!)
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (firebase/init)
  (mount-root))
