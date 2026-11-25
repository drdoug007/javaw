# T75 Callable, Future and FutureTask

```kit
id: T75
slug: future-task
title: link java.util.concurrent Callable, Future, RunnableFuture, FutureTask, ExecutionException, CancellationException and TimeoutException (single-threaded)
tests: BytecodeFutureTaskTest
depends: T52
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/Callable.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/Future.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/RunnableFuture.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/FutureTask.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/ExecutionException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/CancellationException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/TimeoutException.java
allow: docs/bytecode-future-task.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-future-task-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeFutureTaskTest.java`.

## Goal

Add the `java.util.concurrent` future family to the bundled runtime under the single-threaded contract (Wasm runs
one thread; no locks, no `Thread`, no parking). `runtime-target/target/audit/member-references.tsv` lists the uses:

- `Future` (opcode 185, 30 rows in 15 artifacts): `get()` (Tomcat 20 sites, websocket 5, H2 4, byte-buddy 3,
  logback, Spring core/aop/beans/orm/tx, spring-boot), `get(JLjava/util/concurrent/TimeUnit;)` (Tomcat 11,
  websocket 8, logback-core, spring-core), `cancel(Z)` (Tomcat 5, Spring context 3, byte-buddy 2, logback, web),
  `isCancelled()` (Tomcat 5) and `isDone()` (Tomcat 3, spring-core, spring-tx).
- `Callable.call()` (opcode 185, 8 artifacts: byte-buddy 7, spring-web 4, Hibernate, JAXB, spring-context 3, H2).
- `FutureTask` (spring-core only): `<init>(Ljava/util/concurrent/Callable;)V` 5, `<init>(Ljava/lang/Runnable;Ljava/lang/Object;)V` 4,
  `run()V`, `get()`.
- `ExecutionException` (14 rows in 10 artifacts): `<init>(Ljava/lang/Throwable;)V` (Tomcat 8, websocket 4,
  spring-context, spring-core), `<init>(Ljava/lang/String;Ljava/lang/Throwable;)V` (websocket 2) and inherited
  `getCause()` (Tomcat, spring-core 4, H2, byte-buddy, Spring aop/orm/tx/web).
- `CancellationException.<init>()V` / `<init>(Ljava/lang/String;)V` (antlr4, spring-context, spring-web).
- `TimeoutException.<init>()V` / `<init>(Ljava/lang/String;)V` (websocket, spring-boot), inherited `getMessage`
  (spring-web) and `initCause` (websocket).

`RunnableFuture` has no row of its own; `FutureTask` implements it, so it is added for a correct hierarchy.

**Single-threaded contract** (as in T51/T53, see `docs/bytecode-linked-blocking-queue.md` and
`docs/bytecode-timed-lock-queue.md`): no other thread can ever complete a task. `get()` on a task that is not done
would wait forever in the JDK; here it throws `IllegalStateException` (never compared with the JVM). The timed
`get(timeout, unit)` on a task that is not done returns at once with the JDK's after-timeout result:
`TimeoutException`. It never waits, sleeps or reads a clock.

## Members to implement

All files are in `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/`
(`package java.util.concurrent;`). `TimeUnit`, `Runnable`, `IllegalStateException` already exist.

1. `Callable.java`: `@FunctionalInterface public interface Callable<V>` with `V call() throws Exception;`.
2. `Future.java`: `public interface Future<V>` declaring exactly
   - `boolean cancel(boolean mayInterruptIfRunning);`
   - `boolean isCancelled();`
   - `boolean isDone();`
   - `V get() throws InterruptedException, ExecutionException;`
   - `V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException;`
3. `RunnableFuture.java`: `public interface RunnableFuture<V> extends Runnable, Future<V>` with `void run();`.
4. `ExecutionException.java`: `public class ExecutionException extends Exception` with
   `protected ExecutionException()`, `protected ExecutionException(String message)`,
   `public ExecutionException(String message, Throwable cause)`, `public ExecutionException(Throwable cause)`.
5. `CancellationException.java`: `public class CancellationException extends IllegalStateException` with
   `public CancellationException()` and `public CancellationException(String message)`.
