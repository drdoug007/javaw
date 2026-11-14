# T29 AtomicBoolean and AtomicReference

```kit
id: T29
slug: atomic-boolean-reference
title: link java.util.concurrent.atomic.AtomicBoolean and AtomicReference
tests: BytecodeAtomicBooleanReferenceTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicBoolean.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicReference.java
allow: docs/bytecode-atomic-boolean-reference.md
```

## Goal

Add `java.util.concurrent.atomic.AtomicBoolean` and `java.util.concurrent.atomic.AtomicReference<V>` to the bundled
runtime. The pinned libraries refer to `AtomicReference` from 85 classes and to `AtomicBoolean` from 63
(`runtime-target/target/audit/external-types.tsv`: Logback `COWArrayList`, Jackson `JsonGenerator` and the
`AtomicBoolean` (de)serializers, HikariCP `HikariDataSource`, Micrometer, Byte Buddy, ...). The member rows in
`member-references.tsv` are `AtomicReference.get()` (169 call sites), `compareAndSet` (62), `set` (44),
`<init>(Object)` (33), `<init>()` (30), `getAndSet` (20), `lazySet` (11), and `AtomicBoolean.get()` (65), `set` (50),
`compareAndSet` (43), `<init>(Z)` (42), `<init>()` (35), `getAndSet` (5), plus `equals`/`hashCode` inherited from
`Object`. The task links every Java 17 public member of both classes so that later libraries need no follow-up.

The JDK classes keep the value in a `volatile` field and update it through a `VarHandle`. Here the runtime is
**single-threaded**, exactly like the existing runtime `AtomicReferenceArray` (`docs/bytecode-atomic-reference-array.md`):
a plain private field, every access mode is a plain read or write, and the compare-and-set family compares and
then writes without any retry or spurious failure. No `volatile`, no `synchronized`, no `VarHandle`, no
`MethodHandles` (RULES.md).

## Members to implement

`java/util/concurrent/atomic/AtomicBoolean.java`:
`public class AtomicBoolean implements java.io.Serializable` with one field `private boolean value;` and

- `public AtomicBoolean(boolean initialValue)`, `public AtomicBoolean()` (value `false`)
- `public final boolean get()`, `public final void set(boolean newValue)`, `public final void lazySet(boolean newValue)`
- `public final boolean getAndSet(boolean newValue)`
- `public final boolean compareAndSet(boolean expectedValue, boolean newValue)`
- `@Deprecated(since="9") public boolean weakCompareAndSet(boolean expectedValue, boolean newValue)` (**not** final)
- `public boolean weakCompareAndSetPlain(boolean expectedValue, boolean newValue)` (**not** final)
- `public final boolean weakCompareAndSetVolatile(boolean expectedValue, boolean newValue)`
- `public final boolean weakCompareAndSetAcquire(boolean expectedValue, boolean newValue)`
- `public final boolean weakCompareAndSetRelease(boolean expectedValue, boolean newValue)`
- `public final boolean compareAndExchange(boolean expectedValue, boolean newValue)`
- `public final boolean compareAndExchangeAcquire(boolean expectedValue, boolean newValue)`
- `public final boolean compareAndExchangeRelease(boolean expectedValue, boolean newValue)`
- `public final boolean getPlain()`, `public final void setPlain(boolean newValue)`
- `public final boolean getOpaque()`, `public final void setOpaque(boolean newValue)`
- `public final boolean getAcquire()`, `public final void setRelease(boolean newValue)`
- `public String toString()` (**not** final)

`java/util/concurrent/atomic/AtomicReference.java`:
`public class AtomicReference<V> implements java.io.Serializable` with one field `private V value;` and

- `public AtomicReference(V initialValue)`, `public AtomicReference()` (value `null`)
- `public final V get()`, `public final void set(V newValue)`, `public final void lazySet(V newValue)`
- `public final V getAndSet(V newValue)`
- `public final boolean compareAndSet(V expectedValue, V newValue)`
- `@Deprecated(since="9") public final boolean weakCompareAndSet(V expectedValue, V newValue)`
- `public final boolean weakCompareAndSetPlain(V expectedValue, V newValue)`
- `public final boolean weakCompareAndSetVolatile(V expectedValue, V newValue)`
- `public final boolean weakCompareAndSetAcquire(V expectedValue, V newValue)`
- `public final boolean weakCompareAndSetRelease(V expectedValue, V newValue)`
- `public final V compareAndExchange(V expectedValue, V newValue)`
- `public final V compareAndExchangeAcquire(V expectedValue, V newValue)`
- `public final V compareAndExchangeRelease(V expectedValue, V newValue)`
- `public final V getAndUpdate(java.util.function.UnaryOperator<V> updateFunction)`
- `public final V updateAndGet(java.util.function.UnaryOperator<V> updateFunction)`
- `public final V getAndAccumulate(V x, java.util.function.BinaryOperator<V> accumulatorFunction)`
- `public final V accumulateAndGet(V x, java.util.function.BinaryOperator<V> accumulatorFunction)`
- `public final V getPlain()`, `public final void setPlain(V newValue)`
- `public final V getOpaque()`, `public final void setOpaque(V newValue)`
- `public final V getAcquire()`, `public final void setRelease(V newValue)`
- `public String toString()` (**not** final; every other `AtomicReference` method is final)

