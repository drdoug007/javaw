# T53 Timed tryLock, offer and poll

```kit
id: T53
slug: timed-lock-queue
title: link the timed tryLock/offer/poll members that take java.util.concurrent.TimeUnit (single-threaded)
tests: BytecodeTimedLockQueueTest
depends: T52
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/Lock.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReentrantLock.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReentrantReadWriteLock.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/BlockingQueue.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/LinkedBlockingQueue.java
allow: docs/bytecode-timed-lock-queue.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-timed-lock-queue-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeTimedLockQueueTest.java`.

## Goal

T49, T50 and T51 left out every member that takes a `java.util.concurrent.TimeUnit`, because `TimeUnit` was not in
the runtime. T52 adds `TimeUnit`; this task adds the five timed members to the existing lock and queue classes.
The pinned libraries call them (`runtime-target/target/audit/member-references.tsv`):
`Lock.tryLock(JLjava/util/concurrent/TimeUnit;)Z` (opcode 185) in ByteBuddy
`AgentBuilder$CircularityLock$Global.acquire()`, `ReentrantLock.tryLock(JLjava/util/concurrent/TimeUnit;)Z` (182) in
H2 `MVStore.tryExecuteUnderStoreLock`, `BlockingQueue.poll(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;` (185)
in Tomcat `ThreadPoolExecutor.getTask()`, `BlockingQueue.offer(Ljava/lang/Object;JLjava/util/concurrent/TimeUnit;)Z`
(185) in Tomcat `AsyncFileHandler$DropFlushPolicy.rejectedExecution`, and
`LinkedBlockingQueue.poll(JLjava/util/concurrent/TimeUnit;)Ljava/lang/Object;` (183, `super` call) in Tomcat
`TaskQueue.poll(long, TimeUnit)`. `ReentrantReadWriteLock$ReadLock`/`$WriteLock` have no timed rows, but once
`Lock` declares the abstract timed `tryLock` both classes must implement it.

**Single-threaded contract** (as in T49-T51): there is no other thread, so nothing can change the lock or queue
state while a timed call waits. Every timed call therefore returns at once with the result the JDK would return
after its timeout: it never waits, never sleeps and never reads a clock. The JVM test fixtures only use cases where
the JDK returns at once too, or after a 1 ns timeout.

## Members to implement

No new files. Add exactly these members (and nothing else) to the existing runtime files; leave every existing
member unchanged. Import nothing new except `java.util.concurrent.TimeUnit` in the `locks` files (or write the fully
qualified name). `BlockingQueue` and `LinkedBlockingQueue` are in the same package as `TimeUnit` (no import).

1. `java/util/concurrent/locks/Lock.java`: `boolean tryLock(long time, TimeUnit unit) throws InterruptedException;`
2. `java/util/concurrent/locks/ReentrantLock.java`:
   `public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException`
3. `java/util/concurrent/locks/ReentrantReadWriteLock.java`, in **both** nested classes `ReadLock` and `WriteLock`:
   `public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException`
4. `java/util/concurrent/BlockingQueue.java`:
   `boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException;` and
   `E poll(long timeout, TimeUnit unit) throws InterruptedException;`
5. `java/util/concurrent/LinkedBlockingQueue.java`:
   `public boolean offer(E e, long timeout, TimeUnit unit) throws InterruptedException` and
   `public E poll(long timeout, TimeUnit unit) throws InterruptedException`

## Members to leave out

- `newCondition()` and `Condition` (`Condition.await(long, TimeUnit)` needs its own single-threaded contract): still
  unlinked; the test checks `ReentrantLock.newCondition` and `Lock.newCondition`.
- Queue inspection (`getQueueLength`, `hasQueuedThreads`, ...) and `LinkedBlockingQueue.writeObject`: still
  unlinked (checked).
- Do not create or edit `TimeUnit.java` (T52 owns it) and do not edit any other runtime file. Do not add
  `TimeUnit.sleep`, `timedJoin` or anything that waits.

## JDK behaviour to match

