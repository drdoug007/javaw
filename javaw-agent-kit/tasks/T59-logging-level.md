# T59 java.util.logging.Level

```kit
id: T59
slug: logging-level
title: link java.util.logging.Level
tests: BytecodeLevelTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/logging/Level.java
allow: docs/bytecode-logging-level.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-logging-level-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeLevelTest.java`.

## Goal

Add `java.util.logging.Level` to the bundled runtime. It is the first class in the `java.util.logging` package
(no `Logger`, `Handler` or `LogRecord` yet). `runtime-target/target/audit/member-references.tsv` has 71 rows for
`java/util/logging/Level` in 16 artifacts. Most are `getstatic` reads of the constants (summed `count`):
`FINE` (98, e.g. logback `JULHelper.asJULLevel`), `SEVERE` (56), `WARNING` (37), `INFO` (37), `FINEST` (35),
`FINER` (29, istack-commons `com/sun/istack/logging/Logger.finer`), `CONFIG` (10), `ALL` (4), `OFF` (2). Then
`intValue()I` (7, Tomcat `JdkLoggerFormatter.format`), `parse(Ljava/lang/String;)` (2, Tomcat
`ClassLoaderLogManager.lambda$addLogger$1`), `getLocalizedName()` (2, Tomcat `OneLineFormatter.format`),
`toString()` (1, Spring `EclipseLinkJpaVendorAdapter.getJpaPropertyMap`) and the two protected constructors
`<init>(Ljava/lang/String;I)V` and `<init>(Ljava/lang/String;ILjava/lang/String;)V` (1 each, jboss-logging
`JDKLevel`, a `Level` subclass).

There are no resource bundles in this runtime: `getLocalizedName()` always returns the name.

## Members to implement

One file, `java/util/logging/Level.java`: `public class Level implements java.io.Serializable`. The test
subclasses it and overrides `getName`, `getLocalizedName`, `getResourceBundleName` and `hashCode`.

- Fields: `private final String name;`, `private final int value;`, `private final String resourceBundleName;`
  and one static registry of every level ever constructed, in construction order:
  `private static final java.util.ArrayList<Level> known = new java.util.ArrayList<>();`
- The nine constants, `public static final Level`, built with the three-argument constructor and the bundle name
  `"sun.util.logging.resources.logging"`: `OFF` (`Integer.MAX_VALUE`), `SEVERE` (1000), `WARNING` (900), `INFO`
  (800), `CONFIG` (700), `FINE` (500), `FINER` (400), `FINEST` (300), `ALL` (`Integer.MIN_VALUE`), in this order.
- `protected Level(String name, int value)` (calls the next one with a `null` bundle)
- `protected Level(String name, int value, String resourceBundleName)`
- `public String getName()`, `public String getLocalizedName()`, `public String getResourceBundleName()`
- `public final String toString()`, `public final int intValue()`
- `public static Level parse(String name)`
- `public boolean equals(Object ox)`, `public int hashCode()`

## Members to leave out

- Serialization: `serialVersionUID` and `readResolve` (`Serializable` is only a marker here).
- The JDK's package-private `findLevel(String)` and `getLevelName()`, the `KnownLevel` nested class, weak
  references, `ResourceBundle` lookup and the localized-name cache (`localizedLevelName`, `cachedLocale`). The test
  checks `findLevel` and `getLevelName` stay unlinked.
- Every other `java.util.logging` class (`Logger`, `Handler`, `LogRecord`, `LogManager`, ...): later tasks. The
  test checks `Logger.getLogger(String)` stays unlinked.
