# Bytecode FutureTask

Revision `future-task-v338` links `java.util.concurrent.Callable`, `Future`, `RunnableFuture`, `FutureTask`,
`ExecutionException`, `CancellationException` and `TimeoutException` in the bundled exception runtime under
the single-threaded contract.

Contract:

- State machine `NEW` -> `NORMAL`, `EXCEPTIONAL`, `CANCELLED` or `INTERRUPTED`, held in a plain `int`
  (`CANCELLED < INTERRUPTED`; the transient JDK `COMPLETING`/`INTERRUPTING` states are unobservable on one
  thread). `isDone()` is `state != NEW`, `isCancelled()` is `state >= CANCELLED`.
- `run()` runs the callable at most once, guarded by `state != NEW` and a `boolean running` flag so a `run()`
  call from inside `call()` returns without re-invoking the callable; the `running` flag is cleared in a
  `finally`. The callable's `Throwable` goes to `setException`, a normal return to `set`, both virtual calls;
  `set(result)` is outside the `try/catch (Throwable)` around `call()` so an exception from an overridden
  `done()` propagates out of `run()`. A `null` callable/runnable throws `new NullPointerException()` (message
  `null`); the `(Runnable, V)` constructor wraps the runnable in a private static `Callable<T>` adapter whose
  `call()` runs it and returns `result`.
- `set(v)` and `setException(t)` change nothing unless `state == NEW`, then store `outcome`, set the final
  state and complete; a `null` `t` is stored as is. Subclasses may call them before `run()`, which then is a
  no-op.
- Completion calls the virtual `done()` **after** the final state and `outcome` are stored, then clears
  `callable`; so `done()` sees `isDone() == true` and can call `get()`. An exception from `done()` propagates
  out of `set`/`setException`/`cancel` (and so out of `run`), but the state is already final.
- `cancel(mayInterrupt)` returns `false` when `state != NEW` (nothing changes, no `done()`); otherwise stores
  `INTERRUPTED` (true) or `CANCELLED` (false), completes and returns `true`. There is no running thread to
  interrupt.
- Both `get`s read the `state` field via `report(s)`: `NORMAL` returns `outcome`; `s >= CANCELLED` throws a
  new `CancellationException()` (message `null`); otherwise a **new** `ExecutionException((Throwable) outcome)`
  per call, whose message is `cause.toString()` (`null` for a `null` cause). `get()` on a task that is not done
  throws `IllegalStateException`; `get(timeout, unit)` first checks `unit == null` -> `NullPointerException`
  (even when done), then for a not-done task evaluates `unit.toNanos(timeout)` and throws
  `new TimeoutException()` (message `null`) for every timeout value.
- `toString()` is `super.toString()` plus `[Completed normally]`, `[Completed exceptionally: ` + `outcome` +
  `]`, `[Cancelled]` (both cancelled states), `[Not completed, task = ` + callable + `]` (comma, no
  parentheses, the callable's `toString()`) or `[Not completed]` when the callable is `null`.
- The only overridable self-calls are `run` -> `set`/`setException` and completion -> `done()`; `get`,
  `get(timed)`, `cancel`, `run` and `toString` never call `isDone()`, `isCancelled()`, `get()` or `cancel()`.
- `ExecutionException` extends `Exception` with `protected ()`, `protected (String)`, `public (String,
  Throwable)` and `public (Throwable)`; `CancellationException` extends `IllegalStateException` with `public
  ()`/`public (String)`; `TimeoutException` extends `Exception` with `public ()`/`public (String)`. `Callable`
  is a functional interface; `RunnableFuture` extends `Runnable` and `Future`.

Known differences: no waiter queue, runner thread or interrupts (the selected deployment is single-threaded).
`get()` on a task that is not done throws IllegalStateException instead of blocking forever; the timed `get`
throws TimeoutException at once. `cancel(true)` interrupts nothing. `runAndReset`, the JDK 19 `resultNow`/
`exceptionNow`/`state` members and Executors are not linked.

Evidence: `BytecodeFutureTaskTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `FutureTask.runAndReset`, `FutureTask.resultNow`, `Future.exceptionNow` and
`Executors.callable` are rejected before WAT generation.
