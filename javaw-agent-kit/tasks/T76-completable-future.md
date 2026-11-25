# T76 CompletableFuture single-threaded core

```kit
id: T76
slug: completable-future
title: link java.util.concurrent.CompletableFuture, CompletionStage and CompletionException (single-threaded core)
tests: BytecodeCompletableFutureTest
depends: T75
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CompletionException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CompletionStage.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CompletableFuture.java
allow: docs/bytecode-completable-future.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-completable-future-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeCompletableFutureTest.java`.

## Goal

Add `java.util.concurrent.CompletionException`, the `java.util.concurrent.CompletionStage` interface and
`java.util.concurrent.CompletableFuture` to the bundled runtime under the single-threaded contract (Wasm runs one
thread: no executor, no blocking, no time). T75 already added `Future`, `ExecutionException`,
`CancellationException` and `TimeoutException`; use them, do not change them.

`runtime-target/target/audit/member-references.tsv` has 30 rows in 7 artifacts. Construction:
`CompletableFuture.<init>()V` in Spring `AsyncResult.completable()` and `ReactiveAdapterRegistry$EmptyCompletableFuture`
(5 sites), `completedFuture(Object)` in Hibernate `EventListenerGroupImpl.<clinit>`, Spring Data
`QueryExecutionConverters$NullableWrapperToCompletableFutureConverter` and Spring `ConcurrentMapCache.retrieve` (6).
Completion: `complete(Object)` (method references in Spring `ListenableFuture.completable()`),
`completeExceptionally(Throwable)` (Spring `AsyncResult.completable()`, `FutureUtils`, 4), `cancel(Z)` (Spring
`CompletableToListenableFutureAdapter.cancel`, `DelegatingCompletableFuture.cancel`). Results: `get()` (Spring
`Decoder.decode`, webmvc `DefaultAsyncServerResponse.block`, 3), `get(long,TimeUnit)` (Spring Boot
`HikariCheckpointRestoreLifecycle.closeConnections`, `JdkClientHttpRequest.executeInternal`, 4), `join()` (Spring
`ConcurrentMapCache.lambda$retrieve$1`), `getNow(Object)` (`DefaultAsyncServerResponse.delegate`), `isDone()`,
`isCancelled()`. Stages: `whenComplete(BiConsumer)` on the class (Spring `CacheAspectSupport$CachePutRequest.apply`,
`CompletableToListenableFutureAdapter.<init>`, webmvc `DefaultAsyncServerResponse.createDeferredResult`, 4) and on
`CompletionStage` (opcode 185: micrometer `ObservedAspect.observe`, Spring `ApplicationListenerMethodAdapter.handleResult`,
webmvc `CompletionStageEntityResponse`, 4), `thenCompose(Function)` on the class (Spring
`CacheAspectSupport.findInCaches`) and on `CompletionStage` (Hibernate `EventListenerGroupImpl.fireEventOnEachListener`,
3), `CompletionStage.toCompletableFuture()` (Spring `CompletableToListenableFutureAdapter.<init>`), and
`new CompletionException(Throwable)` (Spring `FutureUtils.lambda$toSupplier$0`). The rest of the core
(`failedFuture`, `isCompletedExceptionally`, `thenApply`, `thenAccept`, `thenRun`, `handle`, `exceptionally`) is the
minimum for a coherent stage API.

The `runAsync`/`supplyAsync`/`completeAsync` rows (Spring Boot, Spring `AsyncTaskExecutor.submitCompletable`,
`FutureUtils.callAsync`, `ConcurrentMapCache.retrieve`) need an executor and stay unlinked. `thenCombine`,
`allOf`/`anyOf`, `obtrudeValue`/`obtrudeException` and `toString` are not referenced and are a later task.

## Members to implement

1. `java/util/concurrent/CompletionException.java`: `public class CompletionException extends RuntimeException` with
   `protected CompletionException()`, `protected CompletionException(String message)`,
   `public CompletionException(String message, Throwable cause)`, `public CompletionException(Throwable cause)`
   (each just calls the matching `super(...)`; `(Throwable cause)` gets the message `cause.toString()` from
   `Throwable`, or `null` for a `null` cause).

