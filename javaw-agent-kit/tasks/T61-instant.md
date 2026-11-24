# T61 java.time.Instant core subset

```kit
id: T61
slug: instant
title: link java.time.Instant (core subset, no Temporal interfaces)
tests: BytecodeInstantTest
depends: T56
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/Instant.java
allow: docs/bytecode-instant.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-instant-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeInstantTest.java`.

## Goal

Add `java.time.Instant` (the part that needs no `Temporal`, `TemporalAccessor`, `TemporalAmount`, `TemporalUnit`,
`Clock`, zones or formatters) to the bundled runtime. T56 already added `java.time.DateTimeException` and
`Duration`. `runtime-target/target/audit/member-references.tsv` has 63 `java/time/Instant` rows in 10 artifacts.
Uses covered here: `ofEpochMilli(J)` (opcode 184: 9 sites in Hibernate `InstantJavaType.wrap`, 8 in logback-core
`TimeBasedFileNamingAndTriggeringPolicyBase.computeNextCheck`, logback-classic `ILoggingEvent.getInstant`, Spring
web `HttpHeaders.formatDate`, Spring `DateTimeConverters$LongToInstantConverter`, Spring Data `Jsr310Converters`,
Spring Boot `InfoProperties.getInstant`, Jackson `InstantDeserializer`); `ofEpochSecond(J)` (H2
`TimeZoneProvider$WithTimeZone.getShortId`), `ofEpochSecond(JJ)` (H2 `JSR310Utils.valueToInstant`, Jackson
`InstantDeserializer`); the constants `EPOCH` (getstatic, Spring web `DefaultServerWebExchange.addCachingResponseHeaders`,
Spring webmvc `DefaultServerRequest.checkNotModified`, H2 `DateTimeFormatFunction.getTimeOffset`), `MIN` (H2
`JSR310Utils.valueToInstant`, Spring web `DefaultServerWebExchange.checkNotModified`, Jackson `DecimalUtils`) and
`MAX` (H2); `toEpochMilli()` (11 sites in Hibernate `StatisticsImpl.getStartTime`, 6 in logback-core, Spring web
`HttpHeaders.getFirstDate`, Spring Boot `BuildProperties.coerceDate`, Jackson `OffsetDateTimeSerializer`, and a
method handle, opcode -5, in Jackson `InstantSerializer.<init>`); `getEpochSecond()` and `getNano()` (logback-classic
`LoggingEvent.initTmestampFields`, H2 `DateTimeFormatFunction.parseDateTime` and `DateTimeUtils.currentTimestamp`,
Jackson method handles); `isBefore(Instant)` (4 in Spring web `DefaultServerWebExchange.validateIfModifiedSince`,
Spring `CronTrigger.determineLatestTimestamp`), `isAfter(Instant)` (Spring Boot
`SoftReferenceConfigurationPropertyCache.hasExpired`, Spring web, Spring webmvc), `compareTo(Instant)` (Hibernate
`ZonedDateTimeComparator.compare`) and `toString()` (logback-core
`DefaultTimeBasedFileNamingAndTriggeringPolicy.isTriggeringEvent`, Spring `InstantFormatter.print`). The
plusSeconds/plusMillis/plusNanos/minus*, equals and hashCode members make the value type coherent.

