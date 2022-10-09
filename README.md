# Liyarr's Dice

Liyarr’s Dice ([Liar’s Dice](https://en.m.wikipedia.org/wiki/Liar%27s_dice)) is a multiplayer browser-based implementation of the common party dice game of the same name. It’s also known as Pirate’s Dice, as popularized in the second installment of the Pirates of the Caribbean movie. I first played it as a mini-game named _Stones of Wisdom_ in a fantasy game on the Commodore 64 named [Legacy of the Ancients](https://en.wikipedia.org/wiki/Legacy_of_the_Ancients). Apparently I lacked serious wisdom, because I always lost money playing it. It’s fun for a group of people that want to try to fake each other out.

It's built using [re-frame](https://github.com/Day8/re-frame) and [Google Firebase](https://firebase.google.com/). [Click here to read more about the tech stack.](tech-explanation.md)

## Development Mode

### Run application:

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
