# T56 java.time.Duration core subset

```kit
id: T56
slug: duration
title: link java.time.Duration (core subset), DateTimeException and DateTimeParseException
tests: BytecodeDurationTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/Duration.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/DateTimeException.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/format/DateTimeParseException.java
allow: docs/bytecode-duration.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-duration-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeDurationTest.java`.

## Goal

Add `java.time.Duration` (the part that needs no `Temporal`/`TemporalUnit`/`ChronoUnit`), plus
`java.time.DateTimeException` and `java.time.format.DateTimeParseException` for `parse`, to the bundled runtime. The
runtime has no `java.time` classes yet; Duration is the most referenced one (84 rows in
`runtime-target/target/audit/member-references.tsv`). Uses by the pinned libraries: `ZERO` (getstatic, Spring Boot
`SoftReferenceConfigurationPropertyCache.<clinit>`, `KafkaProperties$Listener`, Spring
`ScheduledAnnotationBeanPostProcessor.processScheduledTask`); factories `ofSeconds(J)` (31 sites in Spring Boot
autoconfigure `CassandraAutoConfiguration.<clinit>`, 18 in Spring web `CacheControl.maxAge`), `ofMillis(J)` (11 in
Spring `TaskScheduler.scheduleAtFixedRate`, 9 in `RabbitProperties$Retry`, Jackson `DurationDeserializer`),
`ofMinutes(J)` (14 in `RabbitProperties.<init>`), `ofNanos(J)` (11 in Hibernate `ClockHelper.<clinit>`),
`ofHours(J)` (`Neo4jProperties$Pool`), `ofSeconds(JJ)` (H2 `JSR310Utils.valueToDuration`, Hibernate
`PostgreSQLIntervalSecondJdbcType$2`, Jackson); accessors `getSeconds()` (H2 `JSR310Utils.durationToValue`,
Hibernate `Dialect.appendIntervalLiteral`, Spring `CacheControl.toHeaderValue`, Spring Boot `JdbcTemplateConfiguration`),
`getNano()` (H2, Hibernate, Jackson `DurationSerializer._toNanos`); `toMillis()` (25 sites in Spring Boot
`AbstractRabbitListenerContainerFactoryConfigurer.configure`, Spring `IntervalTask.getInitialDelay`, Jackson),
`toNanos()` (Hibernate `DurationJavaType.unwrap`, Spring `ReschedulingRunnable.schedule`), `toMinutes()` (Spring
Boot `TomcatServletWebServerFactory.getSessionTimeoutInMinutes`), `toHours()` (Jackson `DurationUnitConverter`) and
the `toDays/toHours/toMinutes/toMillis/toNanos/getSeconds` method handles (opcode -5) in Spring Boot
`DurationStyle$Unit.<clinit>` and Jackson `DurationUnitConverter.<clinit>`; `isNegative()` (Spring web
`ResponseCookie.toString`, `ScheduledAnnotationBeanPostProcessor`, Jackson), `isZero()` (Spring Boot
`SoftReferenceConfigurationPropertyCache.setTimeToLive`), `abs()` (Jackson), `equals`/`hashCode` (Spring
`PeriodicTrigger`), `toString()` (Hibernate `DurationJavaType.unwrap`, Spring `DurationFormatter.print`, Spring
Data `Jsr310Converters`, Jackson `DurationSerializer.serialize`) and `parse(CharSequence)` (Hibernate
`DurationJavaType.wrap`, Spring `DurationFormatter.parse`, Spring Boot `DurationStyle$2.parse`, Spring Data,
Jackson `DurationDeserializer._fromString`). The plus/minus/multipliedBy/dividedBy/negated/compareTo members make the
value type coherent. `of(long, TemporalUnit)`, `between(Temporal, Temporal)` and `get(TemporalUnit)` are also
referenced (Spring `ScheduledAnnotationBeanPostProcessor.toDuration`, `ConcurrentTaskScheduler.schedule`,
`PeriodicTrigger.getInitialDelay`) but need `java.time.temporal`, which is a later task.

## Members to implement

1. `javaw-bytecode/src/main/resources/exception-runtime/java/time/DateTimeException.java` (`package java.time;`):
   `public class DateTimeException extends RuntimeException` with `public DateTimeException(String message)` and
   `public DateTimeException(String message, Throwable cause)` (exactly these two constructors).

