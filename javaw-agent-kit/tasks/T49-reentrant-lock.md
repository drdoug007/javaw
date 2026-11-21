# T49 Lock and ReentrantLock

```kit
id: T49
slug: reentrant-lock
title: link java.util.concurrent.locks.Lock and ReentrantLock (single-threaded)
tests: BytecodeReentrantLockTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/Lock.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReentrantLock.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/IllegalMonitorStateException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/InterruptedException.java
allow: docs/bytecode-reentrant-lock.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-reentrant-lock-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeReentrantLockTest.java`.

## Goal

Add `java.util.concurrent.locks.Lock` and `java.util.concurrent.locks.ReentrantLock` to the bundled runtime, plus
the two exception classes they need (`java.lang.IllegalMonitorStateException`, `java.lang.InterruptedException`;
neither exists yet). `java/util/concurrent/locks/Lock` is named as a type by many pinned classes
(`runtime-target/target/audit/external-types.tsv`: Tomcat `ContainerBase`, `StandardContext`, `AbstractProcessor`,
log4j `StatusLogger`, Jackson `PrivateMaxEntriesMap`, ByteBuddy `AgentBuilder$CircularityLock$Global`, ...). The
member rows (`member-references.tsv`) are dominated by `Lock.unlock()V` (509 call sites, e.g. Tomcat
`ContainerBase.getCluster()`) and `Lock.lock()V` (211, e.g. Hibernate `AbstractReadWriteAccess.get`), then
`Lock.tryLock()Z` (5, Spring `ConcurrentLruCache.drainOperations()`), `Lock.lockInterruptibly()V` (2, log4j
`ProviderUtil.lazyInit()`). `ReentrantLock` call sites: `unlock()V` (106), `lock()V` (58), `<init>()V` (46, e.g.
H2 `Session.<init>`, Tomcat `JNDIRealm.<init>`), `isHeldByCurrentThread()Z` (14), `<init>(Z)V` (4, logback
`OutputStreamAppender.<init>`, H2 `FileStore.<init>`), `tryLock()Z` (2), `getHoldCount()I` (1). (Counts are the
summed `count` column.)

The runtime is **single-threaded** (same contract as `docs/bytecode-atomic-integer-long.md`): there is only one
thread, so the lock is just an owner flag plus a hold count. It never blocks, the current thread always is the
owner when the lock is held, and nothing can interrupt. `java.lang.Thread` does not exist in the runtime and must
not be mentioned (`kit guard` rejects the word).

## Members to implement

Four files. No method of `ReentrantLock` is final except `isFair()` (the test subclasses it and overrides `lock`,
`tryLock`, `unlock`, `getHoldCount`, `isLocked`, `isHeldByCurrentThread` and `hashCode`).

`java/util/concurrent/locks/Lock.java`: `public interface Lock`

- `void lock();`, `void lockInterruptibly() throws InterruptedException;`, `boolean tryLock();`, `void unlock();`
  (nothing else; see Members to leave out)

`java/util/concurrent/locks/ReentrantLock.java`:
`public class ReentrantLock implements Lock, java.io.Serializable`

- `private final boolean fair;`, `private int holds;` (0 = unlocked)
- `public ReentrantLock()` (non-fair), `public ReentrantLock(boolean fair)`
- `public void lock()`, `public void lockInterruptibly() throws InterruptedException`, `public boolean tryLock()`,
  `public void unlock()`
- `public int getHoldCount()`, `public boolean isHeldByCurrentThread()`, `public boolean isLocked()`,
  `public final boolean isFair()`
- `public String toString()`

`java/lang/IllegalMonitorStateException.java`: `public class IllegalMonitorStateException extends RuntimeException`
with `public IllegalMonitorStateException()` and `public IllegalMonitorStateException(String s)`.

`java/lang/InterruptedException.java`: `public class InterruptedException extends Exception` with
`public InterruptedException()` and `public InterruptedException(String s)`.

## Members to leave out