Both classes are in package `java.util.concurrent.atomic` (`package java.util.concurrent.atomic;` on the first line).
Neither class overrides `equals` or `hashCode`: the `Object` identity versions are what the JDK has.

## Members to leave out

Nothing public: the lists above are the complete Java 17 public API of both classes. Leave out the JDK's private
`serialVersionUID`, the `VarHandle VALUE` field and its static initializer (the runtime has no `VarHandle`). Do not
create or change any other class (`AtomicInteger`, `AtomicLong`, `AtomicMarkableReference`,
`AtomicReferenceFieldUpdater` and `AtomicReferenceArray` are other work; `kit guard` allows only this task's two
runtime files and its doc).

## JDK behaviour to match

`AtomicBoolean`:

- `toString()` is `Boolean.toString(get())`, so `"true"` or `"false"`; `"x" + new AtomicBoolean()` is `"xfalse"`.
- `getAndSet(v)` returns the old value and stores `v`.
- `compareAndSet(e, n)`: if `value == e`, store `n` and return `true`; otherwise return `false` and leave the value
  alone. `compareAndSet(true, true)` on `true` returns `true`. All five `weakCompareAndSet*` methods behave
  **exactly** like `compareAndSet` (never a spurious `false`).
- `compareAndExchange(e, n)` returns the **old** value (the "witness") and stores `n` only when the old value was `e`.
  On `true`: `compareAndExchange(false, true)` returns `true` (unchanged); `compareAndExchange(true, false)` returns
  `true` and the value becomes `false`. The `Acquire`/`Release` forms are identical.
- No method calls an overridable method. `weakCompareAndSet`, `weakCompareAndSetPlain` and `toString` are not final,
  and the test's `LoggedFlag` subclass overrides all three and logs each call: `compareAndSet`,
  `weakCompareAndSetVolatile/Acquire/Release`, every `compareAndExchange*`, `getAndSet` and the setters must leave the
  log **empty**, and `weakCompareAndSet` must not call `weakCompareAndSetPlain` (or the other way round). Write each
  body on the field (e.g. `if (value != expectedValue) return false; value = newValue; return true;`), or delegate
  only to a `final` method (e.g. `weakCompareAndSetVolatile` may `return compareAndSet(expectedValue, newValue);`).
  Never call `weakCompareAndSet`, `weakCompareAndSetPlain` or `toString` from inside the class.
- `equals`/`hashCode` are `Object`'s: `a.equals(a)` is `true`, two flags holding `true` are not equal.

`AtomicReference<V>`:

- `toString()` is `String.valueOf(get())`: `"null"` for `null`, otherwise the value's own `toString()`, called
  **once** per `AtomicReference.toString()` call (the test's `Tag` counts calls: `a + "," + a.toString()` gives
  `tag1,tag2`). A nested `AtomicReference` value prints as its value (`"deep"`).
- `compareAndSet`, every `weakCompareAndSet*` and every `compareAndExchange*` compare with `==` (**reference
  identity**), never `equals`. `new String("x")` and another `new String("x")` do not match; `null` matches `null`.
  The test's `Same` class has an `equals` that always returns `true` and counts its calls; the count must be `0`
  after all the `AtomicReference` calls (expected `equalsCalls=0`). The trailing `true,equalsCalls=1` in the expected
  output comes from the test calling `p.equals(q)` itself, not from your code.
- `compareAndExchange(e, n)` returns the old value (the same object, not a copy) and stores `n` only when
  `old == e`.
