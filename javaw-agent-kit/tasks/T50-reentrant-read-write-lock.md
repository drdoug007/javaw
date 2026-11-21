# T50 ReadWriteLock and ReentrantReadWriteLock

```kit
id: T50
slug: reentrant-read-write-lock
title: link java.util.concurrent.locks.ReadWriteLock and ReentrantReadWriteLock (single-threaded)
tests: BytecodeReentrantReadWriteLockTest
depends: T49
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReadWriteLock.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReentrantReadWriteLock.java
allow: docs/bytecode-reentrant-read-write-lock.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-reentrant-read-write-lock-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeReentrantReadWriteLockTest.java`.

## Goal

Add the interface `java.util.concurrent.locks.ReadWriteLock` and the class
`java.util.concurrent.locks.ReentrantReadWriteLock` (with its public nested classes `ReadLock` and `WriteLock`) to
the bundled runtime. `runtime-target/target/audit/external-types.tsv` names `java/util/concurrent/locks/ReadWriteLock`
from Tomcat `ContainerBase`, `StandardContext`, juli `FileHandler`, Tomcat EL `ExpressionFactory`/`Util`, log4j
`AbstractLoggerAdapter` and `StatusLogger` (in the static closure). The member rows (`member-references.tsv`) are:
`ReentrantReadWriteLock.<init>()V` (Tomcat `ContainerBase.<init>` 18 sites, Hibernate `AbstractReadWriteAccess.<init>`
4, H2 `FileNioMemData.<init>`, Spring Data `AbstractMappingContext.<init>`, Spring MVC
`AbstractHandlerMethodMapping$MappingRegistry.<init>`, jaxb `Injector.<clinit>`, Tomcat EL), `<init>(Z)V` (log4j
`AbstractLoggerAdapter.<init>`), `readLock()` / `writeLock()` with the covariant `$ReadLock` / `$WriteLock` return
types (Tomcat `StandardWrapper` 23 + 23, Spring MVC 2 + 7, H2 1 + 4), `ReadWriteLock.readLock()Ljava/util/concurrent/locks/Lock;`
/ `writeLock()` through the interface (Tomcat 27 + 43, Hibernate `DriverManagerConnectionProviderImpl$PoolState`
8 + 13, log4j 3 + 15, Spring Data `ReadWriteLock.of`), and `ReentrantReadWriteLock$ReadLock.lock()/unlock()` and
`$WriteLock.lock()/unlock()` (Tomcat `StandardWrapper` 6/12 and 6/12, Spring MVC, H2 `FileNioMemData.truncate`).