Every timed member first evaluates `unit.toNanos(timeout)` (and ignores the result): a `null` unit throws
`NullPointerException` **before** anything else happens, even when the call would fail or succeed without waiting,
and the state stays unchanged. The fixtures print only the exception class (the JVM's helpful NPE message is not
compared). The timeout value is otherwise ignored: `0`, negative, `1` nanosecond, `Long.MAX_VALUE` days and
`Long.MIN_VALUE` all behave the same. No timed member throws `InterruptedException` here (nothing can interrupt),
but every one declares it.

- `ReentrantLock.tryLock(timeout, unit)`: after the unit check, exactly like the untimed `tryLock()`: the lock is
  always free or already held by the only thread, so it does `holds++` (with the same
  `Error("Maximum lock count exceeded")` guard) and returns `true`, also for a fair lock and when already held. It
  must **not** call `tryLock()`, `lock()`, `getHoldCount()`, `isLocked()` or `isHeldByCurrentThread()` (the
  `LoggingLock` subclass logs them; `lockCallPattern` expects no log). A subclass overriding the timed method is
  reached through `Lock` (`TimedOverride`).
- `ReadLock.tryLock(timeout, unit)`: after the unit check, like the untimed `ReadLock.tryLock()`: `readHolds++`
  (same `65535` guard), returns `true`, also while the write lock is held (downgrade). Must not call `lock()` or
  `tryLock()`.
- `WriteLock.tryLock(timeout, unit)`: after the unit check, like the untimed `WriteLock.tryLock()`: if
  `readHolds != 0 && writeHolds == 0` (upgrade: a reader asks for the write lock) return `false` and change nothing;
  the JDK would wait for the timeout and then return `false`, here it returns `false` at once. Otherwise
  `writeHolds++` (same `65535` guard; re-entrant also when read holds exist on top of the write lock) and `true`.
  Must not call `lock()`, `tryLock()`, `getHoldCount()` or `isHeldByCurrentThread()`.
- `LinkedBlockingQueue.offer(e, timeout, unit)`: in this order: `e == null` throws `new NullPointerException()`
  (so a `null` element with a `null` unit is still an NPE); then `unit.toNanos(timeout)`; then if `count == capacity`
  return `false` (the JDK waits for the timeout and returns `false`; nobody can take an element); else
  `enqueue(new Node<E>(e)); count++;` and `true`. Must **not** call `offer(e)`, `put`, `size()` or
  `remainingCapacity()` (the `Spy` subclass logs them; `queueCallPattern` expects no log).
- `LinkedBlockingQueue.poll(timeout, unit)`: first `unit.toNanos(timeout)` (so a `null` unit throws even when the
  queue is not empty, and nothing is removed); then if `count == 0` return `null` (the JDK waits and returns
  `null`); else `count--` and return `dequeue()`. Must **not** call `poll()`, `take()`, `peek()`, `size()` or
  `isEmpty()`. A weakly consistent iterator still returns the item it already fetched after a timed `poll` removed
  it (same as untimed `poll`, since both use `dequeue`).

## Steps

1. Read the test, then the five runtime files above and `java/util/concurrent/TimeUnit.java` (from T52; check that
   `toNanos(long)` exists).
2. Add the members listed under Members to implement. Copy the body of each untimed sibling (`tryLock()`, `offer(E)`,
   `poll()`) and put the `unit.toNanos(timeout);` statement in the right place.
3. Nothing to register: all five files are already registered (`TimeUnit` by T52).
4. `javaw-agent-kit/bin/kit check T53` until PASS.
5. Write `docs/bytecode-timed-lock-queue.md` (below), the report, then `javaw-agent-kit/bin/kit finish T53`.

## Hints

- Do not write `volatile`, `synchronized`, `native`, `Thread`, `System.nanoTime` or `currentTimeMillis` anywhere,
  not even in comments. Do not loop or wait on the timeout.
- `throws InterruptedException` must be on the interface methods (`Lock`, `BlockingQueue`) or the fixture's
  overrides and `catch` blocks do not match the JDK. On the class methods keep it too (the JDK has it).
- For `offer`, the null-element check comes **before** the unit check; for the locks and `poll` the unit check is
  first. `Objects.requireNonNull(unit)` would also throw an NPE, but `unit.toNanos(timeout)` is what the JDK does.