- `getAndSet(v)` returns the old value.
- The four function methods follow the JDK's compare-and-set retry loop **exactly**, because a function that
  changes the same `AtomicReference` while it runs makes the retries observable:

      V prev = get();
      for (;;) {
          V next = updateFunction.apply(prev);          // accumulate: accumulatorFunction.apply(prev, x)
          if (compareAndSet(prev, next)) return prev;   // updateAndGet / accumulateAndGet: return next
          prev = get();
      }

  So when the function itself calls `set(other)` on its first call, the first `compareAndSet(prev, next)` fails
  (the value is now `other`), the function is called a **second** time with `other`, and the result is stored then.
  Example (the test's `referenceUpdateRetries`): on `"a"`, `getAndUpdate(v -> { log v; if first call set(other);
  return v + "!"; })` logs `f(a) f(other)`, returns `other` and leaves `other!`. A function that sets an **equal but
  different** `new String(v)` also forces a retry; one that sets the **same** object it was given does not
  (one call). In the single-threaded runtime the JDK's `haveNext` flag is never true, so the loop above is exact.
- Argument order for the accumulate methods is `apply(prev, x)` (the current value first): on `"aaaa"`,
  `getAndAccumulate("x", (v, x) -> v + "-" + x)` stores `"aaaa-x"`.
- A function result of `null` is stored (`updateAndGet(v -> null)` returns `null` and the value becomes `null`).
- A `null` current value is passed to the function like any other value: on an empty `AtomicReference`,
  `getAndUpdate(v -> v + "!")` returns `null` (printed `null`) and stores `"null!"`, and `updateAndGet(v -> v.trim())`
  throws `NullPointerException` from inside the caller's lambda. Do not add a `null` check.
- If the function throws, the exception propagates unchanged and nothing is stored. A `null` function throws
  `NullPointerException` when `apply` is called (no explicit check is needed; the test checks only the type, since
  the JVM's helpful NPE message is not reproduced).
- `equals`/`hashCode` are `Object`'s.

Both: `instanceof java.io.Serializable` is `true`. There are no exceptions to throw and no messages to match apart
from those the caller's functions throw.

## Steps

1. Read the test `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeAtomicBooleanReferenceTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicReferenceArray.java`,
   the single-threaded sibling of this task, and copy its style.
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicBoolean.java`.
4. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/atomic/AtomicReference.java`.
5. Register both, in any order:
   `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/AtomicBoolean.java` and
   `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/AtomicReference.java`.
   Each adds a `paths.add("java/util/concurrent/atomic/....java");` line before the `ExceptionSupport` line of
   `BytecodeExceptionRuntime.java`.
6. `javaw-agent-kit/bin/kit check T29` until PASS.
7. Write `docs/bytecode-atomic-boolean-reference.md` (below), the report, then `kit finish T29`.

## Hints

- `AtomicReferenceArray.java` in the same directory already has every access mode as a plain access; this task is
  the same idea with one field instead of an array. Do **not** copy its `getAndUpdate`/`updateAndGet`/
  `getAndAccumulate`/`accumulateAndGet` bodies: they apply the function once and store without the compare-and-set
  retry, which gives the wrong result in `referenceUpdateRetries` and `referenceAccumulateRetries` (for example
  `a:a!,false,calls=1` instead of `other:other!,true,calls=2`).
- Write the retry loop with `compareAndSet(prev, next)` (final, so calling it is safe) or with the field directly.
- Use `java.util.function.UnaryOperator` and `java.util.function.BinaryOperator` (both exist in the runtime) with an
  import or the full name. `Boolean.toString(boolean)` and `String.valueOf(Object)` exist.
- `AtomicBoolean`: a `boolean` field is simplest; the JDK's `int` field (`1`/`0`) is not observable.
- Keep `weakCompareAndSet` and `weakCompareAndSetPlain` of `AtomicBoolean` non-final, and keep `toString` non-final
  in both classes. The test's subclasses override them. If one of them is `final`, every test in the class fails with
  `cannot override final instance method java/util/concurrent/atomic/AtomicBoolean.weakCompareAndSet(ZZ)Z` (or the
  matching `weakCompareAndSetPlain(ZZ)Z` / `toString()Ljava/lang/String;` name, with owner `AtomicBoolean` or
  `AtomicReference`). The fixture itself still compiles, because it is compiled against the real JDK. Everything else
  is `final`, as listed.
- Never write `volatile`, `synchronized`, `VarHandle` or `MethodHandles` (RULES.md; `kit guard` rejects them).
- Before the runtime files exist, every test fails with a message naming `java/util/concurrent/atomic/AtomicBoolean`
  or `java/util/concurrent/atomic/AtomicReference`: that only means the classes are missing or unregistered.
- During calibration a variant that copied `AtomicReferenceArray`'s single-shot `getAndUpdate` failed only
  `atomicReferenceUpdateRetries`, with `actual: 'a:a!,false,calls=1|...'`; `kit guard` was clean for the reference.
- Calibration: the reference passed kit check after 1 run (about 75 new lines: AtomicBoolean 28, AtomicReference 47).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

All eight tests compile and link the same single `FIXTURE`. A compile or link problem (a wrongly `final` method, a
missing member, a javac error in a runtime file) therefore fails all eight tests at once with the same message; fix
that one message instead of looking for a separate cause per test.

```kit-hints
atomicBooleanAccessors, atomicBooleanCompareAndSet, atomicBooleanSubclass, atomicReferenceAccessors, atomicReferenceComparesIdentity, atomicReferenceUpdateFunctions, atomicReferenceUpdateRetries, atomicReferenceSubclass: if the message names `java/util/concurrent/atomic/AtomicBoolean` or `java/util/concurrent/atomic/AtomicReference` (missing owner, unsupported, or `link a complete acyclic type hierarchy`), a class is missing or unregistered: run `javaw-agent-kit/bin/kit register java/util/concurrent/atomic/AtomicBoolean.java` and the same for `java/util/concurrent/atomic/AtomicReference.java`, and make sure the member it names exists with exactly that signature; if it says `cannot override final instance method java/util/concurrent/atomic/AtomicBoolean.<name>` or `cannot override final instance method java/util/concurrent/atomic/AtomicReference.toString()Ljava/lang/String;`, remove `final` from that method (only AtomicBoolean.weakCompareAndSet, AtomicBoolean.weakCompareAndSetPlain and both toString methods are non-final); if it says `Cannot build bytecode exception runtime`, javac rejected a runtime file: fix the line it quotes (package java.util.concurrent.atomic; no VarHandle).
booleanBasics, booleanAccessModes, booleanIdentityAndSerializable: AtomicBoolean() starts false; toString() is Boolean.toString(get()) ("true"/"false"); getAndSet returns the OLD value; every get*/set* access mode reads or writes the same private boolean field; do not override equals or hashCode.
booleanCompareAndSet, booleanCompareAndExchange: compareAndSet(e, n) and all five weakCompareAndSet* store n and return true only when value == e (never a spurious false); compareAndExchange*(e, n) return the OLD value and store n only when the old value was e.
booleanSubclassHooks: no AtomicBoolean method may call weakCompareAndSet, weakCompareAndSetPlain or toString (the subclass overrides and logs them; the log must stay empty until the test calls them itself); write each method on the field or delegate only to a final method (compareAndSet, compareAndExchange), and keep exactly those three non-final.
referenceBasics, referenceAccessModes: AtomicReference() starts null; getAndSet returns the old value; every access mode is a plain read or write of the one field.
referenceToString: AtomicReference.toString() is String.valueOf(get()): "null" for null, otherwise exactly one call of the value's toString() per call (tag1,tag2,calls=2).
referenceCompareAndSet, referenceCompareAndExchange, referenceNeverCallsEquals: compare with == (reference identity), NEVER equals(): new String("x") does not match another new String("x"), null matches null; compareAndExchange* return the old object itself; equalsCalls must be 0 after the atomic calls (the later equalsCalls=1 is the test's own p.equals(q)).
referenceUpdateFunctions: getAndUpdate/getAndAccumulate return the value BEFORE the update, updateAndGet/accumulateAndGet the value after; accumulate calls apply(prev, x) with the current value first; a null result is stored.
referenceUpdateExceptions: if the function throws, let the exception propagate and store nothing (value stays "keep"); a null function throws NullPointerException from the apply call; a null current value is passed to the function unchanged (empty.getAndUpdate(v->v+"!") returns null and stores "null!").
referenceUpdateRetries, referenceAccumulateRetries: the four function methods must loop `V prev = get(); for (;;) { V next = f.apply(prev); if (compareAndSet(prev, next)) return prev /* or next */; prev = get(); }` with an identity compare; a function that calls set() on the same reference forces another call with the new value (f(a) f(other)), and an exception thrown on a later call propagates after the earlier set() stayed in place.
referenceSubclass: only toString is non-final in AtomicReference; a subclass's toString override (cfg->on) must be what string concatenation prints, and super.toString() is String.valueOf(get()).
```

## Docs

Create `docs/bytecode-atomic-boolean-reference.md` with this skeleton (fill in the brackets):

    # Bytecode AtomicBoolean and AtomicReference

    Revision `atomic-boolean-reference-vN` links `java.util.concurrent.atomic.AtomicBoolean` and
    `java.util.concurrent.atomic.AtomicReference` in the bundled exception runtime. Like `AtomicReferenceArray`, this
    contract applies only to the **single-threaded** deployment: every access mode is a plain field access, and no
    Java Memory Model ordering is claimed.

    Contract:

    - <AtomicBoolean: accessors, getAndSet, compareAndSet/weak/exchange forms, toString, non-final hooks>
    - <AtomicReference: identity comparison, compareAndExchange witness, toString>
    - <getAndUpdate/updateAndGet/getAndAccumulate/accumulateAndGet: JDK retry loop, argument order, exceptions>

    Evidence: `BytecodeAtomicBooleanReferenceTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances), including library-style subclasses that override the non-final members and
    update functions that modify the reference they update.