The runtime is **single-threaded** (same contract as `AtomicLong`, `docs/bytecode-atomic-integer-long.md`, and
T49's `ReentrantLock`): the only thread is the current thread, so it always "owns" every hold. Keep three plain
`int`/`boolean` fields in the outer class; no `AbstractQueuedSynchronizer`, `Sync`, `HoldCounter`, `ThreadLocal`,
`Thread`, `volatile` or `synchronized` (RULES.md).

T49 provides `java.util.concurrent.locks.Lock` (with only `lock`, `lockInterruptibly`, `tryLock()` and `unlock`),
`java.lang.IllegalMonitorStateException` and `java.lang.InterruptedException`. `TimeUnit` and `Condition` are **not**
in the runtime (T49 leaves them to a later task), so this task leaves out `tryLock(long, TimeUnit)` and
`newCondition()` too. If any of the three T49 classes is missing, stop and `kit block T50` (do not add them here).

## Members to implement

`java/util/concurrent/locks/ReadWriteLock.java`: `public interface ReadWriteLock` with exactly
`Lock readLock();` and `Lock writeLock();`.

`java/util/concurrent/locks/ReentrantReadWriteLock.java`:
`public class ReentrantReadWriteLock implements ReadWriteLock, java.io.Serializable`

- fields: `private final boolean fair; private int readHolds; private int writeHolds;`
  `private final ReentrantReadWriteLock.ReadLock readerLock; private final ReentrantReadWriteLock.WriteLock writerLock;`
- `public ReentrantReadWriteLock()` (calls `this(false)`), `public ReentrantReadWriteLock(boolean fair)` (stores
  `fair`, then `readerLock = new ReadLock(this); writerLock = new WriteLock(this);`)
- `public ReentrantReadWriteLock.ReadLock readLock()`, `public ReentrantReadWriteLock.WriteLock writeLock()`
  (return the fields; javac adds the `Lock`-returning bridge methods itself)
- `public final boolean isFair()`
- `public int getReadLockCount()`, `public int getReadHoldCount()` (both `readHolds`)
- `public boolean isWriteLocked()`, `public boolean isWriteLockedByCurrentThread()` (both `writeHolds != 0`)
- `public int getWriteHoldCount()` (`writeHolds`)
- `public String toString()`

`public static class ReadLock implements Lock, java.io.Serializable` (nested, not final):

- `private final ReentrantReadWriteLock lock;`
- `protected ReadLock(ReentrantReadWriteLock lock)`
- `public void lock()`, `public void lockInterruptibly() throws InterruptedException`, `public boolean tryLock()`,
  `public void unlock()`, `public String toString()`

`public static class WriteLock implements Lock, java.io.Serializable` (nested, not final):

- `private final ReentrantReadWriteLock lock;`
- `protected WriteLock(ReentrantReadWriteLock lock)`
- the same five methods as `ReadLock`, plus `public boolean isHeldByCurrentThread()` and `public int getHoldCount()`

No method except `isFair()` is final.

## Members to leave out

- Queue and condition queries: `hasQueuedThreads()`, `hasQueuedThread(Thread)`, `getQueueLength()`,
  `hasWaiters(Condition)`, `getWaitQueueLength(Condition)`, and the protected `getOwner()`, `getQueuedThreads()`,
  `getQueuedWriterThreads()`, `getQueuedReaderThreads()`, `getWaitingThreads(Condition)` (they need `Thread` or a
  real `Condition`). The test checks `hasQueuedThreads()Z`, `getQueueLength()I` and
  `hasWaiters(Ljava/util/concurrent/locks/Condition;)Z` stay unlinked (`leavesOutQueueQueries`).
- `tryLock(long, TimeUnit)` and `newCondition()` on both nested locks (no `TimeUnit`/`Condition` yet; do not
  import or mention either type). The test checks both stay unlinked on `$ReadLock` and `$WriteLock`.
- `Sync`, `NonfairSync`, `FairSync`, `HoldCounter`, `ThreadLocalHoldCounter`, `serialVersionUID`, `readObject`.
- `equals`/`hashCode`: not overridden (identity), like the JDK.
- Do not edit any other runtime file, including T49's `Lock`, `ReentrantLock` and `IllegalMonitorStateException`.

## JDK behaviour to match

State: `readHolds` (all read holds) and `writeHolds` (write holds); with one thread, "held by another thread" never
happens.

- `ReadLock.lock()` / `lockInterruptibly()` / `tryLock()`: always succeed (even while the write
  lock is held: that is the downgrade path) unless `readHolds == 65535`, where they throw
  `new Error("Maximum lock count exceeded")` and change nothing. On success `readHolds++`; `tryLock*` return `true`.
- `WriteLock.lock()` / `lockInterruptibly()`: if `readHolds == 0` or `writeHolds != 0` (reentrant), then
  `writeHolds == 65535` throws `new Error("Maximum lock count exceeded")`, otherwise `writeHolds++`. If
  `readHolds != 0 && writeHolds == 0` the JDK deadlocks forever (read-to-write upgrade); here throw
  `new IllegalMonitorStateException("write lock would block forever: read lock held")` (not tested, document it).
- `WriteLock.tryLock()`: return `false` and change nothing when `readHolds != 0 && writeHolds == 0`
  (upgrade fails); `true` and `writeHolds++` otherwise, including when this thread holds both write and read
  (downgraded holders may re-take the write lock). At `writeHolds == 65535` they throw the same `Error`.
- `ReadLock.unlock()`: if `readHolds == 0` throw `new IllegalMonitorStateException("attempt to unlock read lock, not
  locked by current thread")` (also while the write lock is held), else `readHolds--`.
- `WriteLock.unlock()`: if `writeHolds == 0` throw `new IllegalMonitorStateException()` (message `null`), else
  `writeHolds--`.
- Queries: `getReadLockCount()` and `getReadHoldCount()` return `readHolds`; `getWriteHoldCount()` and
  `WriteLock.getHoldCount()` return `writeHolds`; `isWriteLocked()`, `isWriteLockedByCurrentThread()` and
  `WriteLock.isHeldByCurrentThread()` return `writeHolds != 0`. `isFair()` returns the constructor flag (fairness
  changes nothing else).
- `readLock()` / `writeLock()` always return the same two objects. A `ReadLock`/`WriteLock` made through the
  protected constructor (`new ReentrantReadWriteLock.ReadLock(rw){}` or a subclass) shares `rw`'s state; a null
  argument throws `NullPointerException` from the constructor (dereference it, e.g.
  `java.util.Objects.requireNonNull(lock)`; no message compared).
- `toString()` (each starts with `super.toString()`, i.e. `getClass().getName() + "@" + Integer.toHexString(hashCode())`):
  outer `super.toString() + "[Write locks = " + w + ", Read locks = " + r + "]"`; ReadLock
  `super.toString() + "[Read locks = " + r + "]"`; WriteLock `super.toString() + "[Unlocked]"` when `writeHolds == 0`,
  otherwise `super.toString() + "[Locked by thread main]"` (the JDK prints the owner thread's name; the test only
  checks the `[Locked by thread ` prefix and the `]` end).
- No self-use: none of these methods calls another overridable method of the outer lock or of the nested locks. Read
  the fields directly (`lock.readHolds`, `lock.writeHolds`): a subclass overriding `getReadLockCount`, `readLock`,
  `getHoldCount`, `lock`, `tryLock` etc. sees only its own direct calls (`noSelfUse`, `protectedConstructors`).

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeReentrantReadWriteLockTest.java`.
2. Read T49's `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReentrantLock.java` and
   `Lock.java` (same package and single-threaded style, the exact `Lock` method list).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReadWriteLock.java`.
4. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReentrantReadWriteLock.java`
   with both nested classes in the same file.
5. Register both: `javaw-agent-kit/bin/kit register java/util/concurrent/locks/ReadWriteLock.java` and
   `javaw-agent-kit/bin/kit register java/util/concurrent/locks/ReentrantReadWriteLock.java` (never edit
   `BytecodeExceptionRuntime.java` by hand; the nested classes come from the same file).
6. `javaw-agent-kit/bin/kit check T50` until PASS.
7. Write `docs/bytecode-reentrant-read-write-lock.md` (below), the report, then `javaw-agent-kit/bin/kit finish T50`.

## Hints

- Do not copy the JDK source: it needs `AbstractQueuedSynchronizer`, `Thread`, `ThreadLocal` and `LockSupport`.
  The whole file is about 90 lines of one-line methods.
- The nested classes are `static` and keep a reference to the outer lock; they change `lock.readHolds` /
  `lock.writeHolds` (private fields of the enclosing class are accessible from nested classes).
- Check the `65535` limit **before** incrementing: `readLimit` takes 65534 holds with `lock()`, one more with
  `tryLock()` (true), then expects the `Error` from both `lock()` and `tryLock()`.
- `WriteLock.tryLock()` returns `false` only when reads are held and writes are not. It must not throw there.
- The two unlock messages differ: read has the long message, write has none (`new IllegalMonitorStateException()`).
- `lockInterruptibly()` never throws `InterruptedException` here (no thread can interrupt); declare the `throws`
  anyway because `Lock` declares it.
- Do not write the lowercase words `volatile`, `synchronized`, `native` or `Thread` anywhere in the files, not even in
  comments (`"[Locked by thread main]"` is fine: lowercase `thread`).
- The Wasm runner stops at the first failing fixture of each `@Test` method.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
initialState, typeChecks, readReentrancy: if FIX THE RUNTIME says `link a complete acyclic type hierarchy: java/util/concurrent/locks/ReentrantReadWriteLock` (or `$ReadLock`, `$WriteLock`, `ReadWriteLock`) or `link the descriptor class/interface ...`, a class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/locks/ReadWriteLock.java` and `javaw-agent-kit/bin/kit register java/util/concurrent/locks/ReentrantReadWriteLock.java`. If it names `Lock`, `InterruptedException` or `IllegalMonitorStateException`, T49 is incomplete: `kit block T50`. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
initialState, typeChecks: readLock()/writeLock() return the same two field objects every call; isFair returns the constructor flag; the outer class implements ReadWriteLock and Serializable, the nested classes implement Lock and Serializable; equals is not overridden.
readReentrancy, writeReentrancy: read holds and write holds are separate counters; each lock/tryLock adds one, each unlock removes one; getReadLockCount == getReadHoldCount == readHolds; isWriteLocked == isWriteLockedByCurrentThread == getHoldCount() != 0.
downgrade, upgradeFails, viaInterfaces: ReadLock always succeeds (also while writing); WriteLock.tryLock() returns false only when readHolds != 0 and writeHolds == 0, and true when this thread already holds the write lock even with reads held.
unlockFailures, independentLocks: ReadLock.unlock with readHolds == 0 throws IllegalMonitorStateException("attempt to unlock read lock, not locked by current thread"); WriteLock.unlock with writeHolds == 0 throws new IllegalMonitorStateException() with a null message; state is unchanged after a failure; each ReentrantReadWriteLock has its own counters (instance fields, not static).
readLimit, writeLimit: check `holds == 65535` before incrementing and throw new Error("Maximum lock count exceeded") from lock() and tryLock(); a write-locked lock at the limit still grants read locks.
interruptible: lockInterruptibly does exactly what lock() does on both locks (never throws InterruptedException).
toStrings: formats are super.toString()+"[Write locks = W, Read locks = R]", super.toString()+"[Read locks = R]", super.toString()+"[Unlocked]" or "[Locked by thread main]"; do not override hashCode or put anything between the hash and '['.
noSelfUse, protectedConstructors: no method may call readLock(), writeLock(), getReadLockCount(), getHoldCount(), lock(), tryLock(), unlock() or any other overridable method; read lock.readHolds/lock.writeHolds directly. The protected constructors must share the given lock's counters and throw NullPointerException for null.
leavesOutQueueQueries: this fails when hasQueuedThreads, getQueueLength, hasWaiters, newCondition or tryLock(long, TimeUnit) has become linked. Remove them.
```

## Docs

Create `docs/bytecode-reentrant-read-write-lock.md` with this skeleton (fill in the brackets):

    # Bytecode ReadWriteLock and ReentrantReadWriteLock

    Revision `reentrant-read-write-lock-vN` links `java.util.concurrent.locks.ReadWriteLock` and
    `ReentrantReadWriteLock` (with `ReadLock` and `WriteLock`) in the bundled exception runtime. This contract applies
    only to the single-threaded deployment, like `ReentrantLock`: two plain hold counters, no queue and no Java
    Memory Model ordering claimed.

    Contract:

    - <read/write hold counting and reentrancy; downgrade allowed; upgrade tryLock false; 65535 limit Error;
      IllegalMonitorStateException messages; query methods;
      toString formats; protected constructors share state; no self-use>

    Known differences: a blocking read-to-write upgrade `WriteLock.lock()` throws IllegalMonitorStateException instead
    of deadlocking; `newCondition()` and timed `tryLock` are not linked; `WriteLock.toString()` always
    names thread `main`; no `Sync`/AQS superclass; serialization is not linked.

    Evidence: `BytecodeReentrantReadWriteLockTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `hasQueuedThreads()`, `getQueueLength()`, `hasWaiters(Condition)`, `newCondition()` and timed `tryLock` are rejected
    before WAT generation.
