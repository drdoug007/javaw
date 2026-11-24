# T60 TemporalUnit and ChronoUnit

```kit
id: T60
slug: chrono-unit
title: link java.time.temporal.TemporalUnit, ChronoUnit and the unit-based Duration/TimeUnit members
tests: BytecodeChronoUnitTest
depends: T52, T56
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/temporal/TemporalUnit.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/temporal/ChronoUnit.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/temporal/UnsupportedTemporalTypeException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/Duration.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/concurrent/TimeUnit.java
allow: docs/bytecode-chrono-unit.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-chrono-unit-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeChronoUnitTest.java`.

## Goal

Add the interface `java.time.temporal.TemporalUnit` (without its `Temporal` members), the enum
`java.time.temporal.ChronoUnit`, `java.time.temporal.UnsupportedTemporalTypeException`, and the unit-based members
of `Duration` and `TimeUnit` that they make possible. `runtime-target/target/audit/member-references.tsv` lists 39
ChronoUnit/TemporalUnit rows. Constants (opcode 178): `DAYS` (Spring `CronField$Type.<clinit>` 7 sites, Jackson
`DurationUnitConverter.<clinit>`, log4j `PropertiesUtil$TimeUnit.<clinit>`, Spring Boot `DurationStyle$Unit.<clinit>`,
Tomcat `FileHandler.streamFilesForDelete`), `NANOS`, `MICROS`, `MILLIS` (also Spring `InMemoryWebSessionStore`),
`SECONDS`, `MINUTES`, `HOURS` in the same initialisers, `HALF_DAYS` (Jackson), `WEEKS`, `MONTHS`, `YEARS` (Spring
Boot `PeriodStyle$Unit.<clinit>`, Spring `CronField$Type.<clinit>`); `ChronoUnit.name()` 8 sites (Jackson
`DurationUnitConverter.<clinit>`). `Duration.of(JLjava/time/temporal/TemporalUnit;)` (opcode 184) is called by
Jackson `DurationUnitConverter$DurationSerialization`, log4j `PropertiesUtil$TimeUnit.getDuration`, Spring Boot
`DurationStyle$Unit.parse` and Spring `ScheduledAnnotationBeanPostProcessor.toDuration` (3 sites);
`TimeUnit.toChronoUnit()` 3 sites in the same Spring method and `TimeUnit.of(ChronoUnit)` in Spring
`PeriodicTrigger.getTimeUnit`. `Duration.plus/minus(long, TemporalUnit)` are added for coherence (`of` uses `plus`).
`addTo`/`between` (Spring `CronField$Type.elapseUntil`, `CronExpression.next`, `DefaultServerWebExchange`) need
`java.time.temporal.Temporal`, which is not in the runtime: a later task.

## Members to implement

1. `java/time/temporal/TemporalUnit.java` (`package java.time.temporal;`, import `java.time.Duration`):
   `public interface TemporalUnit` declaring exactly `Duration getDuration();`, `boolean isDurationEstimated();`,
   `boolean isDateBased();`, `boolean isTimeBased();` and `String toString();`.
2. `java/time/temporal/UnsupportedTemporalTypeException.java`:
   `public class UnsupportedTemporalTypeException extends java.time.DateTimeException` with
   `(String message)` and `(String message, Throwable cause)` (copy `java/time/DateTimeException.java`).
3. `java/time/temporal/ChronoUnit.java`: `public enum ChronoUnit implements TemporalUnit` with the 16 constants in
   this order, each `NAME("Display", duration)`: `NANOS("Nanos", Duration.ofNanos(1))`, `MICROS("Micros",
   ofNanos(1000))`, `MILLIS("Millis", ofNanos(1000_000))`, `SECONDS("Seconds", ofSeconds(1))`, `MINUTES("Minutes",
   ofSeconds(60))`, `HOURS("Hours", ofSeconds(3600))`, `HALF_DAYS("HalfDays", ofSeconds(43200))`, `DAYS("Days",
   ofSeconds(86400))`, `WEEKS("Weeks", ofSeconds(7 * 86400L))`, `MONTHS("Months", ofSeconds(31556952L / 12))`,
   `YEARS("Years", ofSeconds(31556952L))`, `DECADES("Decades", ofSeconds(31556952L * 10L))`,
   `CENTURIES("Centuries", ofSeconds(31556952L * 100L))`, `MILLENNIA("Millennia", ofSeconds(31556952L * 1000L))`,
   `ERAS("Eras", ofSeconds(31556952L * 1000_000_000L))`, `FOREVER("Forever", ofSeconds(Long.MAX_VALUE, 999_999_999))`;
   fields `private final String name; private final Duration duration;`, a private constructor, and
   `getDuration()`, `isDurationEstimated()`, `isDateBased()`, `isTimeBased()`, `toString()`.
