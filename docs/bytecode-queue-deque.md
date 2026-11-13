# Bytecode Queue and Deque

Revision `queue-deque-v265` links `java.util.Queue` and `java.util.Deque` (JDK 17 members, no defaults) in the
bundled exception runtime, and `java.util.LinkedList` now implements `Deque`.

Contract:

- Calls through `Queue`/`Deque` dispatch to LinkedList's existing members and to user classes that implement
  the interfaces. Empty-deque failures are `NoSuchElementException`; `peek*`/`poll*` return null.
- `reversed()` (JDK 21 SequencedCollection) is not linked.

Evidence: `BytecodeQueueDequeTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `Deque.reversed()` and `LinkedList.reversed()` are rejected before WAT generation.