- `newCondition()` and the `Condition` interface (later task). `Condition.await()` with one thread would wait
  forever (nobody can call `signal`), so its contract must be designed separately (for example: throw, or return
  at once like a spurious wakeup). Do not add `Condition` and do not declare `newCondition` in `Lock` or
  `ReentrantLock`. The test checks both stay unlinked.
- `tryLock(long, TimeUnit)` (in `Lock` and `ReentrantLock`): `java.util.concurrent.TimeUnit` is not in the runtime
  (later task together with `TimeUnit`). Checked by `leavesOutConditionsAndQueues`.
- Queue/owner inspection: `hasQueuedThreads()`, `hasQueuedThread(Thread)`, `getQueueLength()`,
  `hasWaiters(Condition)`, `getWaitQueueLength(Condition)`, `getOwner()`, `getQueuedThreads()`,
  `getWaitingThreads(Condition)` (they need `Thread` or `Condition`; the first and third are checked unlinked).
- The JDK `Sync`/`FairSync`/`NonfairSync` classes and `AbstractQueuedSynchronizer`: do not create them.
- `equals`/`hashCode`: not overridden (identity), like the JDK. Do not edit any other runtime file.

## JDK behaviour to match

- `lock()`: `holds++` (re-entrant; 5 `lock()` calls give `getHoldCount() == 5`). If the new count would overflow
  (`holds == Integer.MAX_VALUE`) throw `new Error("Maximum lock count exceeded")` and leave `holds` unchanged (not
  tested). `lock()` calls no other overridable method.
- `lockInterruptibly()`: exactly like `lock()` (nobody can interrupt, so it never throws `InterruptedException`).
  It must **not** call `lock()` (a subclass overriding `lock` sees nothing; `callPattern`).