4. `java/time/Duration.java` (edit; keep everything else): `public static Duration of(long amount, TemporalUnit unit)`,
   `public Duration plus(long amountToAdd, TemporalUnit unit)`, `public Duration minus(long amountToSubtract,
   TemporalUnit unit)`.
5. `java/util/concurrent/TimeUnit.java` (edit): `public ChronoUnit toChronoUnit()` and
   `public static TimeUnit of(ChronoUnit chronoUnit)`.
6. Do not edit earlier tests. Before this task is queued, the reviewer removes the `rejectsUnusedCall` lines for
   `TimeUnit.toChronoUnit`/`TimeUnit.of` in `BytecodeTimeUnitTest.java` and for `Duration.of(long, TemporalUnit)` in
   `BytecodeDurationTest.java`; if `kit check T52`/`T56` still fails on them, stop and report it.

## Members to leave out

- `TemporalUnit.addTo`, `between`, `isSupportedBy` and the `ChronoUnit` overrides: they need
  `java.time.temporal.Temporal`. The test checks `TemporalUnit.addTo`, `ChronoUnit.addTo`, `ChronoUnit.between` and
  `ChronoUnit.isSupportedBy` stay unlinked. Do not create `Temporal.java`.
- `Duration.get(TemporalUnit)`, `getUnits()`, `truncatedTo(TemporalUnit)`, `addTo`/`subtractFrom`: the test checks
  `get` and `truncatedTo` stay unlinked.
- `TimeUnit.convert(Duration)` stays out (T52 still rejects it).

## JDK behaviour to match

- `ChronoUnit.toString()` returns the display name (`Millis`, `HalfDays`), `name()` the constant name (`MILLIS`).
- `isDurationEstimated()`: `this.compareTo(DAYS) >= 0` (so DAYS and FOREVER are estimated). `isDateBased()`:
  `compareTo(DAYS) >= 0 && this != FOREVER`. `isTimeBased()`: `compareTo(DAYS) < 0`. `getDuration()` returns the same
  stored object each time.
- `Duration.of(amount, unit)`: `return ZERO.plus(amount, unit);`.
- `plus(amountToAdd, unit)`, in this order: `Objects.requireNonNull(unit, "unit")` (message `unit`); if
  `unit == ChronoUnit.DAYS` return `plus(Math.multiplyExact(amountToAdd, 86400), 0)` (the private `(long,long)`
  plus; overflow throws ArithmeticException); if `unit.isDurationEstimated()` throw
  `new UnsupportedTemporalTypeException("Unit must not have an estimated duration")` (also for amount 0);
  if `amountToAdd == 0` return `this`; for a ChronoUnit switch: `NANOS` -> `plusNanos(amountToAdd)`; `MICROS` ->
  `plusSeconds((amountToAdd / (1000_000L * 1000)) * 1000).plusNanos((amountToAdd % (1000_000L * 1000)) * 1000)`;
  `MILLIS` -> `plusMillis(amountToAdd)`; `SECONDS` -> `plusSeconds(amountToAdd)`; otherwise
  `plusSeconds(Math.multiplyExact(unit.getDuration().seconds, amountToAdd))`. For any other TemporalUnit:
  `Duration d = unit.getDuration().multipliedBy(amountToAdd); return plusSeconds(d.getSeconds()).plusNanos(d.getNano());`.
- `minus(amount, unit)`: `amount == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amount, unit)`.
- `TimeUnit.toChronoUnit()`: NANOSECONDS->NANOS, MICROSECONDS->MICROS, MILLISECONDS->MILLIS, SECONDS, MINUTES, HOURS,
  DAYS. `TimeUnit.of(c)`: `Objects.requireNonNull(c, "chronoUnit")` (message `chronoUnit`), the reverse mapping, and
  for every other unit `new IllegalArgumentException("No TimeUnit equivalent for " + c)` (uses the display name:
  `No TimeUnit equivalent for HalfDays`).

## Steps