Not covered (later tasks, they need types the runtime does not have): `plus(Ljava/time/temporal/TemporalAmount;)`
(6 in Spring `ScheduledTaskRegistrar.scheduleFixedDelayTask`, Spring Boot) and `minus(TemporalAmount)` (Spring web
`InMemoryWebSession.checkExpired`): T56 left `implements TemporalAmount` out of `Duration`, and javac compiles
`instant.plus(duration)` to the `TemporalAmount` descriptor, so a `plus(Duration)` overload would be a non-JDK member
nobody calls. Also `now()` / `now(Clock)` (needs a clock), `parse(CharSequence)` (Spring `InstantFormatter.parse`,
Spring Data; needs the `DateTimeFormatter` parser), `from(TemporalAccessor)`, `atZone(ZoneId)`,
`atOffset(ZoneOffset)`, `plus(long, TemporalUnit)` and `truncatedTo(TemporalUnit)`.

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/time/Instant.java` (`package java.time;`):
`public final class Instant implements Comparable<Instant>, java.io.Serializable` with fields
`private final long seconds; private final int nanos;` (`0 <= nanos <= 999_999_999`) and

- `private static final long MIN_SECOND = -31557014167219200L;`, `MAX_SECOND = 31556889864403199L;`
- `public static final Instant EPOCH`, `MIN`, `MAX`
- `private Instant(long epochSecond, int nanos)`, `private static Instant create(long seconds, int nanoOfSecond)`
- `public static Instant ofEpochSecond(long epochSecond)`
- `public static Instant ofEpochSecond(long epochSecond, long nanoAdjustment)`
- `public static Instant ofEpochMilli(long epochMilli)`
- `public long getEpochSecond()`, `public int getNano()`
- `public Instant plusSeconds(long)`, `plusMillis(long)`, `plusNanos(long)`, and
  `private Instant plus(long secondsToAdd, long nanosToAdd)`
- `public Instant minusSeconds(long)`, `minusMillis(long)`, `minusNanos(long)`
- `public long toEpochMilli()`
- `public int compareTo(Instant other)`, `public boolean isAfter(Instant other)`, `public boolean isBefore(Instant other)`
- `public boolean equals(Object other)`, `public int hashCode()`, `public String toString()`

Private helpers for `toString` (epoch day to year/month/day) are up to you.

## Members to leave out

- Everything that names `java.time.temporal`, `Clock`, `ZoneId`, `ZoneOffset` or formatter types: `plus/minus(TemporalAmount)`,
  `plus/minus(long, TemporalUnit)`, `from(TemporalAccessor)`, `truncatedTo`, `until`, `atZone`, `atOffset`, `get`,
  `getLong`, `isSupported`, `range`, `with`, `adjustInto`, `query`. Do **not** add `implements Temporal,
  TemporalAdjuster` and do not create any `java/time/temporal` file.
- Do not add `plus(Duration)` / `minus(Duration)`: the JDK has no such methods (see Goal).
- `now()`, `now(Clock)`: no clock in this runtime (and not deterministic).
- `parse(CharSequence)`: needs the ISO parser; a later task.
- Serialization (`writeReplace`, `readObject`, `writeExternal`, `readExternal`, `Ser`). Keep `implements java.io.Serializable`.
- No `LocalDate`, `LocalDateTime`, `ZoneOffset` or `DateTimeFormatter`: write the calendar arithmetic by hand.
- Do not edit `Duration.java`, `DateTimeException.java`, `Math.java` or any other runtime file (`kit guard` rejects it).

The test checks that `plus(TemporalAmount)`, `minus(TemporalAmount)`, `now()`, `parse(CharSequence)`,
`truncatedTo(TemporalUnit)` and `from(TemporalAccessor)` stay unlinked.

## JDK behaviour to match

The test prints `getEpochSecond()/getNano()`, `toString()`, `DateTimeException` messages and the class name of other
exceptions. `ArithmeticException` messages are **not** compared (the runtime's `Math.addExact`/`multiplyExact`
throw it without a message).

- Normal form: `seconds` is the floor second, `nanos` in `[0, 999_999_999]`.
- Static initialisation, in this order: `EPOCH = new Instant(0, 0)` (not `create`, which would read the still-null
  `EPOCH`), `MIN = ofEpochSecond(MIN_SECOND, 0)`, `MAX = ofEpochSecond(MAX_SECOND, 999_999_999)`. The constants
  `MIN_SECOND`/`MAX_SECOND` must be declared before them.
- `create(s, n)`: `if ((s | n) == 0) return EPOCH;` (the **same object**: the test checks `ofEpochMilli(0) ==
  Instant.EPOCH`), then `if (s < MIN_SECOND || s > MAX_SECOND) throw new DateTimeException("Instant exceeds minimum or
  maximum instant");`, else `new Instant(s, n)`.
- `ofEpochSecond(s)` = `create(s, 0)`. `ofEpochSecond(s, adj)`: `secs = Math.addExact(s, Math.floorDiv(adj,
  1_000_000_000L)); nos = (int) Math.floorMod(adj, 1_000_000_000L); return create(secs, nos);`
  (`ofEpochSecond(Long.MAX_VALUE, Long.MAX_VALUE)` is an `ArithmeticException`, `ofEpochSecond(Long.MAX_VALUE)` a
  `DateTimeException`). `ofEpochMilli(m)`: `create(Math.floorDiv(m, 1000), (int) Math.floorMod(m, 1000) * 1_000_000)`
  (never throws).
- Private `plus(s, n)`: `if ((s | n) == 0) return this;` (same object) then `epochSec = Math.addExact(seconds, s);
  epochSec = Math.addExact(epochSec, n / 1_000_000_000L); n = n % 1_000_000_000L; return ofEpochSecond(epochSec,
  nanos + n);`. `plusSeconds(s)` = `plus(s, 0)`, `plusMillis(m)` = `plus(m / 1000, (m % 1000) * 1_000_000)`,
  `plusNanos(n)` = `plus(0, n)`. `minusX(v)`: `v == Long.MIN_VALUE ? plusX(Long.MAX_VALUE).plusX(1) : plusX(-v)` for
  Seconds/Millis/Nanos (so `EPOCH.minusSeconds(Long.MIN_VALUE)` is the `DateTimeException` from the first `plusSeconds`).
- `toEpochMilli()`: `if (seconds < 0 && nanos > 0) { long millis = Math.multiplyExact(seconds + 1, 1000); long adj =
  nanos / 1_000_000 - 1000; return Math.addExact(millis, adj); } else { long millis = Math.multiplyExact(seconds, 1000);
  return Math.addExact(millis, nanos / 1_000_000); }`. This drops the sub-millisecond part exactly like the JDK
  (`ofEpochSecond(-1, 1)` gives `-1000`), works for
  `ofEpochMilli(Long.MIN_VALUE)` and throws `ArithmeticException` for `MAX`, `MIN` and one nanosecond-millisecond
  past the `long` range.
- `compareTo`: `Long.compare(seconds, other.seconds)`, and if equal `nanos - other.nanos` (raw difference; the test
  prints it). `isAfter` = `compareTo(other) > 0`, `isBefore` = `compareTo(other) < 0`. `equals`: `this == other`, or
  an `Instant` with both fields equal (`false` for `null` and for a `String`). `hashCode()`:
  `((int) (seconds ^ (seconds >>> 32))) + 51 * nanos`.
- `toString()` is `DateTimeFormatter.ISO_INSTANT.format(this)`: `2011-12-03T10:15:30Z`. Seconds are always printed
  (`1970-01-01T00:00:00Z`). The fraction is printed only when `nanos > 0`, in groups of three digits: `.100`, `.120`,
  `.123400`, `.123456789`, `.000000001`. Copy the JDK's era split (`SECONDS_PER_10000_YEARS = 146097L * 25 * 86400`,
  `SECONDS_0000_TO_1970 = ((146097L * 5) - (30L * 365 + 7)) * 86400`):

      StringBuilder buf = new StringBuilder();
      if (seconds >= -SECONDS_0000_TO_1970) {                       // year 0 or later
          long zeroSecs = seconds - SECONDS_PER_10000_YEARS + SECONDS_0000_TO_1970;
          long hi = Math.floorDiv(zeroSecs, SECONDS_PER_10000_YEARS) + 1;
          long lo = Math.floorMod(zeroSecs, SECONDS_PER_10000_YEARS);
          if (hi > 0) buf.append('+').append(hi);
          appendDateTime(buf, lo - SECONDS_0000_TO_1970);           // year 0000..9999 here
      } else {                                                       // before year 0
          long zeroSecs = seconds + SECONDS_0000_TO_1970;
          long hi = zeroSecs / SECONDS_PER_10000_YEARS;
          long lo = zeroSecs % SECONDS_PER_10000_YEARS;
          int pos = buf.length();
          int year = appendDateTime(buf, lo - SECONDS_0000_TO_1970); // year -10000..0 here
          if (hi < 0) {
              if (year == -10_000) buf.replace(pos, pos + 2, Long.toString(hi - 1));
              else if (lo == 0) buf.insert(pos, Long.toString(hi));
              else buf.insert(pos + 1, Long.toString(Math.abs(hi)));
          }
      }
      if (nanos > 0) {
          buf.append('.');
          int n = nanos, div = 100_000_000;
          for (int i = 0; n > 0 || i % 3 != 0; i++) { int digit = n / div; buf.append((char) ('0' + digit)); n -= digit * div; div /= 10; }
      }
      buf.append('Z');

  `appendDateTime(buf, epochSecond)` appends `yyyy-MM-ddTHH:mm:ss` and returns the year:
  `epochDay = Math.floorDiv(epochSecond, 86400)`, `secsOfDay = (int) Math.floorMod(epochSecond, 86400)`; the date
  from `epochDay` is the JDK `LocalDate.ofEpochDay` algorithm:

      long zeroDay = epochDay + 719528 - 60;   // days from 0000-03-01
      long adjust = 0;
      if (zeroDay < 0) { long cycles = (zeroDay + 1) / 146097 - 1; adjust = cycles * 400; zeroDay += -cycles * 146097; }
      long yearEst = (400 * zeroDay + 591) / 146097;
      long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
      if (doyEst < 0) { yearEst--; doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400); }
      yearEst += adjust;
      int marchDoy0 = (int) doyEst;
      int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
      int month = (marchMonth0 + 2) % 12 + 1;
      int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
      yearEst += marchMonth0 / 10;

  The year is printed like `LocalDate.toString`: if `|year| < 1000`, four digits with a leading `-` when negative
  (`0000`, `-0001`, `-0999`; e.g. append `year - 10000` and delete the `1`, or `year + 10000` and delete the first
  digit); otherwise a `+` when `year > 9999`, then the number (`-1000`, `-10000`). Month, day, hour, minute and second
  have two digits. Results: `0000-01-01T00:00:00Z`, `-0001-12-31T23:59:59Z`, `9999-12-31T23:59:59.999Z`,
  `+10000-01-01T00:00:00Z`, `-10000-01-01T00:00:00Z`, `-10001-12-31T23:59:59Z`, `MIN` is
  `-1000000000-01-01T00:00:00Z`, `MAX` is `+1000000000-12-31T23:59:59.999999999Z`.

