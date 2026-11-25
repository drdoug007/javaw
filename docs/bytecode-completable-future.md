# Bytecode CompletableFuture

Revision `completable-future-v339` links `java.util.concurrent.CompletionException`, the `CompletionStage`
interface and `CompletableFuture` in the bundled exception runtime under the single-threaded contract.

Contract:

- Result encoding: `null` = not completed, `NIL` (an `AltResult` with `ex == null`) = completed with the
  `null` value, an `AltResult` with a non-null `ex` = completed exceptionally, anything else = the value.
  `encodeValue(v)` is `v == null ? NIL : v`. `complete(v)` and `completeExceptionally(ex)` store the result
  only when not yet completed, always call `postComplete()` and return whether *this* call completed it;
  `completeExceptionally(null)` and `failedFuture(null)` throw `new NullPointerException()` (even on a
  completed future). `failedFuture(ex)` and `completeExceptionally(ex)` store `ex` **raw** (no wrapping).
  `completedFuture(null)` stores `NIL`.
- `cancel(mayInterrupt)` stores `new AltResult(new CancellationException())` (message `null`) when pending,
  calls `postComplete()` and returns `changed || isCancelled()` — so cancelling twice returns `true`, but
  cancelling a normally or otherwise exceptionally completed future returns `false`. `isDone()` is
  `result != null`; `isCancelled()` is `result` an `AltResult` whose `ex instanceof CancellationException`
  (so `completeExceptionally(new CancellationException(...))` counts as cancelled); `isCompletedExceptionally()`
  is `result` an `AltResult` and not `NIL`. A dependent of a cancelled future is exceptional but not
  cancelled (its `ex` is a `CompletionException` wrapping the `CancellationException`).
- `get()`/`get(timeout, unit)` use `reportGet`: a `CancellationException` is thrown as stored; a
  `CompletionException` with a non-null cause is replaced by that cause; anything else is thrown as a new
  `ExecutionException(x)` (message `x.toString()`, so a bare `CompletionException("bare", null)` comes out
  wrapped). `get(timeout, unit)` evaluates `unit.toNanos(timeout)` first (`null` unit -> `NullPointerException`
  even when done) and throws `new TimeoutException()` (message `null`) when not completed, whatever the
  timeout. `join()`/`getNow(d)` use `reportJoin`: a `CancellationException` or `CompletionException` is
  rethrown as the *same object*; any other `x` is thrown as a new `CompletionException(x)` per call.
  `getNow(d)` returns `d` when not completed, else `reportJoin(result)`. Neither `join` nor `getNow` ever
  wraps a value.
- Every stage method throws `new NullPointerException()` for a `null` function (also on a pending source),
  creates a fresh dependent `d`, and if the source is already completed computes `d`'s result immediately,
  else pushes a `Completion` on its stack. Computing `d` from the source result `r` (`x` = the `AltResult`'s
  `ex` or `null`, `v` = the value or `null`) never overwrites a dependent completed by hand, and its function
  is not invoked at all then:

  | method | source normal | source failed with `x` | function throws `ex` |
  |---|---|---|---|
  | `thenApply` | `encodeValue(fn.apply(v))` | `wrap(x, r)`, fn not called | `wrap(ex)` |
  | `thenAccept`/`thenRun` | call it, then `NIL` | `wrap(x, r)`, not called | `wrap(ex)` |
  | `thenCompose` | `fn.apply(v).toCompletableFuture()` (a `null` stage gives `CompletionException<NullPointerException>`); a completed inner stage relays its result, a pending one gets a relay `Completion` pushed on it | `wrap(x, r)`, not called | `wrap(ex)` |
  | `handle` | `encodeValue(fn.apply(v, null))` | `encodeValue(fn.apply(null, x))` | `wrap(ex)` |
  | `whenComplete` | `a.accept(v, null)`, then `r` itself | `a.accept(null, x)`, then `wrap(x, r)` | normal: `wrap(ex)`; failed: `x.addSuppressed(ex)` unless `ex == x`, then `wrap(x, r)` |
  | `exceptionally` | `r` itself, fn not called | `encodeValue(fn.apply(x))` | `wrap(ex)` |

- Wrapping: `wrap(ex)` is `new AltResult(ex)` when `ex` already is a `CompletionException`, else `new
  AltResult(new CompletionException(ex))`. `wrap(x, r)` returns `new AltResult(new CompletionException(x))`
  when `x` is not a `CompletionException`; when it is, it returns `r` itself if `r` is an `AltResult` whose
  `ex == x` (the same `CompletionException` object travels down the chain), else `new AltResult(x)`.
  Relaying an inner `thenCompose` result with an exception `x` uses `wrap(x, s)`, so a raw exception gets a
  new `CompletionException` and a stored `CompletionException` passes through as the same object. `x` is
  always the raw stored exception: `exceptionally`/`handle`/`whenComplete` on `failedFuture(e)` see `e`
  itself; on a dependent of it they see the wrapping `CompletionException`.
- `postComplete` runs every dependent pushed before, newest first, with the JDK loop: pop each head, and
  when the head came from a dependent's stack with a successor, re-link the successor chain onto `this.stack`
  and continue; a fired dependent that now has a result and a non-empty stack becomes the new `f`. The test
  tree `A = src.thenApply`, `B = src.thenAccept`, `A1 = A.thenApply`, `A2 = A.thenRun`, `A3 = A.whenComplete`,
  `C = src.whenComplete`, `B1 = B.thenRun`, `A11 = A1.thenRun` runs as `C B B1 A A1 A11 A2 A3`. A stage added
  to an already completed future runs immediately, in the caller. `CompletionStage.toCompletableFuture()`
  returns `this`.

Known differences: no executor and no other thread. `get()`/`join()` on an incomplete future throw
IllegalStateException instead of blocking forever, and `get(timeout, unit)` throws TimeoutException at once.
Async/Executor members, orTimeout/completeOnTimeout, thenCombine and the either/both family, allOf/anyOf,
obtrude*, toString and the Java 19 state members are not linked.

Evidence: `BytecodeCompletableFutureTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `runAsync(Runnable)`, `supplyAsync(Supplier)`, `completeAsync(Supplier)`,
`thenApplyAsync(Function)`, `CompletionStage.whenCompleteAsync(BiConsumer)` and `orTimeout(long, TimeUnit)` are
rejected before WAT generation.