- Calibration mistakes: writing `return tryLock();` / `return offer(e);` / `return poll();` in the timed member
  (the subclass log then shows `tryLock;`, `offer:` or `poll:`); checking the unit after the fullness/emptiness
  test (then a `null` unit on a full queue returns `false` instead of throwing); making `WriteLock`'s timed
  `tryLock` throw like `lock()` in the upgrade case instead of returning `false`; forgetting one of `ReadLock` or
  `WriteLock` (`link a matching instance method`).
- Existing finished tests of T49/T50/T51 checked that these timed members stayed unlinked; those checks were removed
  when this task was queued. If `kit finish` reports one of them failing, stop and `kit block` (do not edit tests).
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
timedReentrantLock, timedReadWriteLock, timedQueueOperations: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/TimeUnit` or `link a complete acyclic type hierarchy` naming TimeUnit, T52 is not finished: run `javaw-agent-kit/bin/kit block T53`. If it says `link a matching instance method in the declaring hierarchy` naming tryLock, offer or poll with a `TimeUnit` descriptor, add that member exactly as listed under Members to implement (ReadLock and WriteLock each need their own). If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes (often a missing `throws InterruptedException` or a missing import of TimeUnit in the locks package).
lockTimed: the timed ReentrantLock.tryLock first calls unit.toNanos(timeout) (null unit -> NullPointerException, holds unchanged), then holds++ and returns true for every timeout, fair or not, held or not.
lockCallPattern: the timed tryLock must not call tryLock(), lock(), getHoldCount(), isLocked() or isHeldByCurrentThread(); Lock must declare `boolean tryLock(long time, TimeUnit unit) throws InterruptedException` so invokeinterface reaches the TimedOverride subclass.
readWriteTimed: ReadLock timed tryLock: unit check, then readHolds++ and true (also while the write lock is held). WriteLock timed tryLock: unit check, then false without change when readHolds != 0 && writeHolds == 0, else writeHolds++ and true. Never throw in the upgrade case.
readWriteCallPattern: ReadLock/WriteLock timed tryLock must not call lock(), tryLock(), getHoldCount() or isHeldByCurrentThread() on itself.
queueTimed: offer(e, t, u) checks e == null first, then unit.toNanos(timeout), then returns false when count == capacity; poll(t, u) calls unit.toNanos(timeout) first (null unit throws even when not empty and removes nothing), then returns null when empty, else count-- and dequeue().
queueCallPattern: timed offer must not call offer(e), put, size or remainingCapacity; timed poll must not call poll(), take, peek, size or isEmpty. BlockingQueue must declare both timed members (with throws InterruptedException) so invokeinterface reaches the TimedQueue subclass.
leavesOutConditionsAndSerialization: this fails when newCondition, getQueueLength, hasQueuedThreads or LinkedBlockingQueue.writeObject became linked. Add only the five timed members.
```

## Docs

Create `docs/bytecode-timed-lock-queue.md` with this skeleton (fill in the brackets):

    # Bytecode timed lock and queue operations

    Revision `timed-lock-queue-vN` links the `TimeUnit` members `Lock.tryLock(long, TimeUnit)`,
    `ReentrantLock.tryLock(long, TimeUnit)`, `ReentrantReadWriteLock.ReadLock/WriteLock.tryLock(long, TimeUnit)`,
    `BlockingQueue`/`LinkedBlockingQueue.offer(E, long, TimeUnit)` and `poll(long, TimeUnit)` in the bundled
    exception runtime under the single-threaded contract of `bytecode-reentrant-lock.md`,
    `bytecode-reentrant-read-write-lock.md` and `bytecode-linked-blocking-queue.md`.

    Contract:

    - <timed calls never wait: no other thread can change the state, so they return the JDK's result after the
      timeout at once; null unit -> NullPointerException first (after the null element for offer); tryLock true
      as untimed, WriteLock false on upgrade; offer false when full; poll null when empty; no self-calls>

    Known differences: no clock is read and no time passes; the JDK would wait for the timeout in the failing
    cases (write-lock upgrade, full queue, empty queue). Conditions and serialization are still not linked.

    Evidence: `BytecodeTimedLockQueueTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `newCondition`, `getQueueLength`, `hasQueuedThreads` and
    `LinkedBlockingQueue.writeObject` are rejected before WAT generation.
