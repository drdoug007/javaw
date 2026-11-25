# Bytecode Enum.valueOf

Revision `enum-value-of-v333` makes `java.lang.Enum.valueOf(Class, String)` (and so every enum's
`valueOf(String)`) work for application and runtime enums in the bundled exception runtime.

Contract:

- Constants register themselves per declaring class in the `Enum` constructor: `javawRegister`
  records each constructed constant under `getDeclaringClass()` (so a constant with a body
  registers under the enum class, not its anonymous subclass) in parallel arrays
  (`javawTypes`, `javawConstants`, `javawCounts`). `valueOf` looks the class up in that registry
  by identity first and scans its constants for an exact `name()` match, without calling
  `getEnumConstants()` (which runtime enums do not support).
- Checks follow the JDK order: a null class throws `NullPointerException` before anything else;
  a class not in the registry is checked with `Class.isEnum()` and, if not an enum (or if
  `getEnumConstants()` returns `null`), throws
  `IllegalArgumentException(type.getName() + " is not an enum class")`; a null name throws
  `NullPointerException("Name is null")`; a missing constant throws
  `IllegalArgumentException("No enum constant " + type.getCanonicalName() + "." + name)`.
- Unregistered classes (empty application enums) fall back to `getEnumConstants()`, which works
  for application enums because the compiler builds their `values()` reflection route.

Known differences: `Enum.valueOf(RuntimeEnum.class, name)` before the runtime enum class has been initialised
falls back to `getEnumConstants()`, which runtime enums do not support. The registry is a runtime-only
workaround: `BytecodeReflection` builds the `values()` route behind `getEnumConstants()` only for application
enums. Giving runtime (`java/`) enums that route in the compiler would let `valueOf` use `getEnumConstants()`
everywhere and remove both the registry and this limit (open in `Todo.md`, runtime enum reflection).

Evidence: `BytecodeEnumValueOfTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances).
