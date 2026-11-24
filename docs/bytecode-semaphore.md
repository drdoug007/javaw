# Bytecode Semaphore

Revision `semaphore-v318` links `java.util.concurrent.Semaphore` in the bundled exception runtime under the
single-threaded contract: one plain signed permit count, nothing blocks and nothing interrupts.

Contract:

- `Semaphore` implements `java.io.Serializable` and holds one plain signed `int permits` field (negative
  counts are allowed) and a `final boolean fair` flag. Both constructors store the count as given,
  including negatives and `Integer.MIN_VALUE`, with no check; the one-argument constructor is not fair and
  `isFair()` returns the constructor flag. `acquire()`/`acquire(int)` and `acquireUninterruptibly()`/
  `acquireUninterruptibly(int)` check the argument first (`n < 0` throws `new IllegalArgumentException()`,
  null message) and then take `n` permits when `permits - n >= 0`. `tryAcquire()`/`tryAcquire(int)` apply
  the same check and return `false` without changing the count when not enough permits are available
  (`tryAcquire(0)` is `true` on a zero count and `false` on a negative count). `release()`/`release(int)`
  check `n < 0` first, then add with plain `int` arithmetic and throw `new Error("Maximum permit count
  exceeded")` when the sum wraps (`next < permits`), leaving the count unchanged. `availablePermits()`
  returns the count. `drainPermits()` returns the count and sets `0`, also when negative.
  `reducePermits(int)` (protected) checks `r < 0` first, then subtracts and throws `new Error("Permit count
  underflow")` when `next > permits`, otherwise the count may go negative. `toString()` is
  `super.toString() + "[Permits = " + permits + "]"` — Object's `toString`, which calls the overridable
  `hashCode()`, with the field read directly.
- No overridable self-calls: `acquire()` does not call `acquire(1)`, `release()` does not call
  `release(1)`, `tryAcquire()` does not call `tryAcquire(1)`, `toString()` reads the field rather than
  calling `availablePermits()`, and `drainPermits`/`reducePermits` call nothing. `equals` and `hashCode`
  are not overridden (identity, inherited from `Object`).

Known differences: there is no `AbstractQueuedSynchronizer`. `acquire`/`acquireUninterruptibly` that would
have to wait throw IllegalStateException instead of blocking forever; no thread could ever release.
Timed `tryAcquire` (a positive timeout would have to wait) and the queue inspection methods are not linked.

Evidence: `BytecodeSemaphoreTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). The timed `tryAcquire` overloads, `hasQueuedThreads`, `getQueueLength` and
`getQueuedThreads` are rejected before WAT generation.
