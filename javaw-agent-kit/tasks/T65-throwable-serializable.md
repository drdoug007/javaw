# T65 Throwable implements Serializable

```kit
id: T65
slug: throwable-serializable
title: make java.lang.Throwable implement java.io.Serializable
tests: BytecodeThrowableSerializableTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/Throwable.java
allow: docs/bytecode-throwable-serializable.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-throwable-serializable-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeThrowableSerializableTest.java`.

## Goal

In the JDK `java.lang.Throwable implements java.io.Serializable`, so every exception and error is `Serializable`.
The bundled runtime `Throwable` declares no interface, so `e instanceof Serializable` is `false` in Wasm and a
`checkcast java/io/Serializable` on an exception throws `ClassCastException`. `java/io/Serializable` is named by
1119 pinned classes (`runtime-target/target/audit/external-types.tsv`, e.g. logback `ch/qos/logback/classic/Level`,
`ch/qos/logback/classic/Logger`), and libraries that store or pass exceptions as `Serializable` (logging events,
Hibernate/Spring exception wrappers that implement `Serializable` again) need the JDK hierarchy. The pinned
classes also call `Throwable.addSuppressed` (658 sites), `getMessage` (126) and `getCause` (100) in
`member-references.tsv`; those must keep working exactly as before. `java/io/Serializable` already exists in the
runtime (`exception-runtime/java/io/Serializable.java`, an empty interface) and is always compiled together with
`Throwable`, so no registration is needed.

## Members to implement

One file, `javaw-bytecode/src/main/resources/exception-runtime/java/lang/Throwable.java`:

- change the declaration to `public class Throwable implements java.io.Serializable {`
- add `private static final long serialVersionUID = -3042686055658047285L;` (the JDK value) as the first field.
  It is only visible through reflection, so the test does not observe it; add it for fidelity, on `Throwable`
  only.

Nothing else changes. Do not add `serialVersionUID` to `Exception`, `RuntimeException`, `Error` or any other
class, and do not edit any other runtime file.

## Members to leave out

- Serialization hooks (`writeObject(ObjectOutputStream)`, `readObject(ObjectInputStream)`, `readResolve`):
  `java.io.ObjectOutputStream`/`ObjectInputStream` are not in the runtime. Do not add them.
- `printStackTrace` and every other member not already in the file: out of scope.

## JDK behaviour to match

- `instanceof Serializable` is `true` for `Throwable`, `Exception`, `RuntimeException`, `Error`, any runtime
  exception subclass (`IllegalStateException`, ...) and user exceptions, also ones that implement `Serializable`
  again or through a sub-interface. It stays `false` for a plain user class and for `new Object()`, `null`.
- An exception can be assigned to a `Serializable` variable, stored in a `Serializable[]`, passed to
  `<T extends Serializable>` and cast back with `(Throwable)` / `(RuntimeException)` and rethrown.
- Messages, causes, `toString`, suppressed exceptions and `initCause` errors behave exactly as before.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeThrowableSerializableTest.java`.
2. Edit `Throwable.java` as described under Members to implement (two lines).
3. `javaw-agent-kit/bin/kit check T65` until PASS.
4. Write `docs/bytecode-throwable-serializable.md` (below), the report, then `javaw-agent-kit/bin/kit finish T65`.

## Hints

- Use the fully-qualified name `java.io.Serializable` in the `implements` clause (no import needed; either works).
- Do not run `kit register`: `Throwable` and `Serializable` are already in the fixed runtime list.
- Do not change constructors or methods. `Throwable` is the root of every exception; any other edit breaks many
  existing tests.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
instanceChecks, assignments, casts: if Wasm prints `n` for Throwable/Exception/RuntimeException/Error or throws ClassCastException, Throwable does not declare `implements java.io.Serializable`; add it to the class declaration in Throwable.java.
unchanged: messages, causes, toString or suppressed changed; undo every edit to Throwable.java except the implements clause and the serialVersionUID field.
```

## Docs

Create `docs/bytecode-throwable-serializable.md` with this skeleton (fill in the brackets):

    # Bytecode Throwable Serializable

    Revision `throwable-serializable-vN` makes `java.lang.Throwable` in the bundled exception runtime implement
    `java.io.Serializable`, like the JDK.

    Contract:

    - <every Throwable subclass is instanceof Serializable; assignment, arrays, generic bounds and casts back
      work; serialVersionUID is declared on Throwable only; no serialization hooks>

    Evidence: `BytecodeThrowableSerializableTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances).
