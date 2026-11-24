# T62 ClassNotFoundException

```kit
id: T62
slug: class-not-found-exception
title: link java.lang.ClassNotFoundException and the ReflectiveOperationException cause constructors
tests: BytecodeClassNotFoundExceptionTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/ClassNotFoundException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/ReflectiveOperationException.java
allow: docs/bytecode-class-not-found-exception.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-class-not-found-exception-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeClassNotFoundExceptionTest.java`.

## Goal

Add `java.lang.ClassNotFoundException` to the bundled runtime (it does not exist yet) and give the existing
`java.lang.ReflectiveOperationException` its two missing cause constructors. The pinned libraries throw
`ClassNotFoundException` from their own class loaders (`runtime-target/target/audit/member-references.tsv`, 22 rows
in 12 artifacts): `<init>(Ljava/lang/String;)V` (27 call sites, e.g. AspectJ
`ClassLoaderRepository.loadClass`, Tomcat `WebappClassLoaderBase.findClass`, Spring `ShadowingClassLoader`,
cglib `ReflectUtils.getClass`), `<init>(Ljava/lang/String;Ljava/lang/Throwable;)V` (15, e.g. Tomcat
`WebappClassLoaderBase.checkStateForClassLoading`, Jackson `TypeFactory.findClass`, Hibernate
`AggregatedClassLoader.findClass`), `getException()` (AspectJ `LangUtil.unwrapException`), and the inherited
`getMessage`/`getCause` (ByteBuddy `JavaDispatcher.run`, Spring webmvc `GroovyMarkupView.getTemplate`).
`ReflectiveOperationException.getCause/getMessage/toString` are called by Tomcat `BeanFactory` and logback
`ContextInitializer`. Nothing in `javaw-bytecode` synthesizes this exception (there is no `Class.forName` in the
bytecode runtime); it is only thrown by library code.

## Members to implement

`java/lang/ReflectiveOperationException.java` (keep the two existing constructors, add two):

- `public ReflectiveOperationException(String message, Throwable cause)` → `super(message, cause)`
- `public ReflectiveOperationException(Throwable cause)` → `super(cause)`

`java/lang/ClassNotFoundException.java`:
`public class ClassNotFoundException extends ReflectiveOperationException`

- `public ClassNotFoundException()` → `super((Throwable) null)`
- `public ClassNotFoundException(String s)` → `super(s, null)`
- `public ClassNotFoundException(String s, Throwable ex)` → `super(s, ex)`
- `public Throwable getException()` → `return super.getCause();`

Nothing else: no fields, no `toString`/`getMessage`/`getCause` override.

## Members to leave out

- `readObject`/`writeObject`/`serialPersistentFields` (serialization; `java.io.ObjectInputStream` is not linked).
- There is no `ClassNotFoundException(Throwable)` constructor in the JDK; do not add one (checked).
- `Class.forName` and any class lookup: do not add them to `Class.java` or anywhere else (checked).
- Do not edit `Throwable.java`, `Exception.java` or any other runtime file.

## JDK behaviour to match

- All three `ClassNotFoundException` constructors **set the cause** (to `null` for the first two), exactly like
  JDK 21. So `initCause(...)` on any `ClassNotFoundException` throws `IllegalStateException` (with the exception
  itself as cause) and leaves the cause unchanged, also for `new ClassNotFoundException("m")` and also for
  `initCause(null)`. This is why the no-arg constructor uses `super((Throwable) null)` and the one-arg constructor
  uses `super(s, null)`, not `super()`/`super(s)`.
- `new ClassNotFoundException()`: message `null`, cause `null`, `toString()` is
  `java.lang.ClassNotFoundException` (the runtime `Throwable(Throwable)` gives a `null` message for a `null` cause).
- `new ClassNotFoundException("x.Y")`: `toString()` is `java.lang.ClassNotFoundException: x.Y`.
- `getException()` calls `super.getCause()` (invokespecial), so a subclass that overrides `getCause()` does not
  change what `getException()` returns, and `getException()` does not run the override.
- `ReflectiveOperationException(String, Throwable)` and `(Throwable)` set the cause (so `initCause` then throws);
  the old `()` and `(String)` constructors do not set it (`initCause` works once, also with `null`).
  `new ReflectiveOperationException(cause)` has message `cause.toString()`.
- `ClassNotFoundException` is a checked exception: not a `RuntimeException`, not an `Error`.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeClassNotFoundExceptionTest.java`.
2. Read `javaw-bytecode/src/main/resources/exception-runtime/java/lang/ReflectiveOperationException.java`,
   `Exception.java` and `Throwable.java` (do not edit the last two).
3. Add the two constructors to `ReflectiveOperationException.java`.
4. Create `java/lang/ClassNotFoundException.java` under `javaw-bytecode/src/main/resources/exception-runtime/`.
5. Register it (never edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/lang/ClassNotFoundException.java`.
6. `javaw-agent-kit/bin/kit check T62` until PASS.
7. Write `docs/bytecode-class-not-found-exception.md` (below), the report, then `javaw-agent-kit/bin/kit finish T62`.

## Hints

- The whole task is about 15 lines. Copy the JDK constructor bodies exactly as listed above.
- `super()` / `super(s)` in the `ClassNotFoundException` constructors is the classic mistake: everything looks
  right except `initCauseDisallowed`, which then prints `set;` instead of `java.lang.IllegalStateException,...`.
- `return getCause();` in `getException()` is the other mistake: `getExceptionUsesSuper` then logs `cause;`.
- `super((Throwable) null)` needs the cast; without it javac reports an ambiguous constructor call.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constructors, hierarchy, initCauseDisallowed, getExceptionUsesSuper: if FIX THE RUNTIME says `link a complete acyclic type hierarchy` or `link the descriptor class/interface` naming ClassNotFoundException, run `javaw-agent-kit/bin/kit register java/lang/ClassNotFoundException.java`. If it says `link a matching instance method in the declaring hierarchy`, add the member exactly as listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
reflectiveCauses: ReflectiveOperationException needs (String, Throwable) → super(message, cause) and (Throwable) → super(cause); keep () and (String) unchanged.
initCauseDisallowed: every ClassNotFoundException constructor must set the cause: () uses super((Throwable) null), (String s) uses super(s, null), (String s, Throwable ex) uses super(s, ex). Do not call super() or super(s).
getExceptionUsesSuper: getException must be `return super.getCause();` (not getCause()), so an overriding getCause is not called; do not override getMessage/getCause/toString.
leavesOutClassLookup: Class.forName and a ClassNotFoundException(Throwable) constructor must not exist; remove them.
```

## Docs

Create `docs/bytecode-class-not-found-exception.md` with this skeleton (fill in the brackets):

    # Bytecode ClassNotFoundException

    Revision `class-not-found-exception-vN` links `java.lang.ClassNotFoundException` and the
    `ReflectiveOperationException(String, Throwable)` / `(Throwable)` constructors in the bundled exception runtime.

    Contract:

    - <three constructors all set the cause, so initCause throws IllegalStateException; getException returns
      super.getCause(); checked exception under ReflectiveOperationException>

    Known differences: serialization (`readObject`/`writeObject`) is not linked. The runtime never throws
    `ClassNotFoundException` itself; there is no `Class.forName`.

    Evidence: `BytecodeClassNotFoundExceptionTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
    orders, two fresh instances). `Class.forName(String)` and `ClassNotFoundException(Throwable)` are rejected
    before WAT generation.
