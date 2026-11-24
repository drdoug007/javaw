# Bytecode CountDownLatch

Revision `count-down-latch-v316` links `java.util.concurrent.CountDownLatch` in the bundled exception runtime
under the single-threaded contract.

Contract:

- `CountDownLatch` extends `java.lang.Object`, implements nothing, and holds one plain `int count` field.
  The constructor `(int)` throws `new IllegalArgumentException("count < 0")` (no cause) for any negative
  count, including `Integer.MIN_VALUE`; `0` and `Integer.MAX_VALUE` are allowed. `countDown()` decrements
  only while `count > 0` and never goes below `0`. `getCount()` returns `count` widened to `long`.
  `await()` returns immediately (any number of times) when `count == 0` and declares `throws
  InterruptedException` (it never throws it). `toString()` is `super.toString() + "[Count = " + count +
  "]"`, calling Object's `toString` (so the virtual `hashCode()` runs) and reading the field directly.
- No overridable self-calls: `countDown()`, `await()`, `getCount()` and `toString()` never call each
  other, and none of them calls any other overridable method. `equals` and `hashCode` are not overridden
  (identity, inherited from `Object`), and the class is not `Serializable`.

Known differences: no `AbstractQueuedSynchronizer` (the selected deployment is single-threaded). `await()` with a positive count
throws IllegalStateException instead of blocking forever; no thread could ever count down. `await(long,
TimeUnit)` is not linked (a positive timeout would have to wait).

Evidence: `BytecodeCountDownLatchTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `CountDownLatch.await(long, TimeUnit)` is rejected before WAT generation.
