# Bytecode ChronoUnit

Revision `chrono-unit-v323` links `java.time.temporal.TemporalUnit`, `ChronoUnit`,
`UnsupportedTemporalTypeException`, `Duration.of/plus/minus(long, TemporalUnit)` and
`TimeUnit.toChronoUnit()`/`TimeUnit.of(ChronoUnit)` in the bundled exception runtime.

Contract:

- `TemporalUnit` declares exactly `getDuration()`, `isDurationEstimated()`, `isDateBased()`,
  `isTimeBased()` and `toString()`; it has no `Temporal`-based members. `UnsupportedTemporalTypeException`
  extends `java.time.DateTimeException` with `(String)` and `(String,Throwable)` constructors.
- `ChronoUnit` is an enum with the 16 constants in JDK order, each storing a display name and a
  `Duration`: `NANOS`/`MICROS`/`MILLIS` from `ofNanos(1/1000/1_000_000)`, `SECONDS`/`MINUTES`/`HOURS`/
  `HALF_DAYS`/`DAYS`/`WEEKS` from `ofSeconds` (86400 and 7 * 86400), `MONTHS` from
  `ofSeconds(31556952L / 12)`, `YEARS`/`DECADES`/`CENTURIES`/`MILLENNIA`/`ERAS` from multiples of
  `ofSeconds(31556952L)` (times 1, 10, 100, 1000, 1_000_000_000), and `FOREVER` from
  `ofSeconds(Long.MAX_VALUE, 999_999_999)`. `toString()` returns the display name (`Millis`, `HalfDays`)
  while `name()` (inherited from `Enum`) returns the constant name (`MILLIS`). `getDuration()` returns the
  same stored object each time.
- `isDurationEstimated()` is `compareTo(DAYS) >= 0` (so `DAYS` through `FOREVER` are estimated),
  `isDateBased()` is `compareTo(DAYS) >= 0 && this != FOREVER`, and `isTimeBased()` is
  `compareTo(DAYS) < 0`.
- `Duration.of(amount, unit)` is `ZERO.plus(amount, unit)`. `plus(amountToAdd, unit)` checks, in order:
  null unit (`NullPointerException`, message `unit`); `unit == ChronoUnit.DAYS` returns
  `plus(Math.multiplyExact(amountToAdd, 86400), 0)` (overflow throws `ArithmeticException`, and a zero
  amount returns `this`, so `Duration.of(0, DAYS)` is `ZERO`); `unit.isDurationEstimated()` throws
  `UnsupportedTemporalTypeException("Unit must not have an estimated duration")` (also for amount 0);
  `amountToAdd == 0` returns `this`; a `ChronoUnit` `NANOS`/`MILLIS`/`SECONDS` maps to `plusNanos`/
  `plusMillis`/`plusSeconds`, `MICROS` splits into `plusSeconds((a / 10^9) * 1000).plusNanos((a % 10^9) * 1000)`,
  and every other `ChronoUnit` uses `plusSeconds(Math.multiplyExact(unit.getDuration().seconds, amount))`;
  any other `TemporalUnit` uses `unit.getDuration().multipliedBy(amountToAdd)` then
  `plusSeconds(d.getSeconds()).plusNanos(d.getNano())`. `minus(amountToSubtract, unit)` is
  `amount == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-amount, unit)`.
- `TimeUnit.toChronoUnit()` maps `NANOSECONDS`..`DAYS` to `NANOS`..`DAYS`; `TimeUnit.of(ChronoUnit)`
  requires non-null (message `chronoUnit`), maps back the same seven pairs, and throws
  `IllegalArgumentException("No TimeUnit equivalent for " + unit)` (the display name, e.g. `HalfDays`) for
  every other unit.

Known differences: TemporalUnit has no addTo/between/isSupportedBy (no Temporal in the runtime). Also not linked:
`Duration.get`/`getUnits`/`truncatedTo`/`addTo`/`subtractFrom` and `TimeUnit.convert(Duration)`.

Evidence: `BytecodeChronoUnitTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `TemporalUnit.addTo`, `ChronoUnit.addTo/between/isSupportedBy`, `Duration.get(TemporalUnit)` and
`Duration.truncatedTo` are rejected before WAT generation.
