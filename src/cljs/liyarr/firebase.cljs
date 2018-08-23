(ns liyarr.firebase
  (:require [cljsjs.firebase]))

(defn init []
  (js/firebase.initializeApp
    #js {:apiKey "AIzaSyAYLxAdesv9VvaZs4pJXuWEyhI-kavrdtg"
         :authDomain "liyarrs-dice.firebaseapp.com"
         :databaseURL "https://liyarrs-dice.firebaseio.com"
         :projectId "liyarrs-dice"
         :storageBucket "liyarrs-dice.appspot.com"
         :messagingSenderId "851773010925"}))