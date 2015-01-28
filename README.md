# reclojure

Clojure in Clojure, the hard way.

## What is this?

Clojure has a quite extensive Java codebase at its core. It boostraps from Java first and moves to Clojure at higher levels of abstraction. The Java core implements persistent data structures, the STM, class loading and generation facilities, reader, compiler and much more.

As any other Java out there, Clojure Java core can be translated into Clojure through java-interop. The approach adopted here is a 1:1 class by class, method by method, almost line by line translation of Java side into Clojure itself.

The idea is that by avoiding a big upfront re-engeineering, all the care received by Clojure over the years will be preserved instead of chasing compatibility with the reference implementation after the facts.

BTW this is fun stuff!

## basic approach

* find and translate some chunk of usable functionality
* if there is a "main" in original code use it to exercise the code and output a "trace"
* produce the same trace from translated code and compare the two
* fix bugs and problems until traces are the same
* optionally, create trace from ad-hoc unit test if no main is present
* freeze execution and trace comparison in a unit test to prevent regressions
* rinse and repeat

## Current status

* Jan 2015: PersistentHashMap delete (without)
* Nov 2014: PersistentHashMap and Transient brother can assoc elements in. Collision node and array map node are not yet implemented.
* Oct 2014: bulk of PersistentHashMap code translated.

## Gotchas

* Implementing purely functional persistent data structures and at the same time avoid duplication by internal sharing **is going to involve state** no matter what. So expect lot of Java interop, mutable deftypes and native arrays.
* Java-like mutable objects must be implemented with mutable deftypes, the only Clojure type that offers mutability.
* Abstract classes (something that was kept outside the scope of protocols by design) needs to be implemented by assoc-ing general maps of shared functions into deftypes definitions.
* In Java, inhereting from a class that implements interfaces (and maybe those interefaces extend others) automatically implies that the resulting objects implements all the interfaces. In Clojure there is no such a thing like inheritance of types, there is one level only. So the Java concrete type needs to be analyzed for all the inherited interfaces and the resulting Clojure deftype will have to implements all of them (like a mix-in).
* Bit shift operators need special handling to operate on ints from Clojure. Standard library shift operators are going to work on the Long version no matter what. The easiest way is just to call Numbers static method directly (no stdlib wrappers).

## Conventions

* Mutable types created with defmutable follow the Java naming conventions for the mutable class name and fields: (defmutable MyMutable [foo bar baz])
* Implementations of functions defined in protocols, follow clojure functions naming conventions. The name is prefixed with -> to indicate that it implements a protocol function. It is also prefixed with initials of the protocol that is implemented to avoid name clashes in namespaces where multiple protocols are implemented. For example: PersistentHashMap::ensureEditable becomes ->phm-ensure-editable function implementation.

## TODO

* please use consistency of "this" instances, sometimes you call them "node" sometimes something else. All 'this' please.
* things like key-or-null that are calc over and over again could be part of the deftype
* open up type hints warning and enjoy the Matrix
* implementation of arraynode and collisionnode is the current WIP
* all uses of satisfies? to check against deftype (which will never work)
* for all bitmap manipulation operations, use Numbers/static call directly
* check all uses of aset, that after using it I'm correctly the returning the array. Even better get a macro to do te job of embedding that they way you want it, or use doto. Doto is the best.
* evaluating if keeping java convention for defptorocol fn names, would prefer clj, but change after integration tests are all passing
* now that INT coercion was put in right place, are those cast to INT still needed? Try remove once integration tests is working. I.e. ->bin-assoc unchecked int stuff.

## License

Copyright © 2015 Reborg

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