1. Read the test, `java/time/Duration.java`, `java/time/DateTimeException.java` and
   `java/util/concurrent/TimeUnit.java` under `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create the three `java/time/temporal/` files, then edit `Duration.java` and `TimeUnit.java`.
3. Register the new files (never edit `BytecodeExceptionRuntime.java` by hand):
   - `javaw-agent-kit/bin/kit register java/time/temporal/TemporalUnit.java`
   - `javaw-agent-kit/bin/kit register java/time/temporal/UnsupportedTemporalTypeException.java`
   - `javaw-agent-kit/bin/kit register java/time/temporal/ChronoUnit.java`
4. `javaw-agent-kit/bin/kit check T60` until PASS (also run `kit check T52` and `kit check T56`).
5. Write `docs/bytecode-chrono-unit.md` (below), the report, then `javaw-agent-kit/bin/kit finish T60`.

## Hints

- Imports in `Duration.java`: `java.time.temporal.ChronoUnit`, `TemporalUnit`, `UnsupportedTemporalTypeException`.
  In `TimeUnit.java`: `java.time.temporal.ChronoUnit` and `java.util.Objects`.
- Write `switch((ChronoUnit)unit)` with `case NANOS:` labels (Java 17 source, no pattern `instanceof`); the
  `default` branch may read `unit.getDuration().seconds` directly (same class).
- Do not create a static `DAYS` field in Duration; compare `unit == ChronoUnit.DAYS`. ChronoUnit's initialiser calls
  Duration factories, so Duration's own static initialiser must not touch ChronoUnit.
- Calibration pitfalls: checking `amountToAdd == 0` before `isDurationEstimated()` (the JDK throws for
  `of(0, MONTHS)`); checking estimated before `DAYS` (DAYS is estimated but allowed); `Duration.of(0, DAYS)` must be
  the `ZERO` object (the private plus returns `this` for zero); `TimeUnit.of` message must use `toString()`
  (`HalfDays`), not `name()`.
- The runtime's `Math.multiplyExact` throws `ArithmeticException` without the JDK's `long overflow` message; the
  fixture prints only the class name for ArithmeticException, so do not edit `Math.java`.
- Do not call `ChronoUnit.valueOf(String)` or `Enum.valueOf` from runtime code (it needs `getEnumConstants`).
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constantsAndProperties, durationWithUnits, timeUnitConversions, leavesOutTemporalMembers: if FIX THE RUNTIME says `link the symbolic class/interface java/time/temporal/ChronoUnit` (or `TemporalUnit`, `UnsupportedTemporalTypeException`), `link a complete acyclic type hierarchy` or names one of their methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register` for each of the three new files and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constants, properties: toString returns the display name (`HalfDays`), constants are in JDK order, estimated means compareTo(DAYS) >= 0, date-based excludes FOREVER, and the MONTHS/ERAS/FOREVER durations use 31556952L seconds per year exactly as listed.
durationOf, plusAndMinus: overflow cases print only `java.lang.ArithmeticException`; plus checks null unit, then unit == DAYS (multiplyExact by 86400), then isDurationEstimated (throw even for 0), then amount == 0 returns this; MICROS splits into seconds and nanos; other time units use multiplyExact(unit.getDuration().seconds, amount); minus handles Long.MIN_VALUE as plus(MAX).plus(1).
unitErrors: the null unit message is `unit`; the estimated-unit exception is UnsupportedTemporalTypeException `Unit must not have an estimated duration` with no cause, and it extends java.time.DateTimeException.
timeUnitBridge: TimeUnit.of requires non-null with message `chronoUnit` and throws IllegalArgumentException `No TimeUnit equivalent for ` + the unit's toString() for units other than NANOS..HOURS and DAYS.
leavesOutTemporalMembers: this fails when addTo, between, isSupportedBy, Duration.get(TemporalUnit) or truncatedTo became linked. Do not add Temporal or those members.
```

## Docs

Create `docs/bytecode-chrono-unit.md` with this skeleton (fill in the brackets):

    # Bytecode ChronoUnit

    Revision `chrono-unit-vN` links `java.time.temporal.TemporalUnit`, `ChronoUnit`,
    `UnsupportedTemporalTypeException`, `Duration.of/plus/minus(long, TemporalUnit)` and
    `TimeUnit.toChronoUnit()`/`TimeUnit.of(ChronoUnit)` in the bundled exception runtime.

    Contract:

    - <16 constants with display names and durations; estimated/date/time-based rules; unit-based Duration
      arithmetic with the DAYS exception and UnsupportedTemporalTypeException; the TimeUnit mapping>

    Known differences: TemporalUnit has no addTo/between/isSupportedBy (no Temporal in the runtime).

    Evidence: `BytecodeChronoUnitTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `TemporalUnit.addTo`, `ChronoUnit.addTo/between/isSupportedBy`, `Duration.get(TemporalUnit)` and
    `Duration.truncatedTo` are rejected before WAT generation.
