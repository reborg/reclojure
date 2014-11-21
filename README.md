# reclojure

Clojure in Clojure, the hard way.

## What is this?

Clojure has a quite complicated Java codebase (along with the Clojure side of it called standard library) and as any other Java out there, it can be translated into Clojure. The approach adopted in this project takes class by class, method by method, almost line by line translation of the Java side of Clojure into Clojure itself creating a very close translation. It's going to be ugly at first, but I can always refactor it later.

## Gotchas

* Implementing purely functional data structures that offer persistence (like all core Clojure data structures) and at the same time avoid duplication waste by internal sharing, **is going to involve state** no matter how. So expect lot of Java interop, mutable deftypes and native arrays.
* Java-like mutable objects mutst be implemented with mutable deftypes, the only Clojure type that offers mutability.
* Abstract classes (something that was kept outside the scope of protocols by design in Clojure) needs to be implemented by assoc-ing general maps of shared functions into deftypes definitions.
* In Java, inhereting from a class that implements interfaces (and maybe those interefaces extend others) automatically implies that the resulting objects are conformant to all interfaces, from the first level or above. In Clojure there is no such a thing like inheritance of types, there is one level only. So the Java concrete type needs to be analyzed for all the inherited interfaces and the resulting Clojure deftype will have to implements all of them (like a mix-in).
* Bit shift operators need special handling to operate on ints from Clojure. Standard library shift operators are going to work on the Long version no matter what. The easiest way is just to call Numbers static method directly (no stdlib wrappers).

## Conventions

* Mutable types created with defmutable are following Java naming conventions for the mutable class name and fields: (defmutable MyMutable [foo bar baz])
* Implementations of functions defined in protocols, follow clojure functions naming conventions. The name is prefixed with -> to indicate that it implements a protocol function. It is also prefixed with initials of the protocol that is implemented to avoid name clashes in namespaces where multiple protocols are implemented. For example: PersistentHashMap::ensureEditable becomes ->phm-ensure-editable function implementation.

## TODO

* check all uses of aset, that after I'm correctly the returning the array. Even better get a macro to do te job of embedding that they way you want it, or use doto.
* evaluating if keeping java convention for defptorocol fn names, would prefer clj
* Completely WIP, still not working, since the implementation of one persistent data structure (for example) implies a lot of dependencies to be implemented as well.

## License

Copyright © 2014 Reborg

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
