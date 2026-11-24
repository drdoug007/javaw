# T64 Timed CountDownLatch.await and Semaphore.tryAcquire

```kit
id: T64
slug: timed-latch-semaphore
title: link the timed CountDownLatch.await and Semaphore.tryAcquire members that take java.util.concurrent.TimeUnit (single-threaded)
tests: BytecodeTimedLatchSemaphoreTest
depends: T52, T54, T55
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CountDownLatch.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/Semaphore.java
allow: docs/bytecode-timed-latch-semaphore.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-timed-latch-semaphore-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeTimedLatchSemaphoreTest.java`.

## Goal

T54 (`CountDownLatch`) and T55 (`Semaphore`) left out the members that take a `java.util.concurrent.TimeUnit`.
T52 added `TimeUnit` and T53 set the contract for timed calls (`docs/bytecode-timed-lock-queue.md`). This task adds
the timed members to the latch and the semaphore. The pinned libraries call them
(`runtime-target/target/audit/member-references.tsv`, opcode 182):
`CountDownLatch.await(JLjava/util/concurrent/TimeUnit;)Z` 3 sites in Tomcat `org/apache/tomcat/util/net/Acceptor.stop(I)V`,
2 in tomcat-embed-websocket `AsyncChannelWrapperSecure$WrapperFuture.get(JLjava/util/concurrent/TimeUnit;)` and 1 in
spring-context `DefaultLifecycleProcessor$LifecycleGroup.stop()V`;
`Semaphore.tryAcquire(JLjava/util/concurrent/TimeUnit;)Z` 3 sites in Tomcat
`Nio2Endpoint$Nio2SocketWrapper.flushBlocking()V` and 1 in websocket
`WsRemoteEndpointImplBase.acquireMessagePartInProgressSemaphore(BJ)Z`. `Semaphore.tryAcquire(IJLjava/util/concurrent/TimeUnit;)Z`
has no audit row; it is added for coherence (the untimed `tryAcquire(int)` is linked).

**Single-threaded contract** (as in T53): no other thread exists, so nothing can count the latch down or release a
permit while a timed call waits. Every timed call returns at once with the result the JDK returns after its timeout:
it never waits, never sleeps and never reads a clock. The JVM fixtures only use cases where the JDK returns at once,
or after a timeout of 0 or a few nanoseconds.

## Members to implement

No new files, nothing to register. Add exactly these three members to the existing runtime files and leave every
existing member unchanged. Both files are in package `java.util.concurrent`, like `TimeUnit` (no import).

1. `java/util/concurrent/CountDownLatch.java`:
   `public boolean await(long timeout, TimeUnit unit) throws InterruptedException`
2. `java/util/concurrent/Semaphore.java`:
   `public boolean tryAcquire(long timeout, TimeUnit unit) throws InterruptedException` and
   `public boolean tryAcquire(int permits, long timeout, TimeUnit unit) throws InterruptedException`

## Members to leave out

- `Semaphore.hasQueuedThreads`, `getQueueLength`, `getQueuedThreads` (there are never queued threads, and
  `getQueuedThreads` needs `Thread`): still unlinked, checked by the test.
- Do not edit `TimeUnit.java` (T52 owns it; `TimeUnit.sleep` stays unlinked, checked) or any other runtime file.

## JDK behaviour to match

- `CountDownLatch.await(timeout, unit)`: first `unit.toNanos(timeout);` (result ignored), so a `null` unit throws
  `NullPointerException` also when the count is already 0. Then `return count == 0;`. Never throws
  `IllegalStateException` (unlike the untimed `await()`, which would block forever): with count > 0 the JDK waits for
  the timeout and returns `false`. The count never changes. The timeout value is otherwise ignored (0, negative,
  `Long.MIN_VALUE`, `Long.MAX_VALUE` days all behave the same).
- `Semaphore.tryAcquire(timeout, unit)`: first `unit.toNanos(timeout);` (a `null` unit throws an NPE even when
  permits are available, and no permit is taken), then exactly like untimed `tryAcquire()`: take one permit if
  `permits - 1 >= 0` and return `true`, else return `false` and change nothing. Fair and non-fair behave the same.
- `Semaphore.tryAcquire(permits, timeout, unit)`: in this order: `permits < 0` throws
  `new IllegalArgumentException()` (message `null`; also with a `null` unit); then `unit.toNanos(timeout)`; then like
  `tryAcquire(int)`. `tryAcquire(0, ...)` on a negative count returns `false`.