2. `java/util/concurrent/CompletionStage.java`: `public interface CompletionStage<T>` (imports
   `java.util.function.BiConsumer`, `BiFunction`, `Consumer`, `Function`) declaring exactly these abstract methods:

   - `<U> CompletionStage<U> thenApply(Function<? super T, ? extends U> fn);`
   - `CompletionStage<Void> thenAccept(Consumer<? super T> action);`
   - `CompletionStage<Void> thenRun(Runnable action);`
   - `<U> CompletionStage<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn);`
   - `<U> CompletionStage<U> handle(BiFunction<? super T, Throwable, ? extends U> fn);`
   - `CompletionStage<T> whenComplete(BiConsumer<? super T, ? super Throwable> action);`
   - `CompletionStage<T> exceptionally(Function<Throwable, ? extends T> fn);`
   - `CompletableFuture<T> toCompletableFuture();`

3. `java/util/concurrent/CompletableFuture.java`:
   `public class CompletableFuture<T> implements Future<T>, CompletionStage<T>` with

   - `static final class AltResult { final Throwable ex; AltResult(Throwable ex) {...} }` and
     `static final AltResult NIL = new AltResult(null);`
   - fields `Object result;` (`null` = not completed, `NIL` = completed with `null`, an `AltResult` with non-null
     `ex` = completed exceptionally, anything else = the value) and `Completion stack;` (dependents, newest first)
   - a nested `static final class Completion` (fields: an `int kind` or similar, the source future, the dependent
     future, the function object, `Completion next`) with a method `CompletableFuture fire()` (below)
   - `public CompletableFuture()` and `CompletableFuture(Object r)` (package-private, sets `result`)
   - `public static <U> CompletableFuture<U> completedFuture(U value)`
   - `public static <U> CompletableFuture<U> failedFuture(Throwable ex)`
   - `public boolean complete(T value)`
   - `public boolean completeExceptionally(Throwable ex)`
   - `public boolean cancel(boolean mayInterruptIfRunning)`
   - `public boolean isDone()`, `public boolean isCancelled()`, `public boolean isCompletedExceptionally()`
   - `public T get() throws InterruptedException, ExecutionException`
   - `public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException`
   - `public T join()`, `public T getNow(T valueIfAbsent)`
   - `public <U> CompletableFuture<U> thenApply(Function<? super T, ? extends U> fn)`
   - `public CompletableFuture<Void> thenAccept(Consumer<? super T> action)`
   - `public CompletableFuture<Void> thenRun(Runnable action)`
   - `public <U> CompletableFuture<U> thenCompose(Function<? super T, ? extends CompletionStage<U>> fn)`
   - `public <U> CompletableFuture<U> handle(BiFunction<? super T, Throwable, ? extends U> fn)`
   - `public CompletableFuture<T> whenComplete(BiConsumer<? super T, ? super Throwable> action)`
   - `public CompletableFuture<T> exceptionally(Function<Throwable, ? extends T> fn)`
   - `public CompletableFuture<T> toCompletableFuture()` (returns `this`)
   - private helpers as you like (suggested below: `encodeValue`, `wrap(Throwable)`, `wrap(Throwable, Object)`,
     `set(Object)`, `push(Completion)`, `postComplete()`, `run(...)`, `stage(...)`, `reportGet`, `reportJoin`).

## Members to leave out

- Everything that needs an executor or another thread: `runAsync`, `supplyAsync`, `completeAsync`, every
  `...Async` stage method, every member taking an `Executor`, `defaultExecutor`, `newIncompleteFuture`,
  `delayedExecutor`. The test checks that `runAsync(Runnable)`, `supplyAsync(Supplier)`, `completeAsync(Supplier)`,
  `thenApplyAsync(Function)` and `CompletionStage.whenCompleteAsync(BiConsumer)` stay unlinked. Do not mention
  `Executor`, `ForkJoinPool` or `Thread`.
