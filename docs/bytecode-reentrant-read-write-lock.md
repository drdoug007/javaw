# Bytecode ReadWriteLock and ReentrantReadWriteLock

Revision `reentrant-read-write-lock-v311` links `java.util.concurrent.locks.ReadWriteLock` and
`ReentrantReadWriteLock` (with `ReadLock` and `WriteLock`) in the bundled exception runtime. This contract applies
only to the single-threaded deployment, like `ReentrantLock`: two plain hold counters, no queue and no Java
Memory Model ordering claimed.

Contract:

- `ReadWriteLock` is an interface declaring only `Lock readLock()` and `Lock writeLock()`.
  `ReentrantReadWriteLock` implements it (and `java.io.Serializable`) with `private final boolean fair`,
  `private int readHolds` (all read holds), `private int writeHolds` (write holds) and the two final nested
  lock fields. `ReentrantReadWriteLock()` delegates to `this(false)`; both constructors store `fair` and then
  create the one shared `ReadLock` and `WriteLock` instance, so `readLock()` and `writeLock()` always return
  the same two objects and javac's covariant-`Lock` bridges return them through the interface too.
- `ReadLock.lock()`, `lockInterruptibly()` and `tryLock()` always succeed and each increments `readHolds` by
  one, even while the write lock is held (the downgrade path); `tryLock()` returns `true`. They are
  re-entrant and none calls another overridable method. If `readHolds` is `65535` each throws
  `new Error("Maximum lock count exceeded")` and changes nothing.
- `WriteLock.lock()` and `lockInterruptibly()` increment `writeHolds` when `readHolds == 0` or
  `writeHolds != 0` (re-entrant). If `readHolds != 0` and `writeHolds == 0` (a read-to-write upgrade, which
  the JDK would block on forever) they throw
  `new IllegalMonitorStateException("write lock would block forever: read lock held")` instead. At
  `writeHolds == 65535` they throw `new Error("Maximum lock count exceeded")`.
- `WriteLock.tryLock()` returns `false` and changes nothing exactly when `readHolds != 0` and
  `writeHolds == 0`; it returns `true` and increments `writeHolds` otherwise, including when the same
  lock already holds the write lock while reads are held. At `writeHolds == 65535` it throws the same
  `Error`.
- `ReadLock.unlock()` throws `new IllegalMonitorStateException("attempt to unlock read lock, not locked by
  current thread")` (even while the write lock is held) and changes nothing when `readHolds == 0`; otherwise
  it decrements `readHolds`. `WriteLock.unlock()` throws `new IllegalMonitorStateException()` (null message)
  and changes nothing when `writeHolds == 0`; otherwise it decrements `writeHolds`.
- Queries: `getReadLockCount()` and `getReadHoldCount()` return `readHolds`; `getWriteHoldCount()` and
  `WriteLock.getHoldCount()` return `writeHolds`; `isWriteLocked()`, `isWriteLockedByCurrentThread()` and
  `WriteLock.isHeldByCurrentThread()` return `writeHolds != 0`; `isFair()` (the only final method) returns
  the constructor flag, which changes nothing else. `equals` and `hashCode` are not overridden (identity).
- `toString()` formats: outer `super.toString() + "[Write locks = " + w + ", Read locks = " + r + "]"`,
  `ReadLock` `super.toString() + "[Read locks = " + r + "]"`, `WriteLock` `super.toString() + "[Unlocked]"`
  when `writeHolds == 0`, else `super.toString() + "[Locked by thread main]"`. `super.toString()` is
  `Object.toString()` (`getClass().getName() + "@" + Integer.toHexString(hashCode())`), so the virtual
  `hashCode()` is used and nothing is inserted between the hash and `[`.
- Both nested locks are `public static`, keep a `private final ReentrantReadWriteLock lock` and read or write
  `lock.readHolds` / `lock.writeHolds` directly; no method in either class or the outer lock calls another
  overridable method of the lock hierarchy, so subclass overrides observe only their own direct calls.
- The `protected` nested constructors share the given lock's counters (they take no state of their own) and
  throw `NullPointerException` for a `null` argument via `Objects.requireNonNull`.

Known differences: a blocking read-to-write upgrade `WriteLock.lock()` throws IllegalMonitorStateException
instead of deadlocking; `newCondition()` and timed `tryLock` are not linked; `WriteLock.toString()` always
names thread `main`; no `Sync`/AQS superclass; serialization is not linked.

Evidence: `BytecodeReentrantReadWriteLockTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `hasQueuedThreads()`, `getQueueLength()`, `hasWaiters(Condition)`, `newCondition()` and timed `tryLock` are rejected
before WAT generation.
