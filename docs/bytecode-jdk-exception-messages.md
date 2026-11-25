# Bytecode JDK exception messages

Revision `jdk-exception-messages-v329` gives `Throwable.initCause` and the `java.lang.Math` exact methods
in the bundled exception runtime the JDK 21 exception messages.

Contract:

- `initCause`: when a cause is already set — by a prior `initCause` (including `initCause(null)`) or by a
  cause-taking constructor (including one that passed a `null` cause) — the call throws
  `IllegalStateException` with the receiver as cause and message `"Can't overwrite cause with " +
  (value == null ? "a null" : value.toString())`; the new value's own `toString()` override is used, and a
  `null` value prints `a null`. `initCause(this)` with no cause set throws
  `IllegalArgumentException("Self-causation not permitted")` with the receiver as cause and leaves the cause
  unset, so a later `initCause` succeeds. The cause-already-set check runs **before** the self-causation
  check, so `initCause(this)` after any cause is set throws the `IllegalStateException` instead.
- Exact arithmetic: `addExact`, `subtractExact`, `incrementExact`, `decrementExact`, `negateExact`,
  `toIntExact` and `multiplyExact(int,int)` (via `toIntExact`) throw `ArithmeticException("integer
  overflow")`; the `long` overloads and `multiplyExact(long,int)` (via `multiplyExact(long,long)`) throw
  `ArithmeticException("long overflow")`. `absExact(int|long)` performs its own check for `Integer.MIN_VALUE`
  / `Long.MIN_VALUE` and throws `ArithmeticException("Overflow to represent absolute value of
  Integer.MIN_VALUE")` / `"...Long.MIN_VALUE"`, rather than delegating to `negateExact` (which would give a
  plain overflow message). Every one of these exceptions has `getCause() == null`; non-overflowing calls
  return the result unchanged.
- `floorDiv`/`floorMod` (int, long, long-int, also through `StrictMath`) by zero throw
  `ArithmeticException("/ by zero")` from the internal `a/b` / `a%b` division. The compiler's division already
  produced that message, so this task changes nothing there; it is a regression guard.
- `StrictMath` delegates every exact method to `Math`, so it needs no change.
- No new members are added: `divideExact`, `ceilDiv`, `ceilMod`, `ceilDivExact`, `floorDivExact` and
  `unsignedMultiplyHigh` (JDK 18+) remain absent.

Evidence: `BytecodeJdkExceptionMessagesTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances).
