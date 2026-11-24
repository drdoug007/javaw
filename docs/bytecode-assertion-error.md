# Bytecode AssertionError

Revision `assertion-error-v321` links `java.lang.AssertionError` in the bundled exception runtime.

Contract:

- `AssertionError` extends `Error`. The no-arg constructor calls `super()`: the message is `null` and the
  cause stays unset, so a later `initCause(x)` succeeds. The private `AssertionError(String detail)`
  helper calls `super(detail)`; it is private so that `new AssertionError("x")` in source still resolves
  to the `Object` constructor. `AssertionError(Object detail)` calls `this(String.valueOf(detail))` —
  `"null"` for a `null` detail, `detail.toString()` called exactly once — and then, only when
  `detail instanceof Throwable`, calls the virtual `initCause((Throwable) detail)`, so a subclass
  overriding `initCause` sees one call and `getCause()` returns the detail; non-Throwable details leave
  the cause unset and a later `initCause` succeeds. The boolean, char, int, long, float and double
  constructors call `this(String.valueOf(detail))` with the usual `String.valueOf` text (`true`, `x`,
  `-7`, `-2147483648`, `1.5`, `-0.0`, `NaN`, ...); the cause is unset. `AssertionError(String message,
  Throwable cause)` calls `super(message, cause)`; the cause counts as set even when it is `null`, so a
  later `initCause` throws `IllegalStateException`. `getMessage`, `getCause`, `initCause` and `toString`
  are inherited from `Throwable` (`java.lang.AssertionError: <message>`, or just
  `java.lang.AssertionError` for a `null` message); it is an `Error`, not an `Exception`.

Known differences: `assert` statements are still rejected because `Class.desiredAssertionStatus()` is not
linked.

Evidence: `BytecodeAssertionErrorTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `Class.desiredAssertionStatus()` is rejected before WAT generation.