6. `TimeoutException.java`: `public class TimeoutException extends Exception` with `public TimeoutException()` and
   `public TimeoutException(String message)`.
7. `FutureTask.java`: `public class FutureTask<V> implements RunnableFuture<V>` with
   - private fields: `int state`, `Callable<V> callable`, `Object outcome`, `boolean running`
   - a private static nested adapter class `implements Callable<T>` holding a `Runnable` and a result
   - `public FutureTask(Callable<V> callable)`
   - `public FutureTask(Runnable runnable, V result)`
   - `public boolean isCancelled()`, `public boolean isDone()`, `public boolean cancel(boolean mayInterruptIfRunning)`
   - `public V get() throws InterruptedException, ExecutionException`
   - `public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException`
   - `protected void done()`, `protected void set(V v)`, `protected void setException(Throwable t)`
   - `public void run()`
   - `public String toString()`

## Members to leave out

- `FutureTask.runAndReset()` (protected; only `ScheduledThreadPoolExecutor` uses it): checked unlinked.
- The JDK 19+ `Future` default methods and their `FutureTask` overrides: `resultNow()`, `exceptionNow()`, `state()`
  and the `Future.State` enum. Checked unlinked (`FutureTask.resultNow`, `Future.exceptionNow`). The fixtures are
  compiled for Java 17.
- `Executors`, `ExecutorService`, `Executor`, `CompletableFuture`, `ScheduledFuture`, `Delayed`: not part of this
  task. `Executors.callable(Runnable, Object)` is checked unlinked: write your own private adapter class.
- The JDK internals: `WaitNode`, `awaitDone`, `removeWaiter`, `VarHandle`s, `LockSupport`, the `runner` thread,
  `handlePossibleCancellationInterrupt`, `Thread.interrupt`. No `volatile`, `synchronized`, `Thread`.
- Do not edit `TimeUnit.java` or any other existing runtime file.

## JDK behaviour to match

JDK 21 `FutureTask` has states `NEW`, `COMPLETING`, `NORMAL`, `EXCEPTIONAL`, `CANCELLED`, `INTERRUPTING`,
`INTERRUPTED`. Single-threaded, the transient `COMPLETING`/`INTERRUPTING` states are never observable, so a plain
`int` with `NEW`, `NORMAL`, `EXCEPTIONAL`, `CANCELLED`, `INTERRUPTED` is enough (keep `CANCELLED < INTERRUPTED`).

- Constructors: a `null` callable/runnable throws `new NullPointerException()` (message `null`). The `(Runnable, V)`
  form wraps the runnable in an adapter whose `call()` runs it and returns `result` (which may be `null`).
- `isDone()`: `state != NEW` (true after normal completion, an exception or cancellation). `isCancelled()`:
  `state >= CANCELLED`.
- `run()`: if `state != NEW` or the task is already running (a `run()` call from inside its own `call()`), return
  and do nothing. Otherwise mark running, and if `callable != null && state == NEW` (the JDK re-checks the state; on one thread it
  is still `NEW` here): `try { result = c.call(); ran = true; } catch
  (Throwable ex) { result = null; ran = false; setException(ex); } if (ran) set(result);` and finally clear the
  running flag. `set`/`setException` are **virtual** calls (the `Spy` subclass logs `set:a;` / `setEx:Bad: e;`).
  The callable runs at most once; a second `run()` does not call it. If `cancel` is called from inside `call()`, the
  value returned afterwards still goes to `set`, which then does nothing (state is no longer `NEW`).
- `set(v)`: only if `state == NEW`: `outcome = v; state = NORMAL;` then complete. `setException(t)`: only if
  `state == NEW`: `outcome = t; state = EXCEPTIONAL;` then complete (a `null` `t` is stored as is). Called on a
  finished task they do nothing (no `done()`). A subclass may call them before `run()`, which then is a no-op.
- Completion (`finishCompletion`): call `done()` (virtual, empty by default) **after** the final state is stored,
  then `callable = null`. So `done()` sees `isDone() == true` and can call `get()`. If `done()` throws, the
  exception propagates out of `set`/`setException`/`cancel` (and so out of `run`), but the state is already final.