2. `javaw-bytecode/src/main/resources/exception-runtime/java/time/format/DateTimeParseException.java`
   (`package java.time.format;`): `public class DateTimeParseException extends java.time.DateTimeException` with
   fields `private final String parsedString; private final int errorIndex;` and

   - `public DateTimeParseException(String message, CharSequence parsedData, int errorIndex)`
   - `public DateTimeParseException(String message, CharSequence parsedData, int errorIndex, Throwable cause)`
   - `public String getParsedString()`
   - `public int getErrorIndex()`

   Both constructors store `parsedData.toString()` (a `StringBuilder` argument works).

3. `javaw-bytecode/src/main/resources/exception-runtime/java/time/Duration.java` (`package java.time;`):
   `public final class Duration implements Comparable<Duration>, java.io.Serializable` with fields
   `private final long seconds; private final int nanos;` (`0 <= nanos <= 999_999_999`) and

   - `public static final Duration ZERO`
   - `private Duration(long seconds, int nanos)`, `private static Duration create(long seconds, int nanoAdjustment)`
   - `public static Duration ofDays(long days)`, `ofHours(long hours)`, `ofMinutes(long minutes)`,
     `ofSeconds(long seconds)`, `ofSeconds(long seconds, long nanoAdjustment)`, `ofMillis(long millis)`,
     `ofNanos(long nanos)`
   - `public static Duration parse(CharSequence text)`
   - `public boolean isZero()`, `public boolean isNegative()`, `public long getSeconds()`, `public int getNano()`
   - `public Duration plus(Duration duration)`, `plusDays(long)`, `plusHours(long)`, `plusMinutes(long)`,
     `plusSeconds(long)`, `plusMillis(long)`, `plusNanos(long)`, and `private Duration plus(long secondsToAdd, long nanosToAdd)`
   - `public Duration minus(Duration duration)`, `minusDays(long)`, `minusHours(long)`, `minusMinutes(long)`,
     `minusSeconds(long)`, `minusMillis(long)`, `minusNanos(long)`
   - `public Duration multipliedBy(long multiplicand)`, `public Duration dividedBy(long divisor)`,
     `public Duration negated()`, `public Duration abs()`
   - `public long toDays()`, `toHours()`, `toMinutes()`, `toSeconds()`, `toMillis()`, `toNanos()`
   - `public int compareTo(Duration other)`, `public boolean equals(Object other)`, `public int hashCode()`,
     `public String toString()`

## Members to leave out

- Everything that names `java.time.temporal` types: `of(long, TemporalUnit)`, `from(TemporalAmount)`,
  `between(Temporal, Temporal)`, `get(TemporalUnit)`, `getUnits()`, `plus/minus(long, TemporalUnit)`,
  `addTo/subtractFrom(Temporal)`, `truncatedTo(TemporalUnit)`. `TemporalAmount`, `TemporalUnit`, `ChronoUnit` and
  `Temporal` are not in the runtime; do **not** add `implements TemporalAmount` and do not create any
  `java/time/temporal` file. The test checks that `of(JLTemporalUnit;)`, `between` and `get(TemporalUnit)` stay
  unlinked.
- Java 9+ extras not referenced by the libraries: `dividedBy(Duration)`, `withSeconds`, `withNanos`, `toDaysPart`,
  `toHoursPart`, `toMinutesPart`, `toSecondsPart`, `toMillisPart`, `toNanosPart`, `isPositive`, `getUnits`. The
  test checks `dividedBy(Ljava/time/Duration;)J`, `withSeconds` and `toHoursPart`.
- Serialization (`writeReplace`, `readObject`, `writeExternal`, `readExternal`, the `Ser` class). Keep
  `implements java.io.Serializable`.
- `java.util.regex` is not in the runtime: `parse` must be written by hand (no `Pattern`/`Matcher`).
- Do not edit `Math.java`, `BigDecimal.java`, `Long.java` or any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

The test prints `toString()`, `getSeconds()`/`getNano()` and exception class names, and compares with the JVM.
Overflow messages are **not** compared (the runtime's `Math.multiplyExact`/`addExact` throw
`ArithmeticException` with a different message than the JDK), only the class `java.lang.ArithmeticException`.

