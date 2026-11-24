# T54 CountDownLatch

```kit
id: T54
slug: count-down-latch
title: link java.util.concurrent.CountDownLatch (single-threaded)
tests: BytecodeCountDownLatchTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CountDownLatch.java
allow: docs/bytecode-count-down-latch.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-count-down-latch-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeCountDownLatchTest.java`.

## Goal

Add `java.util.concurrent.CountDownLatch` to the bundled runtime under the single-threaded contract (Wasm runs one
thread; no `AbstractQueuedSynchronizer`, no locks, no atomics). `runtime-target/target/audit/member-references.tsv`
lists the uses. Construction `<init>(I)V`: Tomcat `Acceptor.<init>` (3), Tomcat websocket
`AsyncChannelWrapperSecure$WrapperFuture.<init>` (2), Spring `DefaultLifecycleProcessor$LifecycleGroup.stop` (2),
Spring Boot `BackgroundPreinitializer.<clinit>` and `GracefulShutdown.shutDownGracefully`, Spring beans
`DisposableBeanAdapter$ReactiveDestroyMethodHandler.await`. `countDown()V`: Spring `LifecycleGroup.stop` (6), Tomcat
`Acceptor.run` (4), `WrapperFuture.complete` (3), `GracefulShutdown.doShutdown` (3), `BackgroundPreinitializer$1.run`
(2), `DisposableBeanAdapter$DestroyMethodSubscriber.onComplete` (2), logback `SimpleSocketServer.signalAlmostReadiness`.
`getCount()J`: `WrapperFuture.isDone` (2), Tomcat `VirtualThreadExecutor.isShutdown`, Spring `LifecycleGroup.stop`,
logback `SimpleSocketServer.signalAlmostReadiness`. `await()V`: `WrapperFuture.get` (2),
`BackgroundPreinitializer.onApplicationEvent`, `GracefulShutdown.shutDownGracefully`,
`ScheduledAnnotationReactiveSupport$SubscribingRunnable.run`, `ReactiveDestroyMethodHandler.await`. The timed
`await(JLjava/util/concurrent/TimeUnit;)Z` rows (Tomcat `Acceptor.stop` 3, `WrapperFuture.get(long,TimeUnit)` 2,
Spring `LifecycleGroup.stop`) need `java.util.concurrent.TimeUnit`, which is not in the runtime yet: a later task.

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CountDownLatch.java`
(`package java.util.concurrent;`): `public class CountDownLatch` (extends `Object`, implements nothing) with

- field `private int count;`
- `public CountDownLatch(int count)`
- `public void await() throws InterruptedException`
- `public void countDown()`
- `public long getCount()`
- `public String toString()`

`java.lang.InterruptedException` already exists in the runtime (T49); do not create or register it again.

## Members to leave out

- `await(long, TimeUnit)`: `TimeUnit` is not in the runtime, and a positive timeout would have to sleep. The test
  checks that `CountDownLatch.await(JLjava/util/concurrent/TimeUnit;)Z` stays unlinked. Do not create `TimeUnit.java`.
- The JDK internals: the `Sync` inner class, `AbstractQueuedSynchronizer`, `serialVersionUID`. CountDownLatch is not
  `Serializable` (the test checks `instanceof java.io.Serializable` is `false`). No `synchronized`, no `volatile`, no
  `Thread`.
- Do not override `equals` or `hashCode` (identity, inherited from `Object`).

## JDK behaviour to match

- Constructor: `count < 0` throws `new IllegalArgumentException("count < 0")` (no cause), also for
  `Integer.MIN_VALUE`. `0` is allowed.
- `countDown()`: if `count > 0`, `count--`; at `0` it does nothing (never goes negative).
- `getCount()`: returns `count` widened to `long`.
- `await()`: returns immediately when `count == 0` (any number of times). When `count > 0` the JDK waits forever for
  another thread; this runtime has no other thread, so throw
  `new IllegalStateException("CountDownLatch.await would block forever: count is " + count + " and no other thread")`
  (same convention as `LinkedBlockingQueue.take`, see `docs/bytecode-linked-blocking-queue.md`). The test never
  reaches that path (the JVM would hang). It never throws `InterruptedException`, but must declare it.
- `toString()`: `super.toString() + "[Count = " + count + "]"`. It must call `super.toString()` (Object's, which
  calls the virtual `hashCode()`; the `Spy` fixture logs `hash;`) and read the field directly, **not** call
  `getCount()`.
- Overridable self-calls: none. `countDown`, `await`, `getCount` and `toString` must not call each other (the `Spy`
  subclass logs `countDown`, `getCount`, `await` and `hashCode`).

## Steps

1. Read the test, then `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/locks/ReentrantLock.java`
   (same single-threaded style, `toString` via `super.toString()`).
2. Create `java/util/concurrent/CountDownLatch.java` under `javaw-bytecode/src/main/resources/exception-runtime/`
   with the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/concurrent/CountDownLatch.java`
4. `javaw-agent-kit/bin/kit check T54` until PASS.
5. Write `docs/bytecode-count-down-latch.md` (below), the report, then `javaw-agent-kit/bin/kit finish T54`.

## Hints

- Do not copy the JDK source: it needs `AbstractQueuedSynchronizer`, `Thread` and `TimeUnit`. Each method is one to
  three lines with a plain `int count` field.
- Do not write the lowercase words `volatile`, `synchronized`, `native` or the capitalised word `Thread` anywhere,
  not even in comments.
- `getCount()` returns `long`, the field and constructor parameter are `int`.
- Easy calibration mistakes: `toString()` calling `getCount()` (the Spy log shows `getCount;`), building the hash by
  hand instead of `super.toString()`, `countDown()` going below 0, a message other than `count < 0`.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructors, countingDown, awaiting, toStrings, subclassCalls: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/CountDownLatch` or names one of its methods, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/concurrent/CountDownLatch.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constructors: count < 0 throws `new IllegalArgumentException("count < 0")`; 0 and Integer.MAX_VALUE are allowed; getCount returns long.
countingDown: countDown decrements only while count > 0 and never goes negative.
awaiting: await returns immediately when count == 0; it must not throw for a zero count.
toStrings, subclassCalls: toString is `super.toString() + "[Count = " + count + "]"`, reading the field; it must not call getCount. countDown, getCount and await must not call each other. Do not implement Serializable.
```

## Docs

Create `docs/bytecode-count-down-latch.md` with this skeleton (fill in the brackets):

    # Bytecode CountDownLatch

    Revision `count-down-latch-vN` links `java.util.concurrent.CountDownLatch` in the bundled exception runtime
    under the single-threaded contract.

    Contract:

    - <constructor with IllegalArgumentException "count < 0"; countDown stops at 0; getCount as long; await returns
      at 0; toString `Object.toString()[Count = n]`; no overridable self-calls>

    Known differences: no `AbstractQueuedSynchronizer` (the selected deployment is single-threaded). `await()` with a positive count
    throws IllegalStateException instead of blocking forever; no thread could ever count down. `await(long,
    TimeUnit)` (needs TimeUnit) is not linked.

    Evidence: `BytecodeCountDownLatchTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `CountDownLatch.await(long, TimeUnit)` is rejected before WAT generation.