- The word `synchronized` (the JDK's `parse` is synchronized; the runtime is single-threaded).

## JDK behaviour to match

- Constructors: `name == null` throws `new NullPointerException()` before anything is stored or registered.
  Otherwise store the three fields and append `this` to `known`. Every level is registered, also subclass
  instances and levels made by `parse`. `""` is a valid name.
- `getName()`, `toString()` return `name`; `intValue()` and `hashCode()` return `value`;
  `getResourceBundleName()` returns the stored bundle name (`null` for the two-argument constructor; the
  constants return `"sun.util.logging.resources.logging"`).
- `getLocalizedName()` returns `name` (no bundle lookup; the JDK also returns the name when the bundle is `null`
  or cannot be loaded).
- `equals(Object o)`: `true` exactly when `o` is a `Level` (any subclass) with the same `value`; `false` for
  `null` and non-levels. Names do not matter (`new AppLevel("X",800).equals(Level.INFO)` is `true`). Read the
  `value` field, not `intValue()`/`hashCode()`.
- `parse(String name)`, in this order:
  1. `name == null` throws `NullPointerException` (call `name.length()` first, like the JDK).
  2. The first registered level (in `known` order) whose `name` field equals `name` exactly (case-sensitive, no
     trimming). The constants are registered first, so `parse("SEVERE")` returns `Level.SEVERE` even after
     `new AppLevel("SEVERE", 812)`.
  3. Otherwise `int x = Integer.parseInt(name)`; if that throws `NumberFormatException`, throw
     `new IllegalArgumentException("Bad level \"" + name + "\"")` (e.g. `Bad level "info"`, `Bad level ""`,
     `Bad level "2147483648"`). `"+1000"` and `"0300"` are valid numbers.
  4. The first registered level whose `value` is `x` (`parse("900")` is `WARNING`; `parse("+806")` is the level an
     earlier `parse("806")` made).
  5. Otherwise return `new Level(name, x)` (a plain `Level`, bundle `null`, named by the exact string, e.g.
     `"0808"`), which registers itself, so the same string parses to the same object again.
  `parse` reads the fields directly; it must never call `getName()` or any other overridable method (a subclass
  whose `getName()` returns `"renamed"` is still found by its constructor name, and `parse("renamed")` fails).

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeLevelTest.java`.
2. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/logging/Level.java` (new directory).
3. Register it (never edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/logging/Level.java`.
4. `javaw-agent-kit/bin/kit check T59` until PASS.
5. Write `docs/bytecode-logging-level.md` (below), the report, then `javaw-agent-kit/bin/kit finish T59`.

## Hints

- Do not copy the JDK source: it needs `KnownLevel`, `WeakReference`, `ResourceBundle`, `Optional` and
  `SharedSecrets`. The whole class is about 60 lines.
- Declare `known` **before** the nine constants. Static initializers run top to bottom, so a registry declared
  after them is still `null` when `OFF` is constructed.
- Use a plain indexed loop over the `ArrayList` in `parse`; compare names with `equals`, not `==`.
- Do not write the lowercase words `volatile`, `synchronized`, `native` or the capitalised word `Thread`.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constants, equality, parseNames, overrides: if FIX THE RUNTIME says `link a complete acyclic type hierarchy` or `link the descriptor class/interface` naming java/util/logging/Level, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/logging/Level.java`. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constants: the nine constants use the JDK values (OFF=Integer.MAX_VALUE, SEVERE=1000, WARNING=900, INFO=800, CONFIG=700, FINE=500, FINER=400, FINEST=300, ALL=Integer.MIN_VALUE) and the bundle name "sun.util.logging.resources.logging"; hashCode returns value. A NullPointerException during static initialization means `known` is declared after the constants.
equality, asKeys: equals is `ox instanceof Level && ((Level) ox).value == value` (null and non-levels give false, names are ignored); hashCode returns value. getLocalizedName returns name, also when a bundle name was given.
constructorNulls: both constructors throw `new NullPointerException()` for a null name; an empty name is fine.
parseNames: parse(null) throws NullPointerException; names match case-sensitively without trimming; a string that is neither a registered name nor an Integer.parseInt number throws `new IllegalArgumentException("Bad level \"" + name + "\"")`.
parseNumbers, parseCustom: parse looks up the name first, then Integer.parseInt, then the FIRST registered level with that value, and only then returns `new Level(name, x)`; every constructor appends this to `known` so later parses find it (also subclass levels).
overrides, parseCustom: parse, toString, intValue and equals read the fields and never call getName, getLocalizedName, getResourceBundleName or hashCode; the LoggingLevel log must stay empty except for its own direct calls.
leavesOutLoggingFramework: this fails when findLevel or getLevelName is declared on Level, or a java.util.logging.Logger class exists. Remove them.
```

## Docs

Create `docs/bytecode-logging-level.md` with this skeleton (fill in the brackets):

    # Bytecode Logging Level

    Revision `logging-level-vN` links `java.util.logging.Level` in the bundled exception runtime.

    Contract:

    - <nine constants and values; constructors and null names; getName/toString/intValue/hashCode;
      equals by int value; getResourceBundleName; parse order (name, integer, first level with that value,
      new Level) and its exceptions>

    Known differences: there are no resource bundles, so `getLocalizedName()` always returns the name and `parse`
    skips the JDK's localized-name lookup step (localized names equal the names); levels are
    held strongly (no weak references); serialization (`readResolve`) is not implemented.

    Evidence: `BytecodeLevelTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `Level.findLevel`, `Level.getLevelName` and `Logger.getLogger` are rejected before WAT
    generation.
