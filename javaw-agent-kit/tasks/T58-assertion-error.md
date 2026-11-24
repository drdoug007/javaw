# T58 AssertionError

```kit
id: T58
slug: assertion-error
title: link java.lang.AssertionError and all its constructors
tests: BytecodeAssertionErrorTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/AssertionError.java
allow: docs/bytecode-assertion-error.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-assertion-error-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeAssertionErrorTest.java`.

## Goal

Add `java.lang.AssertionError` to the bundled bytecode runtime. It does not exist there yet (the source compiler
in `javaw-core` has its own `AssertionError`, which does not help bytecode input). The pinned libraries throw it
from many unreachable-branch guards (`runtime-target/target/audit/member-references.tsv`, summed `count`):
`<init>()V` 1162 (Hibernate `InFlightMetadataCollectorImpl.processSecondPasses`, H2
`Set.addOrUpdateSetting`, Spring `org/springframework/asm/ClassReader.readCode`), `<init>(Ljava/lang/Object;)V`
155 (H2 `Bnf.addRule`, Hibernate `CreateKeySecondPass.doSecondPass`, Jackson `FftMultiplier.fromFftVector`),
`<init>(I)V` 3 (H2 `FileStore.acceptChunkOccupancyChanges`), `<init>(J)V` 3 (H2 `MVMap.tryLock`) and `<init>(Z)V` 1
(H2 `Select.queryWithoutCache`). The remaining public constructors (`char`, `float`, `double`,
`(String, Throwable)`) have no audit rows; they are added so the class matches the JDK API as a whole.

## Members to implement

One file, `java/lang/AssertionError.java`: `public class AssertionError extends Error`

- `public AssertionError()`
- `private AssertionError(String detail)` (private helper, as in the JDK; must not be public: `new
  AssertionError("x")` in Java source picks the `Object` constructor and must keep doing so)
- `public AssertionError(Object detail)`
- `public AssertionError(boolean detail)`, `public AssertionError(char detail)`, `public AssertionError(int detail)`,
  `public AssertionError(long detail)`, `public AssertionError(float detail)`, `public AssertionError(double detail)`
- `public AssertionError(String message, Throwable cause)`

No methods. `getMessage`, `getCause`, `initCause` and `toString` are inherited from `Throwable`.

## Members to leave out

- `Class.desiredAssertionStatus()` and every other assertion-status API (`ClassLoader.setDefaultAssertionStatus`,
  ...). javac compiles an `assert` statement to a `static final boolean $assertionsDisabled` field initialised from
  `Fixture.class.desiredAssertionStatus()`; that method is not linked, so classes containing `assert` statements
  are still rejected. Supporting them is a later task (it needs a host contract for `-ea`). The test checks that
  `desiredAssertionStatus` stays unlinked and its fixtures contain no `assert` statement. Do not edit `Class.java`.
- Do not edit `Throwable.java`, `Error.java` or any other runtime file.

## JDK behaviour to match

- `AssertionError()`: message `null`, cause `null`, and the cause is still unset, so a later `initCause(x)`
  succeeds.
- `AssertionError(Object detail)`: message is `String.valueOf(detail)` (`"null"` for `null`; calls
  `detail.toString()` exactly once). If `detail instanceof Throwable`, it then calls the virtual
  `initCause((Throwable) detail)` (a subclass overriding `initCause` sees one call), so `getCause()` is the detail
  and a second `initCause` throws `IllegalStateException`. Otherwise `initCause` is not called and the cause stays
  unset (a later `initCause` succeeds). The message for a Throwable detail is its `toString()`, e.g.
  `java.lang.IllegalArgumentException: bad`.
- Primitive constructors: message `String.valueOf(detail)` (`true`, `x`, `-7`, `-2147483648`, `1.5`, `-0.0`, `NaN`,
  `100.0`, ...), cause unset.
- `AssertionError(String message, Throwable cause)`: `super(message, cause)`. The cause is set even when it is
  `null`, so a later `initCause` throws `IllegalStateException`.
- `toString()` is the inherited `java.lang.AssertionError: <message>` or just `java.lang.AssertionError` when the
  message is `null`. It is an `Error`, not an `Exception`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeAssertionErrorTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/lang/Error.java` and `Throwable.java` (which
   constructors exist and how the cause is stored).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/lang/AssertionError.java`.
4. Register it (never edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/lang/AssertionError.java`.
5. `javaw-agent-kit/bin/kit check T58` until PASS.
6. Write `docs/bytecode-assertion-error.md` (below), the report, then `javaw-agent-kit/bin/kit finish T58`.

## Hints

- Each constructor is one line: the primitive and `Object` constructors call `this(String.valueOf(detail))`, the
  private `String` constructor calls `super(detail)`, the two-argument one calls `super(message, cause)`.
- The `Object` constructor sets the cause with `initCause((Throwable) detail)` after `this(...)`; do not pass the
  detail to `super(message, cause)` (that skips the virtual `initCause` call the test counts) and do not call
  `initCause` for non-Throwable details (that would set the cause to `null` and make a later `initCause` throw).
- `String.valueOf` overloads for every primitive exist in the runtime `String`.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
noDetail, objectDetail, primitives, messageAndCause: if FIX THE RUNTIME says `link a complete acyclic type hierarchy` or `link the descriptor class` naming AssertionError, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/lang/AssertionError.java`. If it names a constructor such as `<init>(C)V`, add that constructor exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
noDetail: AssertionError() must call super() so the message is null and the cause stays unset (initCause then works); AssertionError extends Error.
objectDetail, primitives: the message is String.valueOf(detail): "null" for a null Object, the detail's toString() called once, and the usual String.valueOf text for primitives.
throwableDetail, callPattern: AssertionError(Object) calls this(String.valueOf(detail)) and then, only if detail instanceof Throwable, the virtual initCause((Throwable) detail). Non-Throwable details leave the cause unset. Expected callPattern `x|java.lang.RuntimeException:later|java.lang.AssertionError: x;||initCause;|java.lang.RuntimeException: r|java.lang.RuntimeException:r|Probe: java.lang.RuntimeException: r`.
messageAndCause: AssertionError(String, Throwable) must call super(message, cause); a null cause still counts as set, so initCause afterwards throws IllegalStateException.
leavesOutAssertionStatus: this fails when Class.desiredAssertionStatus() has become linked; do not edit Class.java.
```

## Docs

Create `docs/bytecode-assertion-error.md` with this skeleton (fill in the brackets):

    # Bytecode AssertionError

    Revision `assertion-error-vN` links `java.lang.AssertionError` in the bundled exception runtime.

    Contract:

    - <no-arg: null message, cause unset; Object: String.valueOf message, Throwable detail becomes the cause via
      initCause; primitives: String.valueOf message; (String, Throwable): message and cause, cause counts as set>

    Known differences: `assert` statements are still rejected because `Class.desiredAssertionStatus()` is not
    linked.

    Evidence: `BytecodeAssertionErrorTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `Class.desiredAssertionStatus()` is rejected before WAT generation.