- Time: `orTimeout`, `completeOnTimeout` (the test checks `orTimeout(long, TimeUnit)`).
- Not referenced by the pinned libraries, a later task: `thenCombine`/`thenAcceptBoth`/`runAfterBoth`, the
  `...Either` methods, `allOf`, `anyOf`, `exceptionallyCompose`, `obtrudeValue`, `obtrudeException`,
  `getNumberOfDependents`, `minimalCompletionStage`, `copy`, `completedStage`, `failedStage`, `resultNow`,
  `exceptionNow`, `state`, `toString` (the JDK's contains an identity hash).
- Do not edit `Future.java`, `ExecutionException.java`, `CancellationException.java`, `TimeoutException.java` or any
  other runtime file.

## JDK behaviour to match

Result encoding and wrapping (the hard part; every rule below is checked by the test):

- `encodeValue(v)`: `v == null ? NIL : v`.
- `wrap(x)` ("a stage function threw `x`"): `new AltResult(x instanceof CompletionException ? x : new
  CompletionException(x))`.
- `wrap(x, r)` ("the source `r` failed with `x`, propagate it"): if `x` is not a `CompletionException`, return
  `new AltResult(new CompletionException(x))`; else if `r` is an `AltResult` whose `ex == x`, return `r` itself (the
  same `CompletionException` object travels down the chain); else `new AltResult(x)`.
- `completeExceptionally(ex)` and `failedFuture(ex)` store `ex` **raw** (no wrapping): `new AltResult(ex)`.
  `null` throws `new NullPointerException()` (for both, even on a completed future).
- `complete(v)`: if `result == null` set `encodeValue(v)`; then `postComplete()`; return whether this call
  completed it. `completeExceptionally(ex)`: the same with `new AltResult(ex)`. Once completed, later calls return
  `false` and change nothing.
- `cancel(b)`: `boolean c = result == null && set(new AltResult(new CancellationException()))` (message `null`);
  `postComplete()`; `return c || isCancelled();` So cancelling twice returns `true`, cancelling a normally or
  otherwise exceptionally completed future returns `false`.
- `isDone()`: `result != null`. `isCancelled()`: `result` is an `AltResult` whose `ex` is a
  `CancellationException` (so `completeExceptionally(new CancellationException("mine"))` also counts as cancelled).
  `isCompletedExceptionally()`: `result` is an `AltResult` and not `NIL`. A dependent of a cancelled future is
  exceptional but **not** cancelled (its `ex` is a `CompletionException`).
- `reportGet(r)` (for both `get`s): `NIL` returns `null`; a value returns it; for `AltResult` with `x`: a
  `CancellationException` is thrown as it is; a `CompletionException` with a non-null cause is replaced by its
  cause; then `throw new ExecutionException(x)` (message `x.toString()`). So a `CompletionException("bare", null)`
  comes out wrapped: `ExecutionException<CompletionException:bare>`.
- `reportJoin(r)` (for `join` and `getNow`): `NIL` returns `null`; `CancellationException` and
  `CompletionException` are thrown as they are (the same object); any other `x` throws
  `new CompletionException(x)` (a new one each call).
- `getNow(d)`: `d` when not completed, else `reportJoin(result)`.
- `get(timeout, unit)`: first `unit.toNanos(timeout)` (so a `null` unit throws `NullPointerException`, even when
  completed); not completed throws `new TimeoutException()` (message `null`, whatever the timeout: no thread
  could complete it while we wait; the JVM gives the same for the short timeouts the test uses); else
  `reportGet(result)`.
- `get()` and `join()` on a future that is not completed would block forever on the JDK. Throw
  `new IllegalStateException("CompletableFuture.get would block forever: not completed and no other thread")`
  (`join` for `join()`). The test never reaches this.

Stages. Every stage method first throws `new NullPointerException()` for a `null` function (also on a pending
source), creates a new dependent `d`, and then: if this future is completed, computes `d`'s result right now;
otherwise pushes a `Completion` onto `stack` (`c.next = stack; stack = c;`) and returns `d`. Computing `d` from the
source result `r` (one shared method for "now" and "later"; `x` = the `AltResult`'s `ex` or `null`, `v` = the value or
`null`), and only if `d.result == null` (never overwrite a dependent that was completed by hand):

| method | source normal | source failed with `x` | function throws `ex` |
|---|---|---|---|
| `thenApply(fn)` | `encodeValue(fn.apply(v))` | `wrap(x, r)`, fn not called | `wrap(ex)` |
| `thenAccept(a)` / `thenRun(a)` | call it, then `NIL` | `wrap(x, r)`, not called | `wrap(ex)` |
| `thenCompose(fn)` | `g = fn.apply(v).toCompletableFuture()` (a `null` stage gives an NPE, so `wrap(npe)`); if `g` is completed, relay `g.result`, else push a relay `Completion` on `g` that relays to `d` when `g` completes | `wrap(x, r)`, not called | `wrap(ex)` |
| `handle(fn)` | `encodeValue(fn.apply(v, null))` | `encodeValue(fn.apply(null, x))` | `wrap(ex)` |
| `whenComplete(a)` | `a.accept(v, null)`, then `r` itself | `a.accept(null, x)`, then `wrap(x, r)` | normal source: `wrap(ex)`; failed source: if `ex != x` then `x.addSuppressed(ex)`; then `wrap(x, r)` |
| `exceptionally(fn)` | `r` itself, fn not called | `encodeValue(fn.apply(x))` | `wrap(ex)` |

Relaying `g`'s result `s` (thenCompose): a value or `NIL` as it is; an `AltResult` with `x` becomes `wrap(x, s)`
(`failedFuture(e)` inside gives a new `CompletionException(e)`, `failedFuture(ce)` gives the same `ce`).

Note `x` is the raw stored exception: `exceptionally`/`handle`/`whenComplete` on a `failedFuture(e)` get `e`
itself; on a dependent of it they get the `CompletionException` wrapping `e`.

Order of dependents (JDK `postComplete`, without the atomics). Completing a future (by `complete`,
`completeExceptionally`, `cancel`, or as a dependent) runs every dependent pushed before, newest first, and the
dependents of those dependents in this exact order:

```java
final void postComplete() {
    CompletableFuture f = this; Completion h;
    while ((h = f.stack) != null || (f != this && (h = (f = this).stack) != null)) {
        Completion t = h.next;
        f.stack = t;
        if (t != null && f != this) { h.next = stack; stack = h; continue; } // move to this.stack
        h.next = null;
        CompletableFuture d = h.fire();
        f = d == null ? this : d;
    }
}
```

`Completion.fire()` computes its dependent from the source's result (the table above) and returns the dependent if
it now has a result and a non-empty `stack`, else `null`. `postComplete` runs even when `complete`/`cancel` did not
change the result (it finds an empty stack). The test registers this tree on a pending `src`: `A = src.thenApply`,
`B = src.thenAccept`, `A1 = A.thenApply`, `A2 = A.thenRun`, `A3 = A.whenComplete`, `C = src.whenComplete`,
`B1 = B.thenRun`, `A11 = A1.thenRun`; `src.complete` must run them as `C B B1 A A1 A11 A2 A3` (a plain recursive walk
gives `... A A3 A2 A1 A11`, which is wrong). A stage added to an already completed future runs immediately, in the
caller.

## Steps

1. Read the test, then `java/util/concurrent/LinkedBlockingQueue.java` for style and T75's `Future.java`,
   `ExecutionException.java`, `CancellationException.java` and `TimeoutException.java`.
2. Create `CompletionException.java`, `CompletionStage.java` and `CompletableFuture.java` under
   `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/` with the members above.
3. Register them (do not edit `BytecodeExceptionRuntime.java` by hand):
   - `javaw-agent-kit/bin/kit register java/util/concurrent/CompletionException.java`
   - `javaw-agent-kit/bin/kit register java/util/concurrent/CompletionStage.java`
   - `javaw-agent-kit/bin/kit register java/util/concurrent/CompletableFuture.java`
4. `javaw-agent-kit/bin/kit check T76` until PASS.
5. Write `docs/bytecode-completable-future.md` (below), the report, then `javaw-agent-kit/bin/kit finish T76`.

## Hints

- One `Completion` class with an `int kind` (`APPLY`, `ACCEPT`, `RUN`, `COMPOSE`, `HANDLE`, `WHEN`, `EXCEPTIONALLY`,
  `RELAY`) and an `Object fn` keeps the class short; one static method `run(kind, fn, r, d)` does the table above for
  both the "source already completed" path and `Completion.fire()`. Use an `int` `switch` or `if`/`else` (no `switch`
  on an enum). Raw types plus `@SuppressWarnings({"unchecked", "rawtypes"})` on the class are fine.
- Write the dependent through a `set(Object r)` helper that only stores when `result == null`; then a dependent
  completed by hand before its source (`d1.complete("manual")`) keeps its value and its own dependents ran then.
- `catch (Throwable ex)` around the function call; the whenComplete-on-failed-source case is the only one that keeps
  `x`.
- `CancellationException` extends `IllegalStateException`: in `reportGet`/`reportJoin` test for it **before**
  anything else.
- `getNow`, `join` and `get` never wrap a value; only exceptional results differ between them.
- The class has about 150 lines; `CompletionStage` about 15; `CompletionException` 7.
- Calibration notes: a reference written from this spec alone passed on the first `kit check`; replacing the
  `postComplete` loop with a recursive walk (fire, then `d.postComplete()`) fails `dependentsRunInJdkOrder`. Other
  easy mistakes: `thenCompose` must call `toCompletableFuture()` on the returned stage inside the `try` (a `null`
  stage gives `CompletionException<NullPointerException>`, it must not escape); a pending inner stage needs a relay
  `Completion` pushed on `g`, otherwise `d` stays pending after `g.complete`; `reportGet` must throw a stored
  `CancellationException` itself, not wrap it in `ExecutionException`.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
statesAndCompletion, exceptionWrapping, dependentsOfCompletedStages, thenCompose, dependentsRunInJdkOrder, completionStageInterface, leavesOutAsyncAndTimeoutMembers: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/CompletableFuture` (or `CompletionStage`, `CompletionException`) or names one of their methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register` for each of the three files and add every member listed under Members to implement. If it names `Future`, `ExecutionException`, `CancellationException` or `TimeoutException`, T75 is not done: stop and `kit block`. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
states: completeExceptionally/failedFuture store the exception raw and throw `new NullPointerException()` for null; complete returns false once completed; completedFuture(null) stores NIL so get/join/getNow return null.
timedGet: get(timeout, unit) calls unit.toNanos(timeout) first (null unit gives NullPointerException even when done) and throws `new TimeoutException()` when not completed.
wrapping: get throws `new ExecutionException(x)` after replacing a CompletionException by its non-null cause; join/getNow rethrow a CompletionException as the same object and wrap anything else in a new CompletionException.
cancellation: cancel stores `new AltResult(new CancellationException())`, returns `c || isCancelled()`; isCancelled checks `ex instanceof CancellationException`; get/join rethrow a CancellationException unwrapped, but a dependent holds CompletionException<CancellationException> and is not cancelled.
dependentsOfDone: a function that throws gives wrap(ex) (CompletionException unless it already is one); exceptionally and whenComplete on a normal source keep the source result r; every stage method throws NullPointerException for a null function before anything else.
exceptionalSources: propagate a source failure with wrap(x, r): a raw x gets a new CompletionException, a CompletionException stored in r is passed on as the same object; whenComplete on a failed source adds the action's exception as suppressed (unless it is x itself) and keeps x.
compose: thenCompose calls fn.apply(v).toCompletableFuture() inside the try (null gives CompletionException<NullPointerException>); a completed inner g is relayed with wrap(x, s) for failures; a pending g gets a relay Completion pushed on its stack.
pendingOrder, pendingExceptional: copy the postComplete loop from the task exactly; Completion.fire() returns the dependent only when it has a result and a non-empty stack; a dependent that already has a result is never overwritten.
stageInterface: CompletionStage must declare thenApply, thenAccept, thenRun, thenCompose, handle, whenComplete, exceptionally and toCompletableFuture, and toCompletableFuture returns this.
leavesOutAsyncAndTimeoutMembers: this fails when an Async, Executor or timeout member (runAsync, supplyAsync, completeAsync, thenApplyAsync, whenCompleteAsync, orTimeout) became linked. Remove it.
```

## Docs

Create `docs/bytecode-completable-future.md` with this skeleton (fill in the brackets):

    # Bytecode CompletableFuture

    Revision `completable-future-vN` links `java.util.concurrent.CompletionException`, the `CompletionStage`
    interface and `CompletableFuture` in the bundled exception runtime under the single-threaded contract.

    Contract:

    - <result encoding (NIL, AltResult); complete/completeExceptionally/cancel/failedFuture/completedFuture; isDone,
      isCancelled, isCompletedExceptionally; get/get(timeout)/join/getNow and their ExecutionException,
      CompletionException and CancellationException rules; the stage table (thenApply, thenAccept, thenRun,
      thenCompose, handle, whenComplete, exceptionally) with its wrapping rules; JDK postComplete order>

    Known differences: no executor and no other thread. `get()`/`join()` on an incomplete future throw
    IllegalStateException instead of blocking forever, and `get(timeout, unit)` throws TimeoutException at once.
    Async/Executor members, orTimeout/completeOnTimeout, thenCombine and the either/both family, allOf/anyOf,
    obtrude*, toString and the Java 19 state members are not linked.

    Evidence: `BytecodeCompletableFutureTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `runAsync(Runnable)`, `supplyAsync(Supplier)`, `completeAsync(Supplier)`,
    `thenApplyAsync(Function)`, `CompletionStage.whenCompleteAsync(BiConsumer)` and `orTimeout(long, TimeUnit)` are
    rejected before WAT generation.