- `cancel(mayInterrupt)`: if `state != NEW` return `false` (also after normal completion; nothing changes, `done()`
  is not called). Else `state = mayInterrupt ? INTERRUPTED : CANCELLED`, complete (calls `done()`), return `true`.
  There is no running thread to interrupt.
- `report(s)` used by both `get`s: `NORMAL` returns `outcome`; `s >= CANCELLED` throws
  `new CancellationException()` (message `null`); otherwise throws `new ExecutionException((Throwable) outcome)` —
  a **new** exception object on each call, with the stored throwable as cause. `ExecutionException(Throwable)` sets
  the message to `cause.toString()` (or `null` for a `null` cause), which `Exception(Throwable)` already does. A
  `CancellationException` or `TimeoutException` thrown by `call()` is just an exception: it is wrapped in
  `ExecutionException` and `isCancelled()` stays `false`.
- `get()`: if done, `report(state)`. If not done, throw `new IllegalStateException("FutureTask.get would block
  forever: task not completed and no other thread")` (never reached by the test).
- `get(timeout, unit)`: first `if (unit == null) throw new NullPointerException();` (even when done). If done,
  `report(state)`. If not done, `unit.toNanos(timeout)` (result ignored) and `throw new TimeoutException()`
  (message `null`) for every timeout value (0, negative, `Long.MIN_VALUE`, 1 ns). Inside `call()` the task is not
  done yet, so `get(0, SECONDS)` there throws `TimeoutException` too.
- Overridable self-calls: only `run -> set/setException` and completion `-> done()`. `get`, `get(timed)`, `cancel`,
  `run` and `toString` never call `isDone()`, `isCancelled()`, `get()` or `cancel()` (the `Spy` subclass logs
  `isDone;`, `get;` and `cancel:...;`).
