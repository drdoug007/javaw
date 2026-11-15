# T30 AtomicReferenceArray update and accumulate retries

```kit
id: T30
slug: atomic-array-updates
title: AtomicReferenceArray update and accumulate functions retry like JDK 21
tests: BytecodeAtomicReferenceArrayUpdatesTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicReferenceArray.java
allow: docs/bytecode-atomic-reference-array.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-atomic-array-updates-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeAtomicReferenceArrayUpdatesTest.java`.

This is a **fix** task: `AtomicReferenceArray.java` already exists and is already registered. You change four method
bodies in it and add a section to the existing doc `docs/bytecode-atomic-reference-array.md`. You create no file and
run no `kit register`.

## Goal

The runtime `java.util.concurrent.atomic.AtomicReferenceArray` (contract in `docs/bytecode-atomic-reference-array.md`)
is used by the pinned closure in Jackson Core `BufferRecycler`, Jackson Databind `PrivateMaxEntriesMap`, H2
`TraceSystem` and `TransactionStore`, and Spring Core `ConcurrentLruCache$ReadOperations`
(`runtime-target/target/audit/external-types.tsv`). The pinned call sites (`member-references.tsv`) use `<init>(I)`,
`get`, `set`, `lazySet`, `getAndSet`, `compareAndSet` and `length`; none of them calls the update functions today.
The four update methods, however, still apply the function **once** and store its result unconditionally. The JDK 21
methods loop with `weakCompareAndSetVolatile(i, prev, next)`: when the function itself changes the element, the JDK
discards the result and calls the function again with the new element. The T28 `AtomicInteger`/`AtomicLong` and T29
`AtomicReference` runtime classes already do this; this task makes `AtomicReferenceArray` consistent with them and
with the JDK.

## Members to implement

Change only the bodies of these four existing methods in
`javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicReferenceArray.java`. Keep
their signatures exactly as they are:

- `public final E getAndUpdate(int index, java.util.function.UnaryOperator<E> update)`
- `public final E updateAndGet(int index, java.util.function.UnaryOperator<E> update)`
- `public final E getAndAccumulate(int index, E value, java.util.function.BinaryOperator<E> accumulator)`
- `public final E accumulateAndGet(int index, E value, java.util.function.BinaryOperator<E> accumulator)`

## Members to leave out

- Do not add, remove or rename any member. Every other member (constructors, `length`, `get`/`set`/`lazySet`, the
  `Plain`/`Opaque`/`Acquire`/`Release` accessors, `getAndSet`, `compareAndSet`, all `weakCompareAndSet*`,
  `compareAndExchange*`, `toString`) stays exactly as it is. The non-kit test `BytecodeAtomicReferenceArrayTest`
  (run by the full suite in `kit finish`) checks them; `otherMembersUnchanged` checks them again.
- Do not make the class or `toString` final: the test's subclass `Slots` overrides `toString`.
- Do not edit `AtomicReference.java`, `AtomicInteger.java`, `AtomicLong.java` or any other runtime file.

## JDK behaviour to match

The JDK 21 source of `getAndUpdate` is (the other three differ only as noted):

    E prev = get(i), next = null;
    for (boolean haveNext = false;;) {
        if (!haveNext)
            next = updateFunction.apply(prev);              // accumulate: accumulatorFunction.apply(prev, x)
        if (weakCompareAndSetVolatile(i, prev, next))
            return prev;                                    // updateAndGet / accumulateAndGet: return next
        haveNext = (prev == (prev = get(i)));
    }

In this single-threaded runtime `weakCompareAndSetVolatile` never fails spuriously, so after a failed compare the
re-read element is always a different reference and `haveNext` is always `false`. The loop you write is therefore
the same as in `AtomicReference.java`:

    E prev = get(index);
    for (;;) {
        E next = update.apply(prev);                        // accumulate: accumulator.apply(prev, value)
        if (compareAndSet(index, prev, next)) return prev;  // ...AndGet forms: return next
        prev = get(index);
    }

What that means, with the values the test checks (all verified on JDK 21):

- **Identity, not equals.** The compare is `array[index] == prev`. If the function stores `new String(v)` (equal
  text, different object) into the same index, that counts as a change: on `["k"]`,
  `updateAndGet(0, v -> { if (first call) a.set(0, new String(v)); return v + calls; })` calls the function twice
  and gives `k2:[k2],calls=2`.
- **A change by the function forces another call with the new element.** On `["a", "b"]`,
  `getAndUpdate(0, f)` where `f` sets index 0 to `other` on its first call and returns `v + "!"` logs
  `f(a) f(other)`, returns `other` (the same object) and leaves `[other!, b]`. The loop repeats as many times as the
  function keeps changing the element: on `["x", "y"]`, `updateAndGet(1, u)` where `u` sets index 1 to `v + calls` on
  its first two calls and returns `v + "?"` logs `u(y) u(y1) u(y12)` (3 calls), returns `y12?` and leaves
  `[x, y12?]`.