- Normal form: `seconds` is the floor second, `nanos` is always in `[0, 999_999_999]`, so -0.001 s is
  `seconds = -1, nanos = 999_000_000`. `create(s, n)` returns the **same `ZERO` object** when `(s | n) == 0`
  (the test checks `Duration.ofMillis(0) == Duration.ZERO`, `d.minus(d) == Duration.ZERO`, etc.), else `new Duration(s, n)`.
- `ofDays(d)` = `create(Math.multiplyExact(d, 86400), 0)`; `ofHours` with 3600, `ofMinutes` with 60 (overflow ->
  `ArithmeticException`). `ofSeconds(s)` = `create(s, 0)`. `ofSeconds(s, adj)`: `secs = Math.addExact(s,
  Math.floorDiv(adj, 1_000_000_000L))`, `nos = (int) Math.floorMod(adj, 1_000_000_000L)`, `create(secs, nos)`
  (`ofSeconds(0, Long.MIN_VALUE)` works; `ofSeconds(Long.MIN_VALUE, -1)` overflows). `ofMillis(m)`: `secs = m / 1000;
  mos = (int)(m % 1000); if (mos < 0) { mos += 1000; secs--; }` then `create(secs, mos * 1_000_000)`. `ofNanos` the
  same with 1_000_000_000.
- `plus(Duration d)` = `plus(d.getSeconds(), d.getNano())`. `plusDays/Hours/Minutes(x)` = `plus(Math.multiplyExact(x,
  86400/3600/60), 0)`, `plusSeconds(s)` = `plus(s, 0)`, `plusMillis(m)` = `plus(m / 1000, (m % 1000) * 1_000_000)`,
  `plusNanos(n)` = `plus(0, n)`. Private `plus(s, n)`: `if ((s | n) == 0) return this;` (same object) then
  `epochSec = Math.addExact(seconds, s); epochSec = Math.addExact(epochSec, n / 1_000_000_000L);
  n = n % 1_000_000_000L; return ofSeconds(epochSec, nanos + n);`.
- `minus(Duration d)`: `s = d.getSeconds(); n = d.getNano(); if (s == Long.MIN_VALUE) return plus(Long.MAX_VALUE,
  -n).plus(1, 0); return plus(-s, -n);`. `minusX(v)`: `v == Long.MIN_VALUE ? plusX(Long.MAX_VALUE).plusX(1) :
  plusX(-v)` for Days/Hours/Minutes/Seconds/Millis/Nanos (so `ofSeconds(-1).minusSeconds(Long.MIN_VALUE)` is
  `Long.MAX_VALUE` seconds, not an overflow).
- `multipliedBy(m)`: `m == 0` returns `ZERO`, `m == 1` returns `this`; otherwise exact arithmetic on the total
  nanoseconds (the JDK uses `BigDecimal`: `create(toBigDecimalSeconds().multiply(BigDecimal.valueOf(m)))`, where
  `toBigDecimalSeconds()` is `BigDecimal.valueOf(seconds).add(BigDecimal.valueOf(nanos, 9))` and
  `create(BigDecimal)` does `BigInteger n = secs.movePointRight(9).toBigIntegerExact(); BigInteger[] qr =
  n.divideAndRemainder(BigInteger.valueOf(1_000_000_000L)); if (qr[0].bitLength() > 63) throw new
  ArithmeticException("Exceeds capacity of Duration: " + n); return ofSeconds(qr[0].longValue(), qr[1].intValue());`).
  The runtime's `BigDecimal`/`BigInteger` have all of these methods.
- `dividedBy(d)`: `d == 0` throws `new ArithmeticException("Cannot divide by zero")` (message compared); `d == 1`
  returns `this`; otherwise the exact quotient **truncated toward zero** in nanoseconds
  (`toBigDecimalSeconds().divide(BigDecimal.valueOf(d), RoundingMode.DOWN)` then `create(BigDecimal)`): `ofNanos(-7).dividedBy(2)`
  is -3 ns (`PT-0.000000003S`), `ofSeconds(Long.MIN_VALUE).dividedBy(-1)` overflows.
- `negated()` = `multipliedBy(-1)` (so `ofSeconds(Long.MIN_VALUE).negated()` throws `ArithmeticException`).
  `abs()` = `isNegative() ? negated() : this`. `isNegative()`: `seconds < 0`. `isZero()`: `(seconds | nanos) == 0`.
