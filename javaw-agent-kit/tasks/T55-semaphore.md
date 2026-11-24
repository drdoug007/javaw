# T55 Semaphore

```kit
id: T55
slug: semaphore
title: link java.util.concurrent.Semaphore (single-threaded)
tests: BytecodeSemaphoreTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/Semaphore.java
allow: docs/bytecode-semaphore.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-semaphore-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeSemaphoreTest.java`.

## Goal

Add `java.util.concurrent.Semaphore` to the bundled runtime under the single-threaded contract (Wasm runs one
thread; no `AbstractQueuedSynchronizer`, no atomics, nothing blocks). `runtime-target/target/audit/member-references.tsv`
lists the uses. Construction: `<init>(I)V` in Tomcat `StuckThreadDetectionValve$MonitoredThread.<init>` and
websocket `WsRemoteEndpointImplBase.<init>`; `<init>(IZ)V` in HikariCP `SuspendResumeLock.<init>(Z)` and Tomcat
`PersistentValve$UsageCountingSemaphore.<init>(Z)`. Calls (opcode 182): `release()V` 23 sites in Tomcat (plus 3 in
websocket `WsRemoteEndpointImplBase.endMessage` and HikariCP `SuspendResumeLock.release`), `tryAcquire()Z` 13 in
Tomcat (`UsageCountingSemaphore.tryAcquire`), 2 in websocket (`startMessage`) and HikariCP `SuspendResumeLock.acquire`,
`acquire()V` 5 (`UsageCountingSemaphore.acquire`), `availablePermits()I` 5 (Tomcat
`Nio2Endpoint$Nio2SocketWrapper.fillReadBuffer`), `acquireUninterruptibly()V` 2 in Tomcat and 1 in HikariCP
(`SuspendResumeLock.acquire`), `acquireUninterruptibly(I)V` (HikariCP `SuspendResumeLock.suspend`) and `release(I)V`
(HikariCP `SuspendResumeLock.resume`). The timed `tryAcquire(JLjava/util/concurrent/TimeUnit;)Z` rows (Tomcat
`Nio2SocketWrapper.flushBlocking`, websocket `acquireMessagePartInProgressSemaphore`) need
`java.util.concurrent.TimeUnit`, which is not in the runtime yet: they are a later task.