## Steps

1. Read the test, then `java/time/Duration.java` and `java/time/DateTimeException.java` (style) and the `floorDiv`,
   `floorMod`, `addExact`, `multiplyExact` lines of `java/lang/Math.java`, all under
   `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `java/time/Instant.java` under `javaw-bytecode/src/main/resources/exception-runtime/` with the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/time/Instant.java`
4. `javaw-agent-kit/bin/kit check T61` until PASS.
5. Write `docs/bytecode-instant.md` (below), the report, then `javaw-agent-kit/bin/kit finish T61`.

## Hints

- `DateTimeException` is in the same package (no import). No other imports are needed.
- Keep the constants local: `86400`, `719528` (days from 0000-01-01 to 1970-01-01), `146097` (days per 400 years),
  `1_000_000_000L`, `1_000_000`. There is no `LocalDate`/`LocalTime` to read them from.
- Use `Math.floorDiv`/`Math.floorMod` for the epoch day and second of day: `/` and `%` give a wrong date for any
  negative epoch second that is not a whole day.
- In the "before year 0" branch `hi` and `lo` use `/` and `%` (truncating), not floorDiv/floorMod; copy it exactly.
- `StringBuilder.insert(int, String)` and `replace(int, int, String)` exist in the runtime; pass `Long.toString(...)`.
- Calibration pitfalls: printing the fraction with trailing zeros removed (Duration style) instead of groups of three
  (`.100` not `.1`); dropping `:00` seconds; `EPOCH` initialised with `create`; `plusMillis(0)` returning a new object.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
