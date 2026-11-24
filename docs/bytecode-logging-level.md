# Bytecode Logging Level

Revision `logging-level-v322` links `java.util.logging.Level` in the bundled exception runtime.

Contract:

- Nine `public static final` constants, built in this order with the three-argument constructor and bundle name
  `"sun.util.logging.resources.logging"`: `OFF` (2147483647), `SEVERE` (1000), `WARNING` (900), `INFO` (800),
  `CONFIG` (700), `FINE` (500), `FINER` (400), `FINEST` (300), `ALL` (-2147483648).
- `protected Level(String, int)` delegates to `protected Level(String, int, String)` with a `null` bundle name.
  A `null` name throws `NullPointerException` before anything is stored or registered; `""` is a valid name.
  Every constructed level, including subclass instances and levels created by `parse`, is appended to a single
  static construction-order registry.
- `getName()` and `toString()` return the name; `intValue()` and `hashCode()` return the int value;
  `getResourceBundleName()` returns the stored bundle name (`null` for the two-argument constructor).
- `getLocalizedName()` always returns the name (the JDK also returns the name when no bundle is loadable).
- `equals(Object)` is `true` exactly when the argument is a `Level` (any subclass) with the same int value;
  names are ignored; `null` and non-levels are not equal. `hashCode` returns the value, so equal levels are
  usable as `HashMap` keys.
- `parse(String)` throws `NullPointerException` for `null`; it returns the first registered level whose name
  matches exactly (case-sensitive, no trimming, compared to the constructor name, never via `getName()`), then
  the first registered level whose value equals `Integer.parseInt(name)` (`+` and leading zeros are valid
  numbers), and only then a freshly constructed `Level(name, x)` that registers itself so the same string parses
  to the same object again. A name that is neither a registered name nor an int throws
  `IllegalArgumentException("Bad level \"<name>\"")`.

Known differences: there are no resource bundles, so `getLocalizedName()` always returns the name and `parse`
skips the JDK's localized-name lookup step (localized names equal the names); levels are
held strongly (no weak references); serialization (`readResolve`) is not implemented.

Evidence: `BytecodeLevelTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `Level.findLevel`, `Level.getLevelName` and `Logger.getLogger` are rejected before WAT
generation.
