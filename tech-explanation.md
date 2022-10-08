# Overview

In a single sentence: Liyarr's Dice is a multiplayer game utilizing Clojure/Script, re-frame, and firebase for serverless asset hosting and state management.

# Clojure/Script

Clojure is a functional language developed primarily by Rich Hickey and a bunch of other super smart folks. Rich and folks created a company, Cognitect, from which they promoted Clojure and worked on products related to and powered by Clojure.

ClojureScript is, at its core, a transpiler that allows for Clojure-like development to be transpiled into javascript so that it runs in a browser (or other places javascript can run).

The result of the above is that you can develop and deploy full-stack applications by "just" knowing the syntax involved in Clojure development. Pretty neat trick!


## reagent

The ClojureScript used in this project is augmented by reagent, a library that allows ClojureScript to interact/operate with React on the front end. The ability to define React components using ClojureScript functions is pretty powerful!

# re-frame

Most of you are probably familiar with the above section, but might be new to re-frame. Re-frame is a framework for developing reagent-powered ClojureScript apps. It's opinionated in that it prescribes where and how state is managed.

Wait, did I just say `state`?! There's `state` in our ClojureScript app?!

Yup. The truth of the matter is that modern web apps, especially single page applications, are FULL of state. State is, in fact, what makes the app interesting. State enables the app to _do things_. State is what allows React to... well, react to changes. Without state, React wouldn't recognize that something had happened. (note this is a bit of a simplification about how stuff works, but it's not completely reductive either)

Re-frame _isolates_ the mutation that inevitably happens in your app. How does it do that? Read on, my friends, and see for yourself!

Some of the real magic of re-frame is that it manages data flow via a directed acyclic graph, which will NOT be covered here.

## Data loop

The folks that manage the documentation for re-frame are very good. Truly, the documentation is a delight to read through, including the part that simply explains what's going on. Kudos to those folks!

The Data Loop provided by re-frame has six stages:

1. Event dispatch
2. Event handling
3. Effect handling
4. Query
5. View
6. DOM

### Event dispatch

An `event` occurs when _something happens_ in the app. Commonly, this is a user taking an action -- clicking a button, but can also be more indirect, like an HTTP request finishing.

Looks like:

```clojure
[:button
 {:class "button-primary"
  :on-click #(rf/dispatch [:boot-player i])}
 "Walk the plank!"]
```

When someone clicks the _Walk the plank!_ button, the `:boot-player` event is dispatched, and it gets whatever value `i` has as well.

### Event handling

When an `event` occurs, something's got to handle it somehow. Event handlers are the first step in that process. They are functions that are responsible for calculating the result of the event. The result is called an `effect`, and it's just a map. That means that there is NO mutation or other side effects here -- you can write tests against this if you really wanted to!

```clojure
(rf/reg-event-fx
  :boot-player
  (fn [{:keys [db] :as coeffects-map} [_ idx]]
    {:firebase/swap! {:path [(keyword (:game-code db))]
                      :function #(game/boot-player % idx)
                      :on-success #(println "boot player success")
                      :on-failure [:firebase-error]}}))
```

It's worth noting at this point that re-frame does a fair amount of wiring on application start up; anything prefixed with `(rf/reg-` is sniffed out. For example, the above event handler was registered as the application initialized so that it could handle `:boot-player` events using the function that takes the `coeffects-map` (which represents the current state of the app) and the event data vector. You can see the result of function, the `effect`, will be a map with key `:firebase/swap!` (which is the effect type) and value of another map (which is the effect data).

### Effect handling

Now we need something that will handle the `effect`s; something has to finally apply the mutation. In this app, all of the interesting state is actually stored in a firebase real-time database (more on that later); there's exactly *one* place that is handled:

```clojure
(rf/reg-fx :firebase/swap! firebase-transaction-effect)
```

`firebase-transaction-effect` is the actually worker function. 
There's another small library that's handling the interactions with firebase, and it's leveraged in the worker function. I'll hand-wave that, but if you're interested, you have have a look.

The main point is: the db has been changed! Our work is done! Right? ...right...?

Well, if there's no actual state change, then yes -- nothing else really happens. The loop ends, and the will begin again with some other event emerging. However, in the case that the state DOES change, our work is not quite finished.

### Query

That it's called "query" makes it feel like there's something you have to _actively_ do. Well, you do have to actively type `(rf/reg-sub` here and there, but re-frame handles the rest for you by calling the functions declared in those registered subscriptions when the state actually changes. 

The last thing we did, `:boot-player`, results in several changes, including at least a change to the `:players` collection. The `subscription` for that looks like:

```clojure
(rf/reg-sub
  :players
  (fn [_ _]
    (rf/subscribe [:game]))
  (fn [game _]
    (:players game)))
  ```
Don't worry too much about the first function there; it's called a `signal` function, and allows reuse of other subscriptions on which this one might depend. This subscription depends on the `:game` subscription, which is wired up to something in the firebase library. < waves hands again >

The more interesting bit is the second function. It's the one that allows for computation of view differences. Most (all?) of the stuff in this game only requires simple data extraction as opposed to anything computational, though.

### View

Almost there! Anything that happens to be subscribed to `:players` in the view has its components re-computed. This results in changed React components!

### DOM

Because all of this is React under the covers, the DOM is automatically updated for you by React (well, reagent helped). The new state is officially shown! THE JOURNEY HAS ENDED!

# Firebase

I promised I'd say a little about this, and I will. But only a little. :-D

Firebase is a suite of serverless services provided FOR FREE* by Google to every Google account. This app leverages two services in particular.

1. **Hosting**. All of the static assets (including the transpiled ClojureScript) are stored there.
2. **Real-time Database**. This is where all of the game state is saved. All of the data is stored as json.

There's not a lot more to say about it, honestly. Told you it would only be a little!

# Conclusion

That wraps up the brief tour through the technologies used to build this little app. I hope you've found this document to be helpful, but if you happen to have other questions, feel free to drop me a line or create an issue on Github. Happy liyarr-ing!