# T52 TimeUnit conversions

```kit
id: T52
slug: time-unit
title: link java.util.concurrent.TimeUnit (constants and conversions)
tests: BytecodeTimeUnitTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/TimeUnit.java
allow: docs/bytecode-time-unit.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-time-unit-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeTimeUnitTest.java`.

## Goal

Add the enum `java.util.concurrent.TimeUnit` to the bundled runtime with its seven constants and the pure
arithmetic conversions. `runtime-target/target/audit/member-references.tsv` lists the uses. Constants (opcode 178):
`MILLISECONDS` 118 sites (logback `ConfigurationModelHandlerFull.processScanAttrib`), `SECONDS` 64 (logback
`TimeBasedRollingPolicy.waitForAsynchronousJobToStop`), `NANOSECONDS` 50 (H2 `RunScript.runTool`), `MINUTES` 5
(HikariCP `HikariConfig.<clinit>`), `MICROSECONDS` 4 and `HOURS` 1 (HikariCP `ClockSource.<clinit>`), `DAYS` 2 (H2
`Utils.shutdownExecutor`). Conversions (opcode 182): `toMillis(J)J` 54 (H2 `RunScript.runTool`), `toNanos(J)J` 14
(H2 `QueryExpressionIndex.<clinit>`), `convert(JLjava/util/concurrent/TimeUnit;)J` 10 (HikariCP
`ClockSource.elapsedDisplayString0`), `toSeconds(J)J` 9 (HikariCP `PrometheusMetricsTracker.createSummary`),
`toMicros(J)J` 1 (Tomcat `AbstractAccessLogValve$ElapsedTimeElement.addElement`), `toDays(J)J` 1 (Tomcat
`ResolverImpl.resolveSslCertificates`); plus the inherited `name()`, `ordinal()`, `toString()`, `equals`, `hashCode`
(Spring `StopWatch.prettyPrint`, HikariCP, Tomcat, Byte Buddy). `toMinutes`/`toHours` are not referenced but belong to
the same family and are added for coherence. TimeUnit unblocks the timed `BlockingQueue`/`Lock` methods in later tasks.

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/TimeUnit.java`
(`package java.util.concurrent;`): `public enum TimeUnit` with, in this order,

- constants `NANOSECONDS(TimeUnit.NANO_SCALE)`, `MICROSECONDS(TimeUnit.MICRO_SCALE)`,
  `MILLISECONDS(TimeUnit.MILLI_SCALE)`, `SECONDS(TimeUnit.SECOND_SCALE)`, `MINUTES(TimeUnit.MINUTE_SCALE)`,
  `HOURS(TimeUnit.HOUR_SCALE)`, `DAYS(TimeUnit.DAY_SCALE)` (no constant-specific class bodies)
- `private static final long NANO_SCALE = 1L; MICRO_SCALE = 1000L * NANO_SCALE; MILLI_SCALE = 1000L * MICRO_SCALE;
  SECOND_SCALE = 1000L * MILLI_SCALE; MINUTE_SCALE = 60L * SECOND_SCALE; HOUR_SCALE = 60L * MINUTE_SCALE;
  DAY_SCALE = 24L * HOUR_SCALE;`
- instance fields `private final long scale, maxNanos, maxMicros, maxMillis, maxSecs; private final long microRatio;
  private final int milliRatio, secRatio;` (`microRatio` is a **long**: `HOURS` and `DAYS` have more than
  `Integer.MAX_VALUE` microseconds)
- `private TimeUnit(long s)`
- `private static long cvt(long d, long dst, long src)`
- `public long convert(long sourceDuration, TimeUnit sourceUnit)`
- `public long toNanos(long duration)`, `toMicros`, `toMillis`, `toSeconds`, `toMinutes`, `toHours`, `toDays`
  (all `long -> long`)

`values()`, `valueOf(String)`, `name()`, `ordinal()`, `toString()`, `compareTo`, `equals`, `hashCode` and
`getDeclaringClass()` come from javac and `java.lang.Enum`; do not write them.

## Members to leave out

- `sleep(long)`, `timedWait(Object, long)`, `timedJoin(Thread, long)` and their private helper `excessNanos`: need threads, monitors and a clock. The
  test checks they stay unlinked.
- `convert(java.time.Duration)`, `toChronoUnit()`, `of(ChronoUnit)`: `java.time` is not in the runtime. The test
  checks they stay unlinked. Do not import anything from `java.time`.
- Do not edit any other runtime file.

## JDK behaviour to match

This is the JDK 17/21 implementation; copy it exactly.

- Constructor: `this.scale = s; this.maxNanos = Long.MAX_VALUE / s;` then
  `long ur = (s >= MICRO_SCALE) ? (s / MICRO_SCALE) : (MICRO_SCALE / s); this.microRatio = ur;
  this.maxMicros = Long.MAX_VALUE / ur;` (no cast) and the same with `MILLI_SCALE` (`this.milliRatio = (int) ur;`,
  `maxMillis`) and `SECOND_SCALE` (`this.secRatio = (int) ur;`, `maxSecs`). `milliRatio` and `secRatio` fit in 32
  bits for every unit; `microRatio` does not for `HOURS`/`DAYS`, hence `long`.
- `cvt(d, dst, src)`: `if (src == dst) return d; else if (src < dst) return d / (dst / src);` else
  `long r = src / dst, m = Long.MAX_VALUE / r; if (d > m) return Long.MAX_VALUE; else if (d < -m) return
  Long.MIN_VALUE; else return d * r;`. Note `d < -m`, so `-m * r` is still allowed.
- `convert(d, u)`: `switch (this) { case NANOSECONDS: return u.toNanos(d); case MICROSECONDS: return u.toMicros(d);
  case MILLISECONDS: return u.toMillis(d); case SECONDS: return u.toSeconds(d); default: return cvt(d, scale,
  u.scale); }`. A `null` unit throws `NullPointerException` (from the call on `u`).
- `toNanos(d)`: `long s, m; if ((s = scale) == NANO_SCALE) return d; else if (d > (m = maxNanos)) return
  Long.MAX_VALUE; else if (d < -m) return Long.MIN_VALUE; else return d * s;`
- `toMicros(d)`: `if ((s = scale) <= MICRO_SCALE) return (s == MICRO_SCALE) ? d : d / microRatio; else if (d > (m =
  maxMicros)) return Long.MAX_VALUE; else if (d < -m) return Long.MIN_VALUE; else return d * microRatio;`
- `toMillis`, `toSeconds`: the same shape with `MILLI_SCALE`/`milliRatio`/`maxMillis` and
  `SECOND_SCALE`/`secRatio`/`maxSecs`.
- `toMinutes(d)`: `cvt(d, MINUTE_SCALE, scale)`; `toHours(d)`: `cvt(d, HOUR_SCALE, scale)`; `toDays(d)`:
  `cvt(d, DAY_SCALE, scale)`.
- Division truncates toward zero (`NANOSECONDS.toMicros(-1999)` is `-1`); multiplication saturates to
  `Long.MAX_VALUE`/`Long.MIN_VALUE` instead of overflowing.
- Enum surface from javac/`Enum`: `toString()` is the name, `ordinal()` follows declaration order, `compareTo`
  is the ordinal difference, `getDeclaringClass()` is `TimeUnit`. The test does not call `TimeUnit.valueOf(String)`:
  `Enum.valueOf(Class, String)` is not supported by the runtime yet and no pinned library calls it on TimeUnit.

## Steps

1. Read the test, then `javaw-bytecode/src/main/resources/exception-runtime/java/math/RoundingMode.java` for an
   existing runtime enum.
2. Create `java/util/concurrent/TimeUnit.java` under `javaw-bytecode/src/main/resources/exception-runtime/` with the
   members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/concurrent/TimeUnit.java`
