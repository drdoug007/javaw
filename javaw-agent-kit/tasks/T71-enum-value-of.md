# T71 Enum.valueOf for every enum

```kit
id: T71
slug: enum-value-of
title: make Enum.valueOf(Class, String) work for application and runtime enums, with JDK messages
tests: BytecodeEnumValueOfTest
depends: T52, T60
allow: javaw-bytecode/src/main/resources/exception-runtime/java/lang/Enum.java
allow: docs/bytecode-enum-value-of.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-enum-value-of-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeEnumValueOfTest.java`.

## Goal

Every javac-generated `E.valueOf(String)` calls `Enum.valueOf(Class, String)`.
`runtime-target/target/audit/member-references.tsv` has a `java/lang/Enum valueOf
(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/Enum;` row (opcode 184) in almost every pinned artifact, for example
H2 2.2.224 (35 sites, `org/h2/api/IntervalQualifier.valueOf`), jackson-databind 2.15.4 (20 sites,
`AnnotationIntrospector$ReferenceProperty$Type.valueOf`), logback-core 1.4.14 (16 sites, `ActionUtil$Scope.valueOf`),
jackson-core (16), jackson-annotations (11), jandex (7), HikariCP (3, `SQLExceptionOverride$Override.valueOf`) and
micrometer. The runtime's current `Enum.valueOf` calls `Class.getEnumConstants()`, which works for application enums
(the compiler generates reflection tables for them) but throws for enums that live in the runtime itself
(`TimeUnit`, `ChronoUnit`, ...), because runtime classes have no reflection tables. Its messages also differ from the
JDK. This task makes `Enum` remember its constants as they are constructed, so `valueOf` works for every enum without
reflection, and matches the JDK's exceptions and messages. Only `Enum.java` changes.

## Members to implement

In `java/lang/Enum.java` (edit; keep every existing member unchanged unless listed here):

1. Three private static fields, a registry of constructed constants per enum class, for example
   `private static Class<?>[] javawTypes = new Class<?>[0];`, `private static Enum<?>[][] javawConstants = new
   Enum<?>[0][];`, `private static int[] javawCounts = new int[0];` (parallel arrays: the class, its constants in
   construction order, and how many are used).
2. `private static void javawRegister(Enum<?> constant)`: finds the entry for `constant.getDeclaringClass()` by
   identity (`==`), adding a new entry at the end if there is none, and appends the constant to it (grow the arrays
   with `System.arraycopy`).
3. The protected constructor `Enum(String name, int ordinal)` keeps assigning both fields and then calls
   `javawRegister(this)`.
4. `public static <T extends Enum<T>> T valueOf(Class<T> enumClass, String name)` rewritten as described below.

## Members to leave out

- Do not edit `Class.java`: `getEnumConstants()` stays as it is (it must return a correctly typed `T[]`, which the
  registry cannot build for runtime enums). `enumConstantDirectory()` is a JDK-private method and is not added.
- No public members are added to `Enum`. Helper names must start with `javaw`.

Why a registry and not `getEnumConstants()`: this task is a deliberate runtime-only workaround. The proper fix is in
the compiler and is tracked in `Todo.md` (runtime enum reflection): `BytecodeReflection` builds the per-class
reflection routes (including the `values()` route behind `Class.getEnumConstants()`) only for application classes.
It skips `java/` owners in its metadata loop and builds nothing unless the application has an enum of its own. Once
runtime enums get that route, `valueOf` can drop the registry and use `getEnumConstants()` for every enum, and the
initialisation limit below disappears. Do not attempt that compiler change in this task.

## JDK behaviour to match

The JDK (`Enum.valueOf` + `Class.enumConstantDirectory`) checks in this order:

1. `enumClass == null` throws `NullPointerException` (the test checks only the type, because the JDK's message is a
   helpful-NPE text).
2. `enumClass` is not an enum class: `IllegalArgumentException(enumClass.getName() + " is not an enum class")`, for
   example `java.lang.String is not an enum class`.
3. `name == null`: `NullPointerException("Name is null")`.
4. A constant whose `name()` equals `name` exactly (case-sensitive; `toString()` overrides do not matter) is returned
   (the same object).