- None of the three calls an overridable method of `this`: not `await()`, `countDown()`, `getCount()`,
  `tryAcquire()`, `tryAcquire(int)`, `acquire*`, `availablePermits()` or `isFair()` (the `LatchSpy`/`SemSpy`
  subclasses log them; the call-pattern fixtures expect an empty log). Use the private helpers/fields directly.
- No timed member throws `InterruptedException` here, but each declares it (the fixtures catch it and override the
  timed members in `TimedLatch`/`TimedSem`).

## Steps

1. Read the test, `CountDownLatch.java`, `Semaphore.java` and `TimeUnit.java` (check `toNanos(long)` exists).
2. Add `await(long, TimeUnit)` to `CountDownLatch`: `unit.toNanos(timeout); return count == 0;`.
3. Add the two `tryAcquire` overloads to `Semaphore`, reusing the private `checkNonNegative` and `tryTake` helpers:
   `unit.toNanos(timeout); return tryTake(1);` and
   `checkNonNegative(permits); unit.toNanos(timeout); return tryTake(permits);`.
4. Nothing to register.
5. `javaw-agent-kit/bin/kit check T64` until PASS.
6. Write `docs/bytecode-timed-latch-semaphore.md` (below), the report, then `javaw-agent-kit/bin/kit finish T64`.

## Hints

- Do not write `volatile`, `synchronized`, `native`, `Thread`, `System.nanoTime` or `currentTimeMillis` anywhere,
  not even in comments. Do not loop or wait on the timeout.
- Do not copy the untimed `await()` body: it throws `IllegalStateException` when the count is positive; the timed
  `await` returns `false` instead.
- Do not write `return tryAcquire();` / `return tryAcquire(permits);` in the timed members: the `SemSpy` log then
  shows `try;` or `try:2;`. Call the private `tryTake` helper.
- The unit check goes before the permit/count test (a `null` unit with enough permits must throw and leave the
  permits unchanged), but after the negative-permits check in the three-argument overload.
- The finished T54 and T55 tests contained `rejectsUnusedCall` checks for these members; they were removed when
  this task was queued. If `kit finish` reports one of them failing, stop and `kit block` (do not edit tests).
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
latchTimed, semaphoreTimed, timedGuard: if FIX THE RUNTIME says `link a matching instance method in the declaring hierarchy` naming await or tryAcquire with a `TimeUnit` descriptor, add that member exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes (often a missing `throws InterruptedException`).
latchTimed: timed await returns count == 0 and never throws IllegalStateException; it does not change the count.
latchNullUnit: timed await must call unit.toNanos(timeout) first, so a null unit throws NullPointerException even when the count is 0.
latchCallPattern: timed await must not call await(), countDown() or getCount(); read the count field directly.
semaphoreArguments: tryAcquire(permits, t, u) throws new IllegalArgumentException() for permits < 0 before touching the unit; then unit.toNanos(timeout) (null unit -> NullPointerException, permits unchanged) before the permit test.
semaphoreTimed: after the unit check the timed tryAcquire is exactly tryTake(1) / tryTake(permits): false without change when permits - n < 0, including tryAcquire(0, ...) on a negative count.
semaphoreCallPattern: the timed tryAcquire members must not call tryAcquire(), tryAcquire(int), availablePermits() or isFair(); use the private tryTake helper.
leavesOutQueuesAndThreads: this fails when hasQueuedThreads, getQueueLength, getQueuedThreads or TimeUnit.sleep became linked. Add only the three timed members.
```

## Docs

Create `docs/bytecode-timed-latch-semaphore.md` with this skeleton (fill in the brackets):

    # Bytecode timed latch and semaphore operations

    Revision `timed-latch-semaphore-vN` links `CountDownLatch.await(long, TimeUnit)`,
    `Semaphore.tryAcquire(long, TimeUnit)` and `Semaphore.tryAcquire(int, long, TimeUnit)` in the bundled exception
    runtime under the single-threaded contract of `bytecode-timed-lock-queue.md`.

    Contract:

    - <timed calls never wait; null unit -> NullPointerException first (after the negative-permits
      IllegalArgumentException); await true iff count is 0; tryAcquire true and takes permits when enough, else
      false; no self-calls>

    Known differences: no clock is read and no time passes; the JDK would wait for the timeout in the failing
    cases (positive count, not enough permits). Queue inspection is still not linked.

    Evidence: `BytecodeTimedLatchSemaphoreTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `hasQueuedThreads`, `getQueueLength`, `getQueuedThreads` and `TimeUnit.sleep`
    are rejected before WAT generation.