- `toDays()` = `seconds / 86400`, `toHours()` = `seconds / 3600`, `toMinutes()` = `seconds / 60` (Java division,
  truncates toward zero on the **seconds field**, so `ofSeconds(-90).toMinutes()` is -1 and `ofNanos(-1).toHours()`
  is 0), `toSeconds()` = `seconds`. Never throw.
- `toMillis()`: `long s = seconds, n = nanos; if (s < 0) { s = s + 1; n = n - 1_000_000_000L; } return
  Math.addExact(Math.multiplyExact(s, 1000), n / 1_000_000);`. `toNanos()` the same with `1_000_000_000L` and `+ n`.
  This handles `ofMillis(Long.MIN_VALUE).toMillis()` and `ofNanos(Long.MIN_VALUE).toNanos()` without overflow and
  throws `ArithmeticException` when the result does not fit.
- `compareTo`: `Long.compare(seconds, other.seconds)`, and if equal `nanos - other.nanos` (the test prints the raw
  value, e.g. `-1`). `equals`: same class and both fields equal (`false` for `null` and for a `String`).
  `hashCode()`: `((int) (seconds ^ (seconds >>> 32))) + (51 * nanos)`.
- `toString()` (ISO-8601, hours are the largest unit, never days):

      if (this == ZERO) return "PT0S";
      long effectiveTotalSecs = seconds;
      if (seconds < 0 && nanos > 0) effectiveTotalSecs++;
      long hours = effectiveTotalSecs / 3600;
      int minutes = (int) ((effectiveTotalSecs % 3600) / 60);
      int secs = (int) (effectiveTotalSecs % 60);
      StringBuilder buf = new StringBuilder(24);
      buf.append("PT");
      if (hours != 0) buf.append(hours).append('H');
      if (minutes != 0) buf.append(minutes).append('M');
      if (secs == 0 && nanos == 0 && buf.length() > 2) return buf.toString();
      if (seconds < 0 && nanos > 0) { if (secs == 0) buf.append("-0"); else buf.append(secs); }
      else buf.append(secs);
      if (nanos > 0) {
          int pos = buf.length();
          if (seconds < 0) buf.append(2 * 1_000_000_000L - nanos); else buf.append(nanos + 1_000_000_000L);
          while (buf.charAt(buf.length() - 1) == '0') buf.setLength(buf.length() - 1);
          buf.setCharAt(pos, '.');
      }
      buf.append('S');
      return buf.toString();

  Examples: `PT8H6M12.345S`, `PT25H`, `PT24H` (one day), `PT-1H-1M`, `PT1H`, `PT-0.001S`, `PT-0.999999999S`,
  `PT-1M-0.5S`, `PT0.000000001S`, `PT1M0.00000001S`.
- `parse(CharSequence text)`: `Objects.requireNonNull(text, "text")` (NPE message `text`). The JDK matches the whole
  text against this case-insensitive regex (groups numbered):

      ([-+]?)P(?:([-+]?[0-9]+)D)?(T(?:([-+]?[0-9]+)H)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)(?:[.,]([0-9]{0,9}))?S)?)?
        1          2                    4                    5                    6              7
      group 3 = the whole "T..." part

  Write it as a hand parser (left to right: optional sign, `P`/`p`, optional number+`D`, optional `T` followed by
  optional number+`H`, number+`M`, number[`.`|`,` 0-9 digits]+`S`, in that order, then end of text; a number is an
  optional `+`/`-` and at least one ASCII digit; letters in either case). Reject (below) when it does not match,
  when group 3 is exactly the one character `'T'` (uppercase only: `"P1DT"` is rejected but `"P1Dt"` parses as one
  day, a JDK quirk), or when none of groups 2, 4, 5, 6 is present (`"P"`, `"PT"`). Otherwise:

      long daysAsSecs = parseNumber(text, dayStart, dayEnd, 86400, "days");
      long hoursAsSecs = parseNumber(text, hourStart, hourEnd, 3600, "hours");
      long minsAsSecs = parseNumber(text, minuteStart, minuteEnd, 60, "minutes");
      long seconds = parseNumber(text, secondStart, secondEnd, 1, "seconds");
      boolean negativeSecs = secondStart >= 0 && text.charAt(secondStart) == '-';
      int nanos = parseFraction(text, fractionStart, fractionEnd, negativeSecs ? -1 : 1);
      try {
          long total = Math.addExact(daysAsSecs, Math.addExact(hoursAsSecs, Math.addExact(minsAsSecs, seconds)));
          return negate ? ofSeconds(total, nanos).negated() : ofSeconds(total, nanos);
      } catch (ArithmeticException ex) {
          throw new DateTimeParseException("Text cannot be parsed to a Duration: overflow", text, 0, ex);
      }

  `parseNumber` returns 0 for an absent group, else `Math.multiplyExact(Long.parseLong(text, start, end, 10),
  multiplier)`, and turns a `NumberFormatException` or `ArithmeticException` into `new DateTimeParseException("Text
  cannot be parsed to a Duration: " + errorText, text, 0, ex)`. `parseFraction` returns 0 for an absent or empty
  group, else `Integer.parseInt(text, start, end, 10)` multiplied by 10 for each missing digit up to 9, times
  `negate` (so `PT-0.5S` is -0.5 s and `PT-1.000000001S` is -1.000000001 s; the fraction sign follows the
  **seconds** sign, not the leading `-`). Rejection: `new DateTimeParseException("Text cannot be parsed to a
  Duration", text, 0)` (error index always 0, no cause). A leading `-` negates the whole result (`-PT-6H+3M` is
  `PT5H57M`).

