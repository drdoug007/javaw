# Bytecode TimeUnit

Revision `time-unit-v315` links `java.util.concurrent.TimeUnit` in the bundled exception runtime.

Contract:

- The seven constants in JDK order: `NANOSECONDS`, `MICROSECONDS`, `MILLISECONDS`, `SECONDS`, `MINUTES`, `HOURS`, `DAYS`. `convert` dispatches on the target unit (`NANOSECONDS`/`MICROSECONDS`/`MILLISECONDS`/`SECONDS` delegate to the source unit's `toX`, otherwise `cvt(d, scale, u.scale)`); `toNanos`/`toMicros`/`toMillis`/`toSeconds` use the unit's scale or ratio, and `toMinutes`/`toHours`/`toDays` use `cvt`, all with division truncating toward zero and multiplication saturating to `Long.MAX_VALUE`/`Long.MIN_VALUE` instead of overflowing. `microRatio` is a `long` because `HOURS` and `DAYS` have more than `Integer.MAX_VALUE` microseconds. The enum surface (`name`, `ordinal`, `toString`, `compareTo`, `equals`, `hashCode`, `values`, `getDeclaringClass`) comes from `java.lang.Enum`. `valueOf(String)` is not covered: the runtime does not support `Enum.valueOf(Class, String)` yet.

Known differences: `sleep`, `timedWait`, `timedJoin` (threads/clock) and the `java.time` bridges
(`convert(Duration)`, `toChronoUnit`, `of`) are not linked.

Evidence: `BytecodeTimeUnitTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `sleep`, `timedWait`, `timedJoin`, `convert(Duration)`, `toChronoUnit` and `of` are rejected
before WAT generation.