- `tryLock()`: always succeeds here; same as `lock()` and returns `true`, also when the lock is already held and
  also for a fair lock (the JDK's untimed `tryLock` barges). Must not call `lock()`.
- `unlock()`: if `holds == 0` throw `new IllegalMonitorStateException()` (message `null`, cause `null`) and change
  nothing; otherwise `holds--`. Calls no other overridable method.
- `getHoldCount()` returns `holds`; `isLocked()` and `isHeldByCurrentThread()` return `holds != 0`; `isFair()`
  returns the constructor flag (`false` for `new ReentrantLock()`).
- `toString()`: `super.toString() + (holds == 0 ? "[Unlocked]" : "[Locked by thread main]")`. `super.toString()` is
  `Object.toString()`, i.e. `getClass().getName() + "@" + Integer.toHexString(hashCode())` and so calls the virtual
  `hashCode()` (a subclass returning `0x2a` prints `LoggingLock@2a[Unlocked]` and logs `hash;`). It reads `holds`
  directly: it must not call `isLocked()`, `getHoldCount()` or `isHeldByCurrentThread()`. The owner name is fixed
  to `main` (the only thread); the fixtures replace the name with `*`, so only the `[Locked by thread ` prefix and
  `]` are compared. Write the literal with lowercase `thread`.
- `IllegalMonitorStateException`/`InterruptedException`: `toString()` is the usual
  `java.lang.InterruptedException: stop`; no-arg constructors give a `null` message. `InterruptedException` is a
  checked exception (not a `RuntimeException`).

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeReentrantLockTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicLong.java` (same
   single-threaded style) and `java/lang/IllegalStateException.java` (exception class to copy).
3. Create `java/lang/IllegalMonitorStateException.java` and `java/lang/InterruptedException.java` under
   `javaw-bytecode/src/main/resources/exception-runtime/`.
4. Create `java/util/concurrent/locks/Lock.java` and `java/util/concurrent/locks/ReentrantLock.java` there.
5. Register all four (never edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/lang/IllegalMonitorStateException.java`,
   `javaw-agent-kit/bin/kit register java/lang/InterruptedException.java`,
   `javaw-agent-kit/bin/kit register java/util/concurrent/locks/Lock.java`,
   `javaw-agent-kit/bin/kit register java/util/concurrent/locks/ReentrantLock.java`.
6. `javaw-agent-kit/bin/kit check T49` until PASS.
7. Write `docs/bytecode-reentrant-lock.md` (below), the report, then `javaw-agent-kit/bin/kit finish T49`.

## Hints

- Do not copy the JDK source: it needs `AbstractQueuedSynchronizer`, `Thread`, `LockSupport` and `TimeUnit`. Each
  method is one or two lines.
- Do not write the lowercase words `volatile`, `synchronized`, `native` or the capitalised word `Thread` anywhere,
  not even in comments. The method name `isHeldByCurrentThread` is fine (the guard matches whole words only).
- `lockInterruptibly()` and `tryLock()` must change `holds` themselves, not call `lock()`.
- `toString()` must use `super.toString()` (not build the hash by hand) and read `holds` directly.
- `java.io.Serializable` exists in the runtime.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
basics, interruptibly, exceptionTypes, toStrings: if FIX THE RUNTIME says `link a complete acyclic type hierarchy` or `link the descriptor class/interface` naming ReentrantLock, Lock, IllegalMonitorStateException or InterruptedException, that class is missing or not registered: run `javaw-agent-kit/bin/kit register <path>` for each of the four files. If it says `link a matching instance method in the declaring hierarchy`, add the member exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
basics, reentrancy, tryLocking, lockGuard: lock and tryLock do holds++ (tryLock always returns true, even when already held or fair), unlock does holds--, getHoldCount returns holds, isLocked and isHeldByCurrentThread return holds != 0, isFair returns the constructor flag (default false).
unlockFailures: unlock with holds == 0 must throw `new IllegalMonitorStateException()` (no message) and leave holds at 0.
exceptionTypes: IllegalMonitorStateException extends RuntimeException and InterruptedException extends Exception; both need a no-arg and a String constructor passing the message to super.
interruptibly: lockInterruptibly behaves like lock (holds++) and never throws.
toStrings, callPattern: toString is `super.toString() + (holds == 0 ? "[Unlocked]" : "[Locked by thread main]")`; it must call super.toString() (which calls hashCode) and must not call isLocked/getHoldCount/isHeldByCurrentThread. lock, tryLock, lockInterruptibly and unlock must not call each other. Expected `lock;|tryLock;|LoggingLock@2a[Locked by thread *]hash;|unlock;unlock;||false|LoggingLock@2a[Locked by thread *]hash;|LoggingLock@2a[Unlocked]unlock;hash;`.
throughInterface: Lock must declare lock, lockInterruptibly, tryLock and unlock (abstract) so invokeinterface reaches both ReentrantLock and the fixture's `CountingLock` (a ReentrantLock subclass that overrides all four without calling super).
leavesOutConditionsAndQueues: this fails when newCondition, tryLock(long, TimeUnit), getQueueLength or hasQueuedThreads has become linked. Remove them from Lock/ReentrantLock and do not add Condition.
```

## Docs

Create `docs/bytecode-reentrant-lock.md` with this skeleton (fill in the brackets):

    # Bytecode ReentrantLock

    Revision `reentrant-lock-vN` links `java.util.concurrent.locks.Lock`, `ReentrantLock`,
    `java.lang.IllegalMonitorStateException` and `java.lang.InterruptedException` in the bundled exception runtime.
    This contract applies only to the single-threaded deployment, like `AtomicLong`: one plain hold count, the
    current thread is always the owner, nothing blocks and nothing interrupts.

    Contract:

    - <lock/tryLock/lockInterruptibly re-entrant hold count; tryLock always true; unlock with no hold throws
      IllegalMonitorStateException with a null message; isLocked/isHeldByCurrentThread/getHoldCount/isFair;
      toString suffix `[Unlocked]` / `[Locked by thread main]` after Object.toString>

    Known differences: there is no `AbstractQueuedSynchronizer`; the owner name in `toString` is always `main`.
    `newCondition`, `Condition`, `tryLock(long, TimeUnit)` and the queue inspection methods are not linked.

    Evidence: `BytecodeReentrantLockTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `newCondition`, timed `tryLock`, `getQueueLength` and `hasQueuedThreads` are rejected
    before WAT generation.
