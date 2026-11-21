# Bytecode ReentrantLock

Revision `reentrant-lock-v310` links `java.util.concurrent.locks.Lock`, `ReentrantLock`,
`java.lang.IllegalMonitorStateException` and `java.lang.InterruptedException` in the bundled exception runtime.
This contract applies only to the single-threaded deployment, like `AtomicLong`: one plain hold count, the
current thread is always the owner, nothing blocks and nothing interrupts.

Contract:

- `Lock` is a small interface declaring only `lock()`, `lockInterruptibly()` (throws
  `InterruptedException`), `tryLock()` and `unlock()`. `ReentrantLock` implements it (and
  `java.io.Serializable`) with one `private final boolean fair` and a `private int holds` field where
  `0` means unlocked. Both constructors set `fair` (the no-argument one to `false`).
- `lock()`, `lockInterruptibly()` and `tryLock()` are re-entrant: each increments `holds` by one and none of
  them calls another `Lock` method, so a subclass override of one never triggers another. `tryLock()` always
  succeeds and returns `true`, even when the lock is already held or the lock is fair (the JDK's untimed
  `tryLock` barges). `lockInterruptibly()` never throws `InterruptedException` because nothing can interrupt.
  If `holds` were `Integer.MAX_VALUE`, each acquisition method throws `new Error("Maximum lock count exceeded")`
  and leaves `holds` unchanged.
- `unlock()` throws `new IllegalMonitorStateException()` (no message, no cause) and changes nothing when
  `holds == 0`; otherwise it decrements `holds`. It calls no other overridable method.
- `getHoldCount()` returns `holds`; `isLocked()` and `isHeldByCurrentThread()` return `holds != 0`;
  `isFair()` (final) returns the constructor flag.
- `toString()` is `super.toString()` — `Object.toString()`, which calls the virtual `hashCode()` — followed by
  `[Unlocked]` when `holds == 0`, else `[Locked by thread main]`. It reads `holds` directly and never calls
  `isLocked()`, `getHoldCount()` or `isHeldByCurrentThread()`.
- `IllegalMonitorStateException` extends `RuntimeException` and `InterruptedException` extends `Exception`
  (it is checked); both have no-argument and `String` constructors passing the message to `super`.
  `equals` and `hashCode` are not overridden on `ReentrantLock` (identity, like the JDK).

Known differences: there is no `AbstractQueuedSynchronizer`; the owner name in `toString` is always `main`.
`newCondition`, `Condition`, `tryLock(long, TimeUnit)` and the queue inspection methods are not linked.

Evidence: `BytecodeReentrantLockTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `newCondition`, timed `tryLock`, `getQueueLength` and `hasQueuedThreads` are rejected
before WAT generation.