- **The same reference, or another index, does not.** If the function sets the element to the very reference it was
  given (or sets it to something else and back again), or changes only other indexes, its result is stored after one
  call: on `["p", "q", "r"]`, `getAndUpdate(0, v -> { a.set(0, p); return "P"; })` returns `p` and leaves
  `[P, q, r]` with one call.
- **Nested updates.** On `["a"]`, an outer `getAndUpdate(0, ...)` whose first call runs an inner
  `updateAndGet(0, w -> w + "i")` logs `outer(a) inner=in(a) ai outer(ai)`, returns `ai` and leaves `[aio]`.
- **Accumulate passes `(current element, x)`**, element first, and passes the same `x` on every retry: on
  `["a", "b"]`, `getAndAccumulate(1, "x", g)` where `g` sets index 1 to `"c"` on its first call logs
  `g(b,x) g(c,x)`, returns `c` and leaves `[a, cx]`. A `null` element or a `null` `x` is passed through unchanged,
  and a `null` result is stored. On `new AtomicReferenceArray<Object>(3)` (all `null`), `accumulateAndGet(2, "y", h)`
  where `h` sets index 2 to the Integer call count on its first two calls and returns `v + "|" + x` logs
  `h(null,y) h(1,y) h(2,y)`, returns `2|y` and leaves `[null, null, 2|y]`.
- **Exceptions.** An exception from the function propagates unchanged, and nothing is stored by that call. Anything
  the function itself stored on an earlier call stays: on `["a", "b"]`, `accumulateAndGet(0, "z", k)` where `k`
  sets index 0 to `"c"` and returns `v` on its first call, then throws `IllegalStateException("stop:" + v)`, logs
  `k(a) k(c)`, throws `IllegalStateException` with message `stop:c` and leaves `[c, b]`. Likewise on `[c, b]`,
  `getAndUpdate(1, m)` where `m` sets index 1 to `"d"` and returns `"ignored"` on its first call, then throws
  `UnsupportedOperationException(v)`, logs `m(b) m(d)`, throws `UnsupportedOperationException` with message `d` and
  leaves `[c, d]` (`"ignored"` is never stored). A function that throws on its first call leaves the element
  unchanged.
- **Null function.** A `null` function throws `NullPointerException` from the `apply` call (no message is checked,
  do not add `Objects.requireNonNull`), and the element stays unchanged. A `null` element is passed to the function
  as it is: on `["keep", null]`, `updateAndGet(1, v -> v.trim())` throws `NullPointerException` from inside the
  lambda and stores nothing, and then `getAndUpdate(1, v -> v + "!")` returns `null` and leaves `[keep, null!]`.
