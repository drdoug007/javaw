# Bytecode timed latch and semaphore operations

Revision `timed-latch-semaphore-v327` links `CountDownLatch.await(long, TimeUnit)`,
`Semaphore.tryAcquire(long, TimeUnit)` and `Semaphore.tryAcquire(int, long, TimeUnit)` in the bundled exception
runtime under the single-threaded contract of `bytecode-timed-lock-queue.md`.

Contract:

- Timed calls never wait: with no other thread nothing can count the latch down or release a permit while a
  timed call would wait, so each returns at once the result the JDK would return after its timeout. No clock
  is read, no sleeping, no looping on the timeout.
- Every timed member first evaluates `unit.toNanos(timeout)` and ignores the result, so a `null` unit throws
  `NullPointerException` and leaves the state unchanged — before the permit/count test, but after the
  negative-permits check in `tryAcquire(int, long, TimeUnit)` (`permits < 0` ->
  `new IllegalArgumentException()`, message `null`).
- Timed `await` returns `true` exactly when the count is 0 (never `IllegalStateException`, never changes the
  count); timed `tryAcquire` returns `true` and takes the permits when the count allows, else `false` without
  change (including `tryAcquire(0, ...)` on a negative count). Fair and non-fair behave the same.
- Timed members never call their untimed siblings or any other overridable method on themselves (`await()`,
  `countDown()`, `getCount()`, `tryAcquire()`, `tryAcquire(int)`, `acquire*`, `availablePermits()`, `isFair()`);
  they read the fields and use the private helpers directly. None throws `InterruptedException` here, but each
  declares it.

Known differences: no clock is read and no time passes; the JDK would wait for the timeout in the failing
cases (positive count, not enough permits). Queue inspection (`hasQueuedThreads`, `getQueueLength`,
`getQueuedThreads`) is still not linked.

Evidence: `BytecodeTimedLatchSemaphoreTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `hasQueuedThreads`, `getQueueLength`, `getQueuedThreads` and `TimeUnit.sleep`
are rejected before WAT generation.