5. Otherwise `IllegalArgumentException("No enum constant " + enumClass.getCanonicalName() + "." + name)`, for example
   `No enum constant Color.PINK`, `No enum constant Fixture.Mode.APPEND` (canonical name, with a dot for a nested
   enum), `No enum constant java.util.concurrent.TimeUnit.SECOND`.

An enum with no constants (`enum Empty {}`) gives `No enum constant Empty.X`.

## Steps

1. Add the three registry fields and `javawRegister` to `Enum.java`, and call it at the end of the constructor.
2. Rewrite `valueOf`:
   - if `enumClass == null` throw `new NullPointerException()`;
   - look for `enumClass` in the registry (by `==`). If found: null name throws `NullPointerException("Name is null")`,
     otherwise scan its constants for `name.equals(constant.name)` and return the match cast to `T`, else throw the
     `No enum constant` exception;
   - if not found (the class has no constructed constants yet, e.g. an empty enum, or a non-enum class): if
     `!enumClass.isEnum()` throw the `is not an enum class` exception; else take `T[] values =
     enumClass.getEnumConstants()` (works for application enums), treat `null` as "not an enum class", then apply the
     null-name check, the scan and the `No enum constant` exception in the same way.
3. Build messages with `String.concat` or `+`; use `enumClass.getCanonicalName()` and `enumClass.getName()` from the
   runtime `Class`.
4. Create the docs file (below). No registration is needed (`Enum` is already registered).

## Hints

- `getDeclaringClass()` already exists in `Enum` and returns the enum class also for a constant with a body
  (`GREEN { ... }`, whose runtime class is an anonymous subclass). Register under `getDeclaringClass()`, not
  `getClass()`, or `Color.valueOf("GREEN")` fails.
- `E.valueOf(String)` runs inside `E`, so its constants are already constructed and registered when `valueOf` is
  called. The fallback through `getEnumConstants()` is only needed for empty application enums and non-enum classes.
  Do not call `getEnumConstants()` for a class found in the registry: for runtime enums it throws.
- Read the field `constant.name` directly or call `name()`; both are fine.
- Keep the checks in the order listed under JDK behaviour: a null class must throw before the name check, and a
  non-enum class must report `is not an enum class` even when `name` is null.
- Calibration: the reference implementation (about 30 lines) passed on the first run. Static fields with
  initialisers in `java.lang.Enum` work in the runtime.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
runtimeEnums, runtimeFailures: an uncaught exception for TimeUnit/ChronoUnit means valueOf still calls getEnumConstants() for runtime enums; look the class up in the constructor-filled registry first and only fall back to getEnumConstants() when the class is not registered.
userEnums: a failure for GREEN means constants with a body were registered under getClass(); register under getDeclaringClass().
unknownNames: the message is `No enum constant ` + enumClass.getCanonicalName() + `.` + name (canonical name, so `Fixture.Mode`, not `Fixture$Mode`); Empty must fall back to getEnumConstants() and give `No enum constant Empty.X`.
nullArguments: a null name throws NullPointerException with message `Name is null`; a null class throws NullPointerException before anything else.
notAnEnum: a non-enum class throws IllegalArgumentException `java.lang.String is not an enum class` (getName(), not getCanonicalName()).
```

## Docs

Create `docs/bytecode-enum-value-of.md` with this skeleton (fill in the brackets):

    # Bytecode Enum.valueOf

    Revision `enum-value-of-vN` makes `java.lang.Enum.valueOf(Class, String)` (and so every enum's
    `valueOf(String)`) work for application and runtime enums in the bundled exception runtime.

    Contract:

    - <constants register themselves per declaring class in the Enum constructor; lookup order and the JDK
      exception types and messages; fallback through getEnumConstants() for unregistered classes>

    Known differences: `Enum.valueOf(RuntimeEnum.class, name)` before the runtime enum class has been initialised
    falls back to `getEnumConstants()`, which runtime enums do not support. The registry is a runtime-only
    workaround: `BytecodeReflection` builds the `values()` route behind `getEnumConstants()` only for application
    enums. Giving runtime (`java/`) enums that route in the compiler would let `valueOf` use `getEnumConstants()`
    everywhere and remove both the registry and this limit (open in `Todo.md`, runtime enum reflection).

    Evidence: `BytecodeEnumValueOfTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances).
