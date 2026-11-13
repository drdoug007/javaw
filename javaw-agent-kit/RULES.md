# Rules for runtime sources

The bytecode compiler does not use the real JDK library. It compiles its own copies of JDK classes from
`javaw-bytecode/src/main/resources/exception-runtime/` (for example `java/util/LinkedList.java`). A task adds
or changes files there. These rules come from how that runtime is built and admitted; breaking one gives a
diagnostic, never a silent pass.

## How a runtime class is built

- The files are compiled once per test JVM by javac with `--patch-module java.base=...`. So the package must
  be in `java.base` (`java.lang`, `java.util`, `java.util.function`, `java.security`, `javax.crypto`, ...).
- javac resolves anything missing against the **host** JDK. "It compiles" proves nothing. Only the tests
  prove that every class and member you use is really in the runtime.
- **Admission checks every runtime method, even ones nothing calls.** One bad method breaks every bytecode
  test, not just yours.
- A class must be **registered** or it is silently ignored:
  - `java/lang/Name.java`: add `Name` to the `NAMES` string at the top of
    `javaw-bytecode/src/main/java/one/dastec/javaw/bytecode/BytecodeExceptionRuntime.java`.
  - `java/util/Name.java`: add `"Name"` to the `List.of(...)` line that already lists `"LinkedList"` in the
    same file.
  - Any other package: add a line `paths.add("java/security/Name.java");` next to the other `paths.add` lines,
    before the `ExceptionSupport` line.
  - Nested and anonymous classes need no registration.

## Every type you mention must exist in the runtime

Every class named in a field type, parameter, return type, `throws`, `extends`/`implements`, `new`, cast,
`instanceof` or `catch` must itself be a runtime class. Check before you use it:

```
ls javaw-bytecode/src/main/resources/exception-runtime/java/util/
grep -n "public .* methodName(" javaw-bytecode/src/main/resources/exception-runtime/java/lang/String.java
```

If a type is missing, either add it (an exception class is a few lines; copy
`java/util/NoSuchElementException.java`) if the task allows that file, or leave the member out. The same goes
for methods: call only methods that exist in the runtime copy of the class. Do not assume the full JDK is there.

## Forbidden in runtime sources (`kit guard` checks these)

- `synchronized` methods or blocks, `volatile` fields. Write single-threaded code. Where the JDK class is
  synchronized (Vector, Hashtable, Collections.synchronizedX), implement the same behaviour without locks.
- `native` methods, `assert`, `Thread`, `System.out`/`err`/`in`, `System.currentTimeMillis`/`nanoTime`,
  `Math.random`, `new Random()`.
- `switch` on enum constants (it generates a synthetic helper class). Use `if`/`else` or `ordinal()`.
- Redefining `java.lang.Object` or `java.lang.System`. Only `System.arraycopy`, `System.identityHashCode` and
  `System.getProperty` exist.

## Allowed and commonly used

String `+`, `StringBuilder`, lambdas and method references, interface `default` methods, static fields and
static initializers, nested/anonymous/generic classes, varargs, `long`/`double` arithmetic, try/catch,
`System.arraycopy`, `java.util.Arrays`, `java.util.Objects`, `Integer`/`Long` bit helpers
(`bitCount`, `numberOfTrailingZeros`, `rotateLeft`, ...). Check that a helper exists before you use it.

## Matching the JDK

- The expected values in the tests come from the real JVM, so match the JDK's behaviour exactly: return
  values, `toString` formats, exception **types** and, where a test checks them, exception **messages**.
- The JDK 21 source is the reference for behaviour, for example:
  `unzip -p ~/.sdkman/candidates/java/21.0.2-graalce/lib/src.zip java.base/java/util/StringJoiner.java`.
  Read it to learn the exact behaviour, but do not paste large pieces of JDK code. Write a small, clear
  implementation. The JDK version uses internal helpers (`jdk.internal.*`, `SharedSecrets`, `Unsafe`) that
  do not exist here.
- Existing style: runtime files are compact (several statements per line). You may write normal formatting.
  Clarity beats density.

## Good examples to copy from

| You need | Look at |
|---|---|
| a small value class | `java/util/OptionalInt.java` |
| an exception class | `java/util/NoSuchElementException.java`, `java/util/ConcurrentModificationException.java` |
| a list implementation | `java/util/ArrayList.java`, `java/util/LinkedList.java` |
| an interface with default methods | `java/util/Collection.java`, `java/util/List.java` |
| fail-fast iterators (`modCount`) | `java/util/LinkedList.java` |
| a delegating wrapper | `java/util/UnmodifiableViews.java` |
| hashing / bit tricks | `java/lang/Integer.java`, `java/lang/Long.java` |
