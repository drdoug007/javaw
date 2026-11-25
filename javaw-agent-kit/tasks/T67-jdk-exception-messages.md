# T67 JDK exception messages for initCause and Math exact arithmetic

```kit
id: T67
slug: jdk-exception-messages
title: JDK messages for Throwable.initCause and Math exact arithmetic
tests: BytecodeJdkExceptionMessagesTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/Throwable.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/Math.java
allow: docs/bytecode-jdk-exception-messages.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-jdk-exception-messages-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeJdkExceptionMessagesTest.java`.

## Goal

The bundled runtime throws the right exception types from `Throwable.initCause` and the `Math` exact methods,
but with a `null` message where the JDK has a fixed text. Libraries log and wrap these messages, and the
differential tests of later tasks cannot print them. The pinned libraries call `Math.toIntExact(J)I` (7 sites,
e.g. spring-data-jpa `PartTreeJpaQuery$QueryPreparer.restrictMaxResultsIfNecessary`), `Math.multiplyExact(JJ)J`
(5, spring-core `DataSize.of`), `Math.addExact(II)I` (2) and `Math.multiplyExact(II)I` (1, spring-boot
`PeriodStyle$1.parse`) and `Throwable.initCause` (2, istack-commons `Logger.logException`) in
`runtime-target/target/audit/member-references.tsv`. This task only changes messages; no member is added.

## Members to implement

No new members. Change only the messages in existing methods.

`Throwable.java`, method `initCause(Throwable value)`. JDK 21 order: the "already set" check comes first,
then the self check:

- cause already set (`cause != this`): `throw new IllegalStateException("Can't overwrite cause with " +
  (value == null ? "a null" : value.toString()), this);` (inline; do not call `java.util.Objects` from
  `Throwable`.)
- `value == this`: `throw new IllegalArgumentException("Self-causation not permitted", this);` (already right).

`Math.java`, every `throw new ArithmeticException()`:

| method | JDK 21 message |
|---|---|
| `addExact(II)`, `subtractExact(II)`, `incrementExact(I)`, `decrementExact(I)`, `negateExact(I)`, `toIntExact(J)`, `multiplyExact(II)` | `"integer overflow"` |
| `addExact(JJ)`, `subtractExact(JJ)`, `incrementExact(J)`, `decrementExact(J)`, `negateExact(J)`, `multiplyExact(JJ)`, `multiplyExact(JI)` | `"long overflow"` |
| `absExact(I)` | `"Overflow to represent absolute value of Integer.MIN_VALUE"` |
| `absExact(J)` | `"Overflow to represent absolute value of Long.MIN_VALUE"` |

Pitfalls in the current code: `incrementExact`/`decrementExact` delegate to `addExact`/`subtractExact`, which
is fine once those have the message. `absExact` delegates to `negateExact`, which would give `"integer
overflow"`; it must throw its own message. `multiplyExact(II)` delegates to `toIntExact`, which gives
`"integer overflow"` as the JDK does.

`floorDiv`/`floorMod` by zero: the JDK message is `"/ by zero"`, from the `a/b` / `a%b` inside. The
compiler's division already throws that message, so `divisionByZeroMessages` passes before any edit; it is a
regression guard. Do not touch `floorDiv`/`floorMod`.

`StrictMath` delegates every exact method to `Math`, so it needs no change and is not in the allow list.

## Members to leave out

- `Math.divideExact`, `ceilDiv`, `ceilMod`, `ceilDivExact`, `floorDivExact`, `unsignedMultiplyHigh` (JDK 18+):
  not referenced by the pinned libraries; do not add them (the test checks `divideExact(II)I` and
  `ceilDiv(JJ)J` with `rejectsUnusedCall`).
- Any other message or behaviour change in `Throwable` (constructors, `addSuppressed`, `toString`).

## JDK behaviour to match

- `initCause` on a throwable whose cause is already set (by `initCause`, also `initCause(null)`, or by a
  cause-taking constructor, also with a `null` cause) throws `IllegalStateException` with message
  `"Can't overwrite cause with " + (value == null ? "a null" : value.toString())` and cause = the receiver.
  The new value's own `toString()` override is used.
- `initCause(this)` with no cause set throws `IllegalArgumentException("Self-causation not permitted")` with
  cause = the receiver, and the cause stays unset (a later `initCause(x)` works). With a cause already set,
  `initCause(this)` throws the `IllegalStateException` ("Can't overwrite cause with " + this.toString()).
- Exact methods: messages from the table above, `getCause()` is `null`. Non-overflowing calls return the result
  unchanged.
- `floorDiv`/`floorMod` (int, long, long-int, also through `StrictMath`) by zero: `"/ by zero"`.

## Steps

1. Read the test.
2. Edit `initCause` in `Throwable.java` and the `ArithmeticException` messages in `Math.java`.
3. `javaw-agent-kit/bin/kit check T67` until PASS.
4. Write `docs/bytecode-jdk-exception-messages.md` (below), the report, then `javaw-agent-kit/bin/kit finish T67`.

## Hints

- Do not run `kit register`: `Throwable` and `Math` are already in the fixed runtime list.
- Do not change the arithmetic, only the `new ArithmeticException(...)` arguments (and `absExact`, which needs
  its own check: `if(value==Integer.MIN_VALUE)throw new ArithmeticException("Overflow to represent absolute
  value of Integer.MIN_VALUE");`).
- `Throwable` is the root of every exception: change only the one `throw` line in `initCause`.
- `String + null` concatenation would print `null`; the JDK prints `a null`.
- The reference solution is 1 line in `Throwable.java` and 8 message edits plus 2 `absExact` guards in
  `Math.java`; the edits are mechanical. Find every `new ArithmeticException()` in `Math.java`: after your
  edit none should be left.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
initCauseMessages: the IllegalStateException message must be "Can't overwrite cause with " + (value==null?"a null":value.toString()); keep the cause-already-set check before the self-causation check.
exactArithmeticMessages: int methods use "integer overflow", long methods (including multiplyExact(long,int)) "long overflow"; absExact needs its own "Overflow to represent absolute value of Integer.MIN_VALUE"/"Long.MIN_VALUE" message instead of delegating to negateExact.
divisionByZeroMessages: this passed before your edits; undo any change to floorDiv/floorMod and the plain a/b, a%b division.
leavesOutNewerExactMethods: do not add divideExact, ceilDiv or other JDK 18+ methods to Math.java.
```

## Docs

Create `docs/bytecode-jdk-exception-messages.md` with this skeleton (fill in the brackets):

    # Bytecode JDK exception messages

    Revision `jdk-exception-messages-vN` gives `Throwable.initCause` and the `java.lang.Math` exact methods
    in the bundled exception runtime the JDK 21 exception messages.

    Contract:

    - <initCause messages and order of checks; integer/long overflow messages; absExact messages; floorDiv/floorMod
      "/ by zero"; StrictMath delegates; no new members>

    Evidence: `BytecodeJdkExceptionMessagesTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances).
