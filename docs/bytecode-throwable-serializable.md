# Bytecode Throwable Serializable

Revision `throwable-serializable-v328` makes `java.lang.Throwable` in the bundled exception runtime implement
`java.io.Serializable`, like the JDK.

Contract:

- Every `Throwable` subclass is `instanceof java.io.Serializable`: `Throwable`, `Exception`, `RuntimeException`,
  `Error`, runtime exception subclasses (`IllegalStateException`, ...), and user exceptions, including ones that
  redeclare `implements Serializable` or implement it through a sub-interface. Plain user classes, `new Object()`
  and `null` are not.
- Exceptions assign to `Serializable` variables, store in `Serializable[]`, satisfy `<T extends Serializable>`
  generic bounds, and cast back through `checkcast` to `Throwable` / concrete exception types and rethrow.
- `serialVersionUID` (`-3042686055658047285L`, the JDK value) is declared on `Throwable` only; `Exception`,
  `RuntimeException`, `Error` and other runtime classes are untouched.
- No serialization hooks: `java.io.ObjectOutputStream`/`ObjectInputStream` are not in the runtime, so
  `writeObject`, `readObject` and `readResolve` are deliberately absent.
- Messages, causes, `toString`, suppressed exceptions and `initCause` semantics are unchanged.

Evidence: `BytecodeThrowableSerializableTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances).