- **Index first.** The index is checked by the first `get(index)`, before the function is called:
  `getAndUpdate(2, f)` on a length-2 array throws `ArrayIndexOutOfBoundsException` with message
  `Index 2 out of bounds for length 2` (`Index -1 ...`, `Index 5 ...`, `Index -2147483648 ...` likewise) and `f` is
  never called; a `null` function with a bad index also throws `ArrayIndexOutOfBoundsException`. The runtime's
  array access already throws exactly this, so read the element with `get(index)` (or `array[index]`) before calling
  the function.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeAtomicReferenceArrayUpdatesTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicReference.java`
   (lines with `getAndUpdate`, `updateAndGet`, `getAndAccumulate`, `accumulateAndGet`): that is the loop to copy,
   with an extra `index` argument.
3. Open `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicReferenceArray.java`.
   Replace the body of each of the four methods under "Members to implement" with the loop from "JDK behaviour to
   match". Use `get(index)` and `compareAndSet(index, prev, next)` (both are `final` methods of the same class).
   `getAndUpdate` and `getAndAccumulate` return `prev`; `updateAndGet` and `accumulateAndGet` return `next`.
4. No registration is needed (the file is already registered). Do not run `kit register`.
5. `javaw-agent-kit/bin/kit check T30` until PASS.
6. Update `docs/bytecode-atomic-reference-array.md` (see Docs), write the report, then
   `javaw-agent-kit/bin/kit finish T30`.

## Hints

- This is a four-method change of about 20 lines. Do not rewrite the file with `Write`; edit the four methods with
  small `Edit`s and leave every other line alone.
- Declare `prev` and `next` with type `E`. Calling `get(index)` avoids writing a new `(E)` cast.
- Before the fix, every test fails with `FIX THE RUNTIME: Wasm execution differs`; for example
  `getAndUpdateRetries` gives `a:[a!, b],false,calls=1|log=f(a) ` instead of
  `other:[other!, b],true,calls=2|log=f(a) f(other) `. That is the bug this task fixes, not a missing class.
- Do not compare with `equals` or `Objects.equals`: `equalButNotSame` needs a retry after `new String(v)` replaced an
  equal string, and `sameReferenceAndOtherSlots` needs no retry after the same reference was stored again.
- Do not call the function before `get(index)`: `indexChecksBeforeFunction` checks that a bad index throws without
  calling it (`calls=0`).
- Do not catch exceptions from the function, and do not store anything before the function returned.
- The Wasm runner stops at the **first** failing fixture of each `@Test` method, so after a fix a different fixture of
  the same test may fail next. That is progress, not a regression.
- Never write the lowercase words `volatile` or `synchronized` in the file (comments included). The existing method
  name `weakCompareAndSetVolatile` is fine.
- The doc says the compare-and-set family works "without retry loops"; that sentence is wrong after this task.
  Update the doc as described under Docs.
- During calibration the unchanged runtime failed all five tests with `Wasm execution differs` (first failures:
  `getAndUpdateRetries`, `nestedUpdate`, `getAndAccumulateRetries`, `throwAfterRetry`, `subclassUpdates`), the
  non-kit `BytecodeAtomicReferenceArrayTest` still passed with the fix, and `kit guard` was clean.
- Calibration: the reference passed kit check after 1 run (about 16 new lines replacing 4).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

All five tests compile and link the same single `FIXTURE`. A compile or link problem therefore fails all five tests
at once with the same message.

```kit-hints
updateFunctionsRetryWhenTheElementChanges, updateFunctionsKeepTheirResultWhenTheElementIsTheSameReference, accumulateFunctionsRetryWithTheSameArgument, exceptionsLeaveTheElementAsTheFunctionLeftIt, subclassAndUnchangedMembers: the class is already present and registered, so a message naming a missing class or `link a complete acyclic type hierarchy: java/util/concurrent/atomic/AtomicReferenceArray` means the file no longer compiles or a member was renamed: restore the original signatures (`getAndUpdate(int,UnaryOperator)`, `updateAndGet(int,UnaryOperator)`, `getAndAccumulate(int,Object,BinaryOperator)`, `accumulateAndGet(int,Object,BinaryOperator)`) and keep the class and `toString` non-final; if it says `Cannot build bytecode exception runtime`, fix the javac error it quotes in AtomicReferenceArray.java. Never run `kit register` for this task.
getAndUpdateRetries, updateAndGetRetries, equalButNotSame: the function is called once and its result stored unconditionally; use `E prev = get(index); for (;;) { E next = update.apply(prev); if (compareAndSet(index, prev, next)) return prev; prev = get(index); }` (updateAndGet returns next). Expected `other:[other!, b],true,calls=2|log=f(a) f(other) `.
sameReferenceAndOtherSlots, nestedUpdate: compare the element with `==` against the exact reference passed to the function (compareAndSet already does this); storing the same reference again or changing another index must NOT cause a second call, while a nested update of the same index must (expected `ai:[aio],calls=2`).
getAndAccumulateRetries, accumulateAndGetRetries, accumulateNullArguments: getAndAccumulate/accumulateAndGet need the same retry loop with `accumulator.apply(prev, value)`: current element FIRST, then the unchanged `value` argument on every retry; null elements, null `value` and null results are passed and stored as they are.
throwAfterRetry, throwFirstAndNullFunctions: let the function's exception propagate (no try/catch) and store only after the function returned; what the function itself stored on an earlier call stays (`[c, b]`); a null function throws NullPointerException just by calling `apply`.
indexChecksBeforeFunction: read the element with `get(index)` BEFORE calling the function, so a bad index throws `ArrayIndexOutOfBoundsException: Index 2 out of bounds for length 2` with `calls=0`, even when the function is null.
subclassUpdates: the subclass's `bump` calls the inherited getAndUpdate with a function that calls `set` on the same index; it needs the retry loop (`bump(a) bump(aa)`), and `toString` must stay non-final so `slots[...]` is printed.
otherMembersUnchanged: only the four update methods may change; compareAndSet, weakCompareAndSetVolatile, compareAndExchange, getAndSet, lazySet, setPlain and toString must behave as before (identity compare, witness returned by compareAndExchange).
```

## Docs

Do not create a new doc. Edit the existing `docs/bytecode-atomic-reference-array.md`:

1. In its first paragraph, replace "the compare-and-set/exchange families compare by reference identity without retry
   loops" with "the compare-and-set/exchange families compare by reference identity".
2. In the Contract bullet that starts "Out-of-range indexes raise", replace "callback exceptions propagate without
   storing a value" with "callback exceptions propagate without storing the function's result". Leave the other
   Contract bullets as they are, and add this bullet directly before that "Out-of-range indexes" bullet (fill in the
   brackets):

        - Revision `atomic-array-updates-vN`: `getAndUpdate`/`updateAndGet`/`getAndAccumulate`/`accumulateAndGet`
          follow the JDK 21 retry loop: <identity compare, re-call with the new element when the function changed
          it, accumulate argument order, exceptions leave what the function stored, index checked before the call>.

3. Append this sentence to the Evidence paragraph: "`BytecodeAtomicReferenceArrayUpdatesTest` (agent kit T30)
   compares update and accumulate functions that change, restore or nest updates of the element they update, and a
   library-style subclass."

Leave the original revision `atomic-reference-array-v253` and the closing paragraph unchanged. `kit finish` does not
rewrite `atomic-array-updates-vN` in this file; the reviewer sets the number.
