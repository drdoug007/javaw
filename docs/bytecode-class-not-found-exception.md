# Bytecode ClassNotFoundException

Revision `class-not-found-exception-v325` links `java.lang.ClassNotFoundException` and the
`ReflectiveOperationException(String, Throwable)` / `(Throwable)` constructors in the bundled exception runtime.

Contract:

- All three `ClassNotFoundException` constructors set the cause (to `null` for the first two, exactly like
  JDK 21), so `initCause(...)` on any `ClassNotFoundException` — including `initCause(null)` — throws
  `IllegalStateException` with the exception itself as cause and leaves the cause unchanged. The no-arg
  constructor uses `super((Throwable) null)` and the one-arg constructor uses `super(s, null)`.
- `getException()` is `return super.getCause();`, so a subclass override of `getCause()` does not change
  what `getException()` returns and is not invoked by it. Nothing else overrides `toString`/`getMessage`/
  `getCause`.
- `ReflectiveOperationException(String, Throwable)` and `(Throwable)` set the cause (so `initCause` then
  throws); the existing `()` and `(String)` constructors do not. `new ReflectiveOperationException(cause)`
  has message `cause.toString()`.
- `ClassNotFoundException` is a checked exception under `ReflectiveOperationException`: not a
  `RuntimeException`, not an `Error`.

Known differences: serialization (`readObject`/`writeObject`/`serialPersistentFields`) is not linked. The
runtime never throws `ClassNotFoundException` itself; there is no `Class.forName`.

Evidence: `BytecodeClassNotFoundExceptionTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances). `Class.forName(String)` and `ClassNotFoundException(Throwable)` are rejected
before WAT generation.
