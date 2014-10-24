# reclojure

Clojure in Clojure. Old style.

## What is this?

Clojure has a quite complicated Java codebase (along with the Clojure side of it called standard library) and as any other Java out there, it can be translated into Clojure. The approach adopted in this project takes class by class, method by method, almost line by line translation of the Java side of Clojure into Clojure itself, hopefully creating a very consistent translation. This was the original vision, circa 2009, with "new-new" that later became deftype/defprotocol/definterface and friends.

## Gotchas

* Implementing a purely functional data structure that offers persistence (like all core Clojure data structures) and at the same time avoid waste by internal sharing, **is going to involve state** although it is carefully hidden from you
* Java-like mutable objects are better implemented with mutable deftypes
* Abstract classes (something that was kept outside the scope of protocols by design in Clojure) needs to be implemented by assoc-ing general functions into deftypes definitions
* In Java, inhereting from a class that implements interfaces (and maybe those interefaces extend others) automatically implies that the resulting objects are conformant to all interfaces, from the first level or above. In Clojure there is no such a thing like inheritance of types, there is one level only. So the Java concrete type needs to be analyzed for all the inherited interfaces and the resulting Clojure deftype will have to implements all of them (like a mix-in).
* More gotchas to come soon.

## TODO

* Completely WIP, still not working, since the implementation of one persistent data structure (for example) implies a lot of dependencies to be implemented as well.

## License

Copyright © 2014 Reborg

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