factoriesAndRange, epochMillisAndArithmetic, comparing, isoStrings, leavesOutTemporalAndClockMembers: if FIX THE RUNTIME says `link the symbolic class/interface java/time/Instant`, `link a complete acyclic type hierarchy` or names one of its methods, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/time/Instant.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
factories: seconds is the floor second and nanos in [0, 999999999] (Math.floorDiv/floorMod in ofEpochSecond(s, adj) and ofEpochMilli); create() returns the EPOCH object when (seconds | nanos) == 0; EPOCH is new Instant(0, 0) and MIN/MAX are declared after MIN_SECOND/MAX_SECOND.
rangeErrors: create() throws DateTimeException "Instant exceeds minimum or maximum instant" outside [MIN_SECOND, MAX_SECOND]; Math.addExact overflow (ofEpochSecond(s, adj), private plus) stays an ArithmeticException; minusX(Long.MIN_VALUE) is plusX(Long.MAX_VALUE).plusX(1).
epochMillis: toEpochMilli uses the JDK's two branches (seconds < 0 && nanos > 0: multiplyExact(seconds + 1, 1000) plus nanos / 1_000_000 - 1000) with Math.multiplyExact/addExact so overflow throws ArithmeticException.
arithmetic: private plus(s, n) returns this when (s | n) == 0, carries n / 1_000_000_000 into the seconds with addExact and calls ofEpochSecond(epochSec, nanos + n % 1_000_000_000); plusMillis is plus(m / 1000, (m % 1000) * 1_000_000).
comparing: compareTo returns nanos - other.nanos when the seconds are equal; hashCode is ((int)(seconds ^ (seconds >>> 32))) + 51 * nanos; equals is false for null and non-Instant values.
isoStrings: seconds are always printed (`:00`), the fraction only when nanos > 0 and in groups of 3 digits; years below 1000 have 4 digits (`0000`, `-0001`), years above 9999 get `+`; use floorDiv/floorMod for the epoch day and second of day; copy the era split with hi/lo and the replace/insert fix-ups for years before -9999 exactly.
calendarSweep: the date is wrong for some days: copy LocalDate.ofEpochDay exactly (zeroDay = epochDay + 719528 - 60, the 400-year adjust for negative zeroDay, the doyEst < 0 correction, yearEst += marchMonth0 / 10) and use floorDiv/floorMod for negative epoch seconds.
leavesOutTemporalAndClockMembers: this fails when plus/minus(TemporalAmount), now(), parse, truncatedTo or from became linked. Do not add java.time.temporal types, a clock or these methods.
```

## Docs

Create `docs/bytecode-instant.md` with this skeleton (fill in the brackets):

    # Bytecode Instant

    Revision `instant-vN` links `java.time.Instant` (core subset) in the bundled exception runtime.

    Contract:

    - <normalised epoch seconds/nanos value with a shared EPOCH and MIN/MAX; ofEpochSecond/ofEpochMilli factories
      with DateTimeException outside the range and ArithmeticException on long overflow; plus/minus
      Seconds/Millis/Nanos; toEpochMilli; compareTo/isBefore/isAfter/equals/hashCode; ISO_INSTANT toString with a
      hand-written proleptic Gregorian calendar>

    Known differences: no `Temporal`/`TemporalAdjuster` interfaces and no TemporalAmount/TemporalUnit/zone members
    (`plus(Duration)` compiles to `plus(TemporalAmount)` and is not linked); no `now()` (no clock) and no `parse`;
    no serialization proxy. ArithmeticException messages come from the runtime's `Math` and differ from the JDK's.

    Evidence: `BytecodeInstantTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `plus(TemporalAmount)`, `minus(TemporalAmount)`, `now()`, `parse`, `truncatedTo` and `from`
    are rejected before WAT generation.
