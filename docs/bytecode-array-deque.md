# Bytecode ArrayDeque

Revision `array-deque-v266` links `java.util.ArrayDeque` (a resizable ring buffer implementing `Deque`) in the
bundled exception runtime.

Contract:

- JDK null rules (null elements rejected with NPE; null queries return false), NoSuchElementException on empty
  ends, iterator remove/next state failures, clone keeps subclasses. `equals`/`hashCode` stay identity-based,
  as in the JDK.
- Iterators are not fail-fast: they walk logical indexes, so changing the deque during iteration (other than
  through the iterator) can skip or repeat elements where the JDK throws `ConcurrentModificationException`.
  `addAll(this)` is the exception: like the JDK it appends the original elements and then throws
  `ConcurrentModificationException` (an empty deque returns false).
- `addAll` adds elements one at a time, so the elements before a null stay added when it throws NPE.
- `removeAll`, `retainAll` and `removeIf` are the inherited iterator-removal versions and `delete` always
  shifts the tail, so bulk removal is O(n²) where the JDK's `bulkRemove` is linear.
- `reversed()`, spliterator and serialization are not linked (the fields are transient and there are no
  `writeObject`/`readObject` hooks).

Evidence: `BytecodeArrayDequeTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances), including wrapped and growing buffers. `ArrayDeque.reversed()` is rejected before WAT
generation.
