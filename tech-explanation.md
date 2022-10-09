# Overview

In a single sentence: Liyarr's Dice is a multiplayer game utilizing Clojure/Script, re-frame, and firebase for serverless asset hosting and state management.

# Clojure/Script

Clojure is a functional language developed primarily by Rich Hickey and a bunch of other super smart folks. Rich and folks created a company, Cognitect, from which they promoted Clojure and worked on products related to and powered by Clojure.

ClojureScript is, at its core, a transpiler that allows for Clojure-like development to be transpiled into JavaScript so that it runs in a browser (or other places JavaScript can run).

The result of the above is that you can develop and deploy full-stack applications by "just" knowing the syntax involved in Clojure development. Pretty neat trick!


## reagent

The ClojureScript used in this project is augmented by reagent, a library that allows ClojureScript to interact/operate with React on the front end. The ability to define React components using ClojureScript functions is pretty powerful!

# re-frame

Most of you are probably familiar with the above section, but might be new to re-frame. Re-frame is a framework for developing reagent-powered ClojureScript apps. It's opinionated in that it prescribes where and how state is managed.

Wait, did I just say _state_?! There's _state_ in our ClojureScript app?!

Yup. The truth of the matter is that modern web apps, especially single page applications, are FULL of state. State is, in fact, what makes the app interesting. State enables the app to _do things_. State is what allows React to... well, react to changes. Without state, React wouldn't recognize that something had happened. (note this is a bit of a simplification about how stuff works, but it's not completely reductive either)

Re-frame _isolates_ the mutation that inevitably happens in your app. How does it do that? Read on, my friends, and see for yourself!

_Aside: some of the real magic of re-frame is that it manages data flow via a directed acyclic graph, which will NOT be covered here._

## Data loop

The folks that manage the documentation for re-frame are very good. Truly, [the documentation](https://day8.github.io/re-frame/re-frame/) is a delight to read through, including the part that simply explains what's going on. Kudos to those folks!

The Data Loop provided by re-frame has six stages:

1. Event dispatch
2. Event handling
3. Effect handling
4. Query
5. View
6. DOM

Let's see how those work in our little application.

### Event dispatch

An _event_ occurs when _something happens_ in the app. Commonly, this is a user taking an action, e.g. clicking a button. It can also be more indirect, like an HTTP request finishing.

For example:

```clojure
[:button
 {:class "button-primary"
  :on-click #(rf/dispatch [:boot-player i])}
 "Walk the plank!"]
```

When someone clicks the _Walk the plank!_ button, the `:boot-player` event is dispatched, and it gets whatever value `i` has as well.

### Event handling

When an _event_ occurs, something's got to handle it somehow. Enter: event handlers. They are functions that are responsible for calculating the result of the event, meaning, the effect the event would have on the overall state of the application. This _effect_ that results from the calculation is just a map. That means that there is NO mutation or other side effects here -- you can write tests against this if you really wanted to!

```clojure
(rf/reg-event-fx
  :boot-player
  (fn [{:keys [db] :as coeffects-map} [_ idx]]
    {:firebase/swap! {:path [(keyword (:game-code db))]
                      :function #(game/boot-player % idx)
                      :on-success #(println "boot player success")
                      :on-failure [:firebase-error]}}))
```

The function that is returned from `(rf/reg-event-fx...)` is the _actual_ event handler. 

It's worth noting again here that re-frame is _opinionated_. If you want to use it, you need to have emitters and handlers property _registered_. Luckily, it's simple enough to do: implement the appropriate `(rf/reg-*` function. For example, the above function registers an event handling function with re-frame's data loop so that it can handle `:boot-player` events. The event handling function takes the _coeffects-map_ (which represents the current state of the app) and the event data vector. You can see the result of the handler, the _effect_, will be a map with key `:firebase/swap!` (which is the effect type) and value of another map (which is the effect data).

### Effect handling

Now we need something that will handle the _effect_; something has to finally apply the mutation. In this app, all the interesting state is actually stored in a firebase real-time database (more on that later); there's exactly *one* place that is handled:

```clojure
(rf/reg-fx :firebase/swap! firebase-transaction-effect)
```

`firebase-transaction-effect` is the actual worker function. There's another small library that's handling the interactions with firebase, and it's leveraged in the worker function. I'll hand-wave that, but if you're interested, you can have a look.

The main point is: the db has been changed! Our work is done! Right? ...right...?

Well, if there's no actual state change, then yes -- nothing else really happens. The loop ends, and will begin again when some other event emerges. However, in the case that the state DOES change, our work is not quite finished.

### Query

That it's called "query" makes it feel like there's something you have to _actively_ do. Well, you do have to actively type `(rf/reg-sub` here and there, but re-frame handles the rest for you by calling the functions declared in those registered subscriptions when the state actually changes. 

The last thing we did, `:boot-player`, had the following as it's `:function` value:

```clojure
(game/boot-player % idx)
```

The first argument was the coeffects map which held the game state. One of the values in the game state was a `:players` collection, which, as you might have guessed, changed as the result of a player being booted. The _subscription_ for that looks like:

```clojure
(rf/reg-sub
  :players
  (fn [_ _]
    (rf/subscribe [:game]))
  (fn [game _]
    (:players game)))
  ```

The first function in the subscription is called a _signal_ function, and allows reuse of other subscriptions on which the current one might depend. Our `:players` subscription depends on the `:game` subscription, which is wired up to something in the firebase library. 

In our application, the state for any particular game is stored in firebase under a `game` key (games are started with a specific key value). The subscription that is relied on here, `:game`, is a listener to changes to values in the state associated with our game's particular key. < waves hands a bit >

The more interesting bit is the second function: the _computation_ function. It's the one that allows for computation of view differences. Most (all?) of the stuff in this game only requires simple data extraction as opposed to anything computational, though. In this case, the `:players` collection has changed, so the new value is extracted by the computation function.

### View

Almost there! Anything that happens to be subscribed to `:players` in the view has its components re-computed with the new values extracted by the _subscription_. This results in changed React components!

### DOM

Because all of this is React under the covers, the DOM is automatically updated for you by React (well, reagent helped). The new state is officially shown! THE JOURNEY HAS ENDED!

# Firebase

I promised I'd say a little about this, and I will. But only a little. :-D

Firebase is a suite of serverless services provided FOR FREE`*` by Google to every Google account. This app leverages two services in particular.

1. **Hosting**. All the static assets (including the transpiled ClojureScript) are stored there.
2. **Real-time Database**. This is where all the game state is saved. All the data is stored as json.

I'll mention something that might've been bugging you at this point. How the heck does storing game state in a serverless real-time db allow for the multiplayer experience we're enjoying? If you suspected _long-lived websockets_ are established on loading the page, you were right! Firebase broadcasts state changes to all connected clients as they are submitted, so that everyone gets the new state as fast as the Internet can transmit them! This fits in beautifully with re-frame's data loop, as the broadcast changes are pushed into the client's state, allowing events to fire and subscriptions to update.

There's not a lot more to say about it, honestly, at least not in the context of this little game and this little page. Told you it would only be a little!

`*` FREE to a limit, beyond which you must pay for stuff. Luckily, this game isn't so popular as we have ever approached the limit. :-D

# Conclusion

That wraps up the brief tour through the technologies used to build this little app. I hope you've found this document to be helpful, but if you happen to have other questions, feel free to drop me a line or create an issue on GitHub. Happy liyarr-ing!