Copy the JDK 21 *observable* behaviour: a plain signed permit count (negative counts are allowed), the
`IllegalArgumentException` checks on negative arguments, the overflow/underflow `Error`s, `drainPermits` on a
negative count, the `toString` suffix, and **no** calls to overridable methods of `this` (the test logs a subclass).

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/Semaphore.java`
(`package java.util.concurrent;`): `public class Semaphore implements java.io.Serializable` with

- fields `private int permits;` and `private final boolean fair;` (no `Sync` inner class)
- `public Semaphore(int permits)`
- `public Semaphore(int permits, boolean fair)`
- `public void acquire() throws InterruptedException`
- `public void acquire(int permits) throws InterruptedException`
- `public void acquireUninterruptibly()`
- `public void acquireUninterruptibly(int permits)`
- `public boolean tryAcquire()`
- `public boolean tryAcquire(int permits)`
- `public void release()`
- `public void release(int permits)`
- `public int availablePermits()`
- `public int drainPermits()`
- `protected void reducePermits(int reduction)`
- `public boolean isFair()`
- `public String toString()`

Private helpers are fine (for example one "take n or fail", one "try take n", one "give n").
`java.lang.InterruptedException`, `java.lang.Error` and `java.io.Serializable` already exist; do not create them.

## Members to leave out

- Timed `tryAcquire(long, TimeUnit)` and `tryAcquire(int, long, TimeUnit)`: `TimeUnit` is not in the runtime and a
  positive timeout would have to sleep. Do not create `TimeUnit.java`.
- Queue inspection `hasQueuedThreads()`, `getQueueLength()` and `getQueuedThreads()`: there are never queued
  threads, and `getQueuedThreads` returns `Collection<Thread>`. The test checks that all five stay unlinked.
- The JDK internals: `Sync`, `NonfairSync`, `FairSync`, `AbstractQueuedSynchronizer`. No `synchronized`, no
  `volatile`, no `Thread`.
- Do not edit any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

- Constructors store the count as given, **including negative values and `Integer.MIN_VALUE`** (no check).
  `Semaphore(int)` is not fair; `isFair()` returns the constructor flag. Fairness changes nothing observable here:
  a fair `tryAcquire` still succeeds when permits are available.
- `tryAcquire(n)`: `n < 0` throws `new IllegalArgumentException()` (message `null`). Otherwise
  `remaining = permits - n`; if `remaining < 0` return `false` and change nothing, else `permits = remaining` and
  return `true`. So `tryAcquire(0)` is `true` on a zero count and `false` on a negative count. `tryAcquire()` is
  the same with `n = 1`.
- `acquire(n)` / `acquireUninterruptibly(n)`: `n < 0` throws `new IllegalArgumentException()`. If
  `permits - n >= 0`, take the permits exactly like `tryAcquire`. Otherwise the JDK parks until another thread
  releases; this runtime has no other thread (same contract as `LinkedBlockingQueue.take`, see
  `docs/bytecode-linked-blocking-queue.md`), so throw
  `new IllegalStateException("Semaphore.acquire would block forever: not enough permits and no other thread")`
  (use `acquireUninterruptibly` in the message for those two methods) and change nothing. The test never
  reaches that path (it cannot be compared with the JVM). `acquire` never throws `InterruptedException` here but
  keeps the `throws` clause.
- `release(n)`: `n < 0` throws `new IllegalArgumentException()`. `next = permits + n`; if `next < permits`
  (int overflow) throw `new Error("Maximum permit count exceeded")` and change nothing, else `permits = next`.
  `release(0)` on `Integer.MAX_VALUE` is fine. Releasing more than was acquired is allowed (no upper bound other
  than overflow).
- `reducePermits(r)`: `r < 0` throws `new IllegalArgumentException()`. `next = permits - r`; if `next > permits`
  (underflow) throw `new Error("Permit count underflow")` and change nothing, else `permits = next` (may go
  negative).
- `availablePermits()`: `permits`. `drainPermits()`: return the current count and set it to `0` — **also when it
  is negative** (JDK 21 returns e.g. `-4` and leaves `0`).
- `toString()`: `super.toString() + "[Permits = " + permits + "]"` (Object's `toString`, which calls the
  overridable `hashCode()`; the test strips the hash).
- Overridable self-calls: none. `acquire()` must not call `acquire(1)`, `release()` must not call `release(1)`,
  `tryAcquire()` must not call `tryAcquire(1)`, `toString()` must read the field, not call `availablePermits()`,
  and `drainPermits`/`reducePermits` call nothing. The `Spy` subclass logs every public method.

## Steps

1. Read the test, then `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReentrantLock.java`
   (same single-threaded style).
2. Create `java/util/concurrent/Semaphore.java` under `javaw-bytecode/src/main/resources/exception-runtime/` with
   the members above.
3. `javaw-agent-kit/bin/kit register java/util/concurrent/Semaphore.java` (never edit
   `BytecodeExceptionRuntime.java` by hand).
4. `javaw-agent-kit/bin/kit check T55` until PASS.
5. Write `docs/bytecode-semaphore.md` (below), the report, then `javaw-agent-kit/bin/kit finish T55`.

## Hints

- Do not copy the JDK source: it needs `AbstractQueuedSynchronizer` and `TimeUnit`. Each method is a few lines.
- Do not write the lowercase words `volatile`, `synchronized`, `native` or the capitalised word `Thread` anywhere,
  not even in comments.
- The method parameters are named `permits` like the field: write `this.permits` in the constructor, or name the
  field/parameters differently (only the signatures matter).
- Detect overflow with `next < permits` and underflow with `next > permits` after plain `int` arithmetic; do not use
  `Math.addExact` (wrong exception type) or `long`.
- Check `n < 0` first in every `(int)` variant, before looking at the count.
- Calibration found no runtime pitfalls (the reference passed on the first compiling run); the only fix was in the
  fixture. If a fixture fails, compare the count after each step with the rules above.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
basics, acquiring, tryAcquiring, guard, negativeArguments, releasing, drainingAndReducing, toStrings, selfCalls: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/Semaphore` or `link a complete acyclic type hierarchy`, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/Semaphore.java`. If it says `link a matching instance method in the declaring hierarchy`, add the member exactly as listed under Members to implement (note `reducePermits` is protected). If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
basics: the constructors store any int, including negatives and Integer.MIN_VALUE; isFair returns the flag (false for the one-argument constructor).
acquiring, tryAcquiring, guard: take n permits when permits - n >= 0; tryAcquire returns false and changes nothing otherwise; tryAcquire(0) is false on a negative count; a fair semaphore behaves the same.
negativeArguments: every (int) variant and reducePermits throw `new IllegalArgumentException()` (null message) for a negative argument before touching the count.
releasing: release adds with plain int arithmetic and throws `new Error("Maximum permit count exceeded")` when the sum wraps (next < permits), leaving the count unchanged; negative counts can be released upwards.
drainingAndReducing: drainPermits returns the count and sets 0 even when negative; reducePermits may go negative and throws `new Error("Permit count underflow")` when next > permits.
toStrings: toString is `super.toString() + "[Permits = " + permits + "]"`, reading the field (the Spy log must show only `hash;`).
selfCalls: no public method may call another overridable method of this: acquire() is not acquire(1), release() is not release(1), tryAcquire() is not tryAcquire(1).
leavesOutTimedAcquireAndQueues: this fails when a timed tryAcquire, hasQueuedThreads, getQueueLength or getQueuedThreads became linked. Remove them and do not add TimeUnit.
```

## Docs

Create `docs/bytecode-semaphore.md` with this skeleton (fill in the brackets):

    # Bytecode Semaphore

    Revision `semaphore-vN` links `java.util.concurrent.Semaphore` in the bundled exception runtime under the
    single-threaded contract: one plain signed permit count, nothing blocks and nothing interrupts.

    Contract:

    - <constructors (any int, fair flag); acquire/acquireUninterruptibly/tryAcquire with and without a count;
      IllegalArgumentException on negative arguments; release with the overflow Error; availablePermits,
      drainPermits (also negative), reducePermits with the underflow Error; isFair; toString suffix; no
      overridable self-calls>

    Known differences: there is no `AbstractQueuedSynchronizer`. `acquire`/`acquireUninterruptibly` that would
    have to wait throw IllegalStateException instead of blocking forever; no thread could ever release.
    Timed `tryAcquire` (needs TimeUnit) and the queue inspection methods are not linked.

    Evidence: `BytecodeSemaphoreTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). The timed `tryAcquire` overloads, `hasQueuedThreads`, `getQueueLength` and
    `getQueuedThreads` are rejected before WAT generation.
