# reclojure

Clojure in Clojure the hard way.

## What is this?

Clojure has a quite complicated Java codebase (along with the Clojure side of it called standard library) and as any other Java out there, it can be translated into Clojure. The approach adopted in this project takes class by class, method by method, almost line by line translation of the Java side of Clojure into Clojure itself. This project will lead eventually to a very consistent translation.

## OO->FP challenges

* ClassB extends ClassA is resolved with a protocol ProtocolA for the abstract part of ClassA and a map map-impl-A containing names to functions impl for the concrete part of ClassA. ClassB is implemented as derecord ClassB extend ProtocolA + mix-in of map-impl-A + any other additional behaviour merged in the map. ClassA as such is never instantiated as a record, extactly like in Java there are no instances of abstract classes.
* Interfaces are mapped to their respective protocols. There is no way to extend protocols from other protocols, so interface extension cannot be resolved by protocols. It needs to be resolved at the concrete defrecord implementation by walking all the implemented interfaces and implementing them inline in the record declaration.
* Inner classes are translated as independent defrecords. If classA contains an inner classB, classB should receive an instantiation of classA when it is initialized, so it can access members of classA if necessary.

## TODO

*

## License

Copyright © 2014 Reborg

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