- `toString()`: `super.toString()` (`Object`: class name `@` hex identity hash) followed by
  `[Completed normally]`, `[Completed exceptionally: ` + `outcome` + `]` (string concatenation, so a `null` outcome
  prints `null`), `[Cancelled]` for both cancelled states, or for `NEW`: `[Not completed, task = ` + callable + `]` (a comma, no parentheses)
  (calls the callable's `toString()`; the test only checks the prefix for the runnable adapter) or
  `[Not completed]` when `callable` is `null`. The fixtures strip everything before `[`.

## Steps

1. Read the test, then `java/util/concurrent/CountDownLatch.java` and `TimeUnit.java` for the single-threaded style.
2. Create the seven files above.
3. Register each one (never edit `BytecodeExceptionRuntime.java` by hand), interfaces and superclasses first:
   - `javaw-agent-kit/bin/kit register java/util/concurrent/Callable.java`
   - `javaw-agent-kit/bin/kit register java/util/concurrent/Future.java`
   - `javaw-agent-kit/bin/kit register java/util/concurrent/RunnableFuture.java`
   - `javaw-agent-kit/bin/kit register java/util/concurrent/ExecutionException.java`
   - `javaw-agent-kit/bin/kit register java/util/concurrent/CancellationException.java`
   - `javaw-agent-kit/bin/kit register java/util/concurrent/TimeoutException.java`
   - `javaw-agent-kit/bin/kit register java/util/concurrent/FutureTask.java`
4. `javaw-agent-kit/bin/kit check T75` until PASS.
5. Write `docs/bytecode-future-task.md` (below), the report, then `javaw-agent-kit/bin/kit finish T75`.

## Hints

- Use a `boolean running` field for the re-entrant `run()` guard instead of the JDK's `runner` thread. Without it,
  `run()` called from inside `call()` runs the callable again (the `notDone` fixture expects `1` call).
- Call `done()` after storing `state` and `outcome`, never before: the `Peek` fixture calls `get()` from `done()`
  and the `Spy` logs `done(isDone;truefalse);` (its `done()` writes `done(` and then calls the overridden,
  logging `isDone()`).
- In `run()`, call `set(result)` **outside** the `try/catch (Throwable)` around `c.call()`; otherwise an exception
  thrown by an overridden `done()` (the `Throwing` fixture) is caught and turned into `setException`, which then does
  nothing. The JDK lets it propagate out of `run()`.
- The `NEW` toString text is JDK 21's `[Not completed, task = Named:a]`, not the older
  `[Not completed (task = ...)]` seen in some JDK sources (calibration got this wrong first).
- Do not check `isDone()` / call `get()` from `get(timeout, unit)` or `cancel`; read the `state` field.
- `ExecutionException(Throwable)` is `super(cause)`; the inherited `Throwable(Throwable)` sets the message to
  `cause == null ? null : cause.toString()`. The protected no-argument and `String` constructors must stay
  `protected` (the fixture subclasses them as `MyEE`).
- `CancellationException` extends `IllegalStateException`, `TimeoutException` and `ExecutionException` extend
  `Exception` (checked).
- Do not write `volatile`, `synchronized`, `native`, `Thread`, `LockSupport` or `System.nanoTime` anywhere.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.
  When a fixture throws only in Wasm, `kit check` prints `(thrown in Wasm: <Java exception>)`.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
completesWithResult, completesExceptionally, cancelsAndTimesOut, callsProtectedHooks: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/FutureTask` (or `Future`, `Callable`, `RunnableFuture`, `ExecutionException`, `CancellationException`, `TimeoutException`), `link a complete acyclic type hierarchy` or names one of their methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register` for each of the seven files (never edit BytecodeExceptionRuntime.java by hand). If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
completesWithResult: run() calls the callable once and stores the result; get(timeout, null) throws NullPointerException even when done; cancel after completion returns false; the toString suffix is `[Completed normally]` or `[Not completed, task = <callable>]` (JDK 21 text: comma, no parentheses); a null callable/runnable throws `new NullPointerException()`.
completesExceptionally: a throwable from call() is stored by setException; each get() throws a new ExecutionException whose cause is that throwable and whose message is cause.toString(); a CancellationException thrown by call() is wrapped and isCancelled() stays false; toString is `[Completed exceptionally: <outcome>]`.
cancelsAndTimesOut: cancel on a NEW task sets the state, calls done() and returns true; later cancel/run do nothing; get throws `new CancellationException()`. get(timeout, unit) on a task that is not done throws `new TimeoutException()` for every timeout, also inside call(); run() called from inside call() must return without calling the callable again (keep a running flag).
callsProtectedHooks: run calls the virtual set/setException; set/setException do nothing when the state is not NEW; done() runs after the state is stored (so get() works inside done) and its exception propagates out of run/cancel; set(result) must be outside the try/catch around call().
leavesOutExecutorsAndJdk19Members: this fails when runAndReset, resultNow/exceptionNow or Executors.callable became linked. Do not add them, and do not create Executors.java.
```

## Docs

Create `docs/bytecode-future-task.md` with this skeleton (fill in the brackets):

    # Bytecode FutureTask

    Revision `future-task-vN` links `java.util.concurrent.Callable`, `Future`, `RunnableFuture`, `FutureTask`,
    `ExecutionException`, `CancellationException` and `TimeoutException` in the bundled exception runtime under
    the single-threaded contract.

    Contract:

    - <state machine NEW/NORMAL/EXCEPTIONAL/CANCELLED/INTERRUPTED; run once with re-entrant guard; set/setException
      no-ops after completion; done() after the final state, its exception propagates; cancel; report with a new
      ExecutionException per get; timed get with the null-unit check and TimeoutException; toString suffixes;
      the overridable self-calls; the exception classes and constructors>

    Known differences: no waiter queue, runner thread or interrupts (the selected deployment is single-threaded).
    `get()` on a task that is not done throws IllegalStateException instead of blocking forever; the timed `get`
    throws TimeoutException at once. `cancel(true)` interrupts nothing. `runAndReset`, the JDK 19 `resultNow`/
    `exceptionNow`/`state` members and Executors are not linked.

    Evidence: `BytecodeFutureTaskTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `FutureTask.runAndReset`, `FutureTask.resultNow`, `Future.exceptionNow` and
    `Executors.callable` are rejected before WAT generation.