4. `javaw-agent-kit/bin/kit check T52` until PASS.
5. Write `docs/bytecode-time-unit.md` (below), the report, then `javaw-agent-kit/bin/kit finish T52`.

## Hints

- Static constants of an enum cannot be read from the constructor by simple name before they are initialised unless
  they are compile-time constants: keep the `*_SCALE` fields `private static final long` with constant initialisers
  (`1000L * NANO_SCALE` is a constant expression), and refer to them as `TimeUnit.NANO_SCALE` in the constant list.
- `convert` switches on `this`; a `switch` on the enum inside the enum itself is fine. An `if (this == NANOSECONDS)`
  chain is equivalent.
- Calibration pitfall: declaring `microRatio` as `int` (as `milliRatio`/`secRatio` are) wraps
  `HOURS`/`DAYS` to a wrong ratio, so `HOURS.toMicros(1)` and `DAYS.toMicros(1)` come out wrong; only `toMicros`
  fixtures fail. Keep it `long`.
- Other easy mistakes: `d <= -m` instead of `d < -m`; using `cvt` for `toNanos..toSeconds` without the saturation
  (it has it, but the ratio direction matters: `cvt(d, dst, src)` takes the *target* scale second); computing
  `toDays` as `toHours(d) / 24` (wrong for saturated values: `NANOSECONDS.toDays(MAX)` is `106751`).
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constants, switching, toMethods, convertAll: if FIX THE RUNTIME says `link the symbolic class/interface java/util/concurrent/TimeUnit` or names one of its methods, the class is missing, unregistered or lacks a listed member: run `javaw-agent-kit/bin/kit register java/util/concurrent/TimeUnit.java` and add every member under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constants, switching: declare the seven constants in the order NANOSECONDS, MICROSECONDS, MILLISECONDS, SECONDS, MINUTES, HOURS, DAYS with no class bodies; do not write values(), valueOf or toString.
toMethods: if only the toMicros column differs for HOURS/DAYS, `microRatio` must be a long field (not int) and assigned without a cast.
toMethods, saturation: toNanos/toMicros/toMillis/toSeconds return d when the scale matches, divide by the ratio when the unit is smaller, else saturate with `d > max` -> Long.MAX_VALUE and `d < -max` -> Long.MIN_VALUE before multiplying.
truncation, asLibrariesUseIt: integer division truncates toward zero; toMinutes/toHours/toDays are cvt(d, MINUTE_SCALE/HOUR_SCALE/DAY_SCALE, scale).
convertAll, nullUnit: convert(d, u) dispatches on this: NANOSECONDS -> u.toNanos(d), MICROSECONDS -> u.toMicros(d), MILLISECONDS -> u.toMillis(d), SECONDS -> u.toSeconds(d), otherwise cvt(d, scale, u.scale).
leavesOutSleepingAndJavaTime: sleep, timedWait, timedJoin, convert(Duration), toChronoUnit and of(ChronoUnit) must not exist in the runtime TimeUnit.
```

## Docs

Create `docs/bytecode-time-unit.md` with this skeleton (fill in the brackets):

    # Bytecode TimeUnit

    Revision `time-unit-vN` links `java.util.concurrent.TimeUnit` in the bundled exception runtime.

    Contract:

    - <seven constants in JDK order; convert and toNanos..toDays with truncating division and saturation to
      Long.MAX_VALUE/Long.MIN_VALUE; enum surface from java.lang.Enum>

    Known differences: `sleep`, `timedWait`, `timedJoin` (threads/clock) and the `java.time` bridges
    (`convert(Duration)`, `toChronoUnit`, `of`) are not linked.

    Evidence: `BytecodeTimeUnitTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `sleep`, `timedWait`, `timedJoin`, `convert(Duration)`, `toChronoUnit` and `of` are rejected
    before WAT generation.