## Steps

1. Read the test, then `java/lang/IllegalStateException.java` (exception style), `java/math/BigDecimal.java`,
   `java/math/BigInteger.java`, and the `multiplyExact`, `addExact`, `floorDiv`, `floorMod` lines of
   `java/lang/Math.java`, all under `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `java/time/DateTimeException.java`, `java/time/format/DateTimeParseException.java` and
   `java/time/Duration.java` under `javaw-bytecode/src/main/resources/exception-runtime/` with the members above.
3. Register all three (do not edit `BytecodeExceptionRuntime.java` by hand):
   - `javaw-agent-kit/bin/kit register java/time/DateTimeException.java`
   - `javaw-agent-kit/bin/kit register java/time/format/DateTimeParseException.java`
   - `javaw-agent-kit/bin/kit register java/time/Duration.java`
4. `javaw-agent-kit/bin/kit check T56` until PASS.
5. Write `docs/bytecode-duration.md` (below), the report, then `javaw-agent-kit/bin/kit finish T56`.

## Hints

- Imports in `Duration.java`: `java.math.BigDecimal`, `java.math.BigInteger`, `java.math.RoundingMode`,
  `java.time.format.DateTimeParseException`, `java.util.Objects`. `DateTimeParseException.java` imports
  `java.time.DateTimeException`.
- `Long.parseLong(CharSequence, int, int, int)` and `Integer.parseInt(CharSequence, int, int, int)` exist in the
  runtime and accept a leading `+` or `-`. `Math.floorDiv(long,long)` and `Math.floorMod(long,long)` exist.
- Keep the constants local: `86400`, `3600`, `60`, `1_000_000_000L`, `1_000_000`. There is no `LocalTime` to read
  them from.
- `ZERO` must be initialised with `new Duration(0, 0)` (not `create`, which would read the still-null `ZERO`).
- The hand parser must also reject: `""`, `"-P"`, `"PT1H1H"`, `"PT1M1H"` (wrong order), `"P1H"` (H before T),
  `"PT1D"`, `"PT.5S"` (digits needed before the point), `"PT1HS"`, `"PT1.1234567890S"` (more than 9 fraction digits),
  `"PT1.-5S"`, and any leading/trailing space. `"PT1.S"` (point, no digits) is valid. A simple way: walk with an
  index, after each number look at the next character to decide which group it is, and require `i == length` at the end.
- `"PT9223372036854775808S"` fails in `Long.parseLong` (message `...: seconds`), `"P106751991167301D"` fails in
  `multiplyExact` (`...: days`), `"P1DT9223372036854775807S"` fails in `addExact` (`...: overflow`), and
  `"-PT-9223372036854775808S"` fails in `negated()` (`...: overflow`). All have the original exception as cause.
- Calibration pitfalls: `toString` of `ofSeconds(-1, 1)` must print `PT-0.999999999S` (the `-0` branch);
  `ofSeconds(60, 10)` prints `PT1M0.00000001S` (the fraction is written as `nanos + 1_000_000_000` and the leading
  `1` replaced by `.`); returning a new object instead of `this`/`ZERO` breaks the `==` checks.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
factoriesAndText, arithmetic, conversionsAndComparison, parsing, leavesOutTemporalMembers: if FIX THE RUNTIME says `link the symbolic class/interface java/time/Duration` (or `java/time/DateTimeException`, `java/time/format/DateTimeParseException`), `link a complete acyclic type hierarchy` or names one of their methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register` for each of the three files (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
factories: seconds must be the floor second and nanos in [0, 999999999] (use Math.floorDiv/floorMod in ofSeconds(s, adj); ofMillis/ofNanos add 1000 or 1_000_000_000 and decrement secs when the remainder is negative); create() must return the ZERO object when (seconds | nanos) == 0.
factoryOverflow: ofDays/ofHours/ofMinutes use Math.multiplyExact and ofSeconds(s, adj) uses Math.addExact, so overflow throws ArithmeticException.
isoStrings: copy the toString algorithm from the spec exactly: hours (no days), the effectiveTotalSecs++ adjustment and the "-0" branch for negative values with nanos, the fraction written from nanos + 1_000_000_000 (or 2_000_000_000 - nanos when negative) with trailing zeros removed.
plusAndMinus: private plus(s, n) returns this when (s | n) == 0 and carries n / 1_000_000_000 into the seconds; minus and minusX handle Long.MIN_VALUE as plusX(Long.MAX_VALUE).plusX(1).
multiplyDivideNegate: multipliedBy(0) is ZERO and multipliedBy(1)/dividedBy(1) return this; dividedBy truncates toward zero in nanoseconds; dividedBy(0) throws ArithmeticException "Cannot divide by zero"; a result outside long seconds throws ArithmeticException; negated() is multipliedBy(-1).
conversions: toDays/toHours/toMinutes divide the seconds field only; toMillis/toNanos add 1 to negative seconds and subtract 1_000_000_000 from nanos before multiplyExact/addExact.
comparing: compareTo returns nanos - other.nanos when the seconds are equal; hashCode is ((int)(seconds ^ (seconds >>> 32))) + 51 * nanos.
parseValid: letters are case-insensitive, ',' works like '.', a fraction shorter than 9 digits is padded with zeros and takes the sign of the seconds number, a leading '-' negates the whole result, and "P1Dt" (lowercase t alone) is accepted.
parseInvalid: every rejection is DateTimeParseException "Text cannot be parsed to a Duration" with errorIndex 0 and no cause; number errors add ": days"/": hours"/": minutes"/": seconds" with the NumberFormatException/ArithmeticException as cause; an addExact or negated overflow adds ": overflow"; parse(null) throws NullPointerException "text".
exceptionTypes: DateTimeException extends RuntimeException; DateTimeParseException extends DateTimeException and stores parsedData.toString() and errorIndex.
leavesOutTemporalMembers: this fails when a TemporalUnit/Temporal member, dividedBy(Duration), withSeconds or toHoursPart became linked. Do not add java.time.temporal types or these methods.
```

## Docs

Create `docs/bytecode-duration.md` with this skeleton (fill in the brackets):

    # Bytecode Duration

    Revision `duration-vN` links `java.time.Duration` (core subset), `java.time.DateTimeException` and
    `java.time.format.DateTimeParseException` in the bundled exception runtime.

    Contract:

    - <normalised seconds/nanos value with a shared ZERO; ofDays/Hours/Minutes/Seconds/Millis/Nanos factories with
      ArithmeticException on overflow; plus/minus variants; multipliedBy/dividedBy (truncating)/negated/abs;
      toDays..toNanos; compareTo/equals/hashCode; ISO-8601 toString; hand-written parse with the JDK's
      DateTimeParseException messages>

    Known differences: no `TemporalAmount` interface and no TemporalUnit/Temporal members (`of(long, TemporalUnit)`,
    `between`, `get`, `addTo`, ...); no Java 9 `toXxxPart`/`withSeconds`/`dividedBy(Duration)`; no serialization
    proxy. ArithmeticException messages come from the runtime's `Math` and differ from the JDK's.

    Evidence: `BytecodeDurationTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `Duration.of(long, TemporalUnit)`, `between`, `get(TemporalUnit)`, `dividedBy(Duration)`,
    `withSeconds` and `toHoursPart` are rejected before WAT generation.
