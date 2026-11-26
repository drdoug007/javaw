# T82 java.time.LocalTime core subset

```kit
id: T82
slug: local-time
title: link java.time.LocalTime (core subset without Temporal/TemporalAccessor)
tests: BytecodeLocalTimeTest
depends: T70
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/LocalTime.java
allow: docs/bytecode-local-time.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-local-time-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeLocalTimeTest.java`.

## Goal

Add `java.time.LocalTime` (a time of day with nanosecond precision) to the bundled runtime, without the
`Temporal`, `TemporalAccessor` and `TemporalAdjuster` interfaces. `runtime-target/target/audit/member-references.tsv`
lists 28 `java/time/LocalTime` rows in 5 artifacts. This task links the ones that need no missing type:
`of(II)`, `of(III)`, `of(IIII)` (opcode 184 in `jackson-datatype-jsr310:2.15.4` and `hibernate-core:6.4.4.Final`),
`getHour()`, `getMinute()`, `getSecond()` (Jackson jsr310), `getNano()` (Jackson jsr310 and Hibernate, 5 sites),
`ofNanoOfDay(J)` (H2 `2.2.224`, 3 sites), `toNanoOfDay()` (H2, 6 sites, `JSR310Utils`/`DateTimeUtils`) and
`parse(Ljava/lang/CharSequence;)` (Spring `spring-context:6.1.5`, `@DateTimeFormat` binding). The constants,
`ofSecondOfDay`, `toSecondOfDay`, `with*`, `plus*`/`minus*`, comparison, `equals`/`hashCode` and `toString` make the
value type coherent (`toString` is what Jackson and Spring print). The remaining audit rows (`now()`, `now(Clock)`,
`from(TemporalAccessor)`, `get(TemporalField)`, `with(TemporalField, long)`, `atDate(LocalDate)`,
`atOffset(ZoneOffset)`, `ofInstant(Instant, ZoneId)`, `format(DateTimeFormatter)`,
`parse(CharSequence, DateTimeFormatter)`) need a clock or types that are not in the runtime.

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/time/LocalTime.java` (`package java.time;`):
`public final class LocalTime implements Comparable<LocalTime>, java.io.Serializable` with fields
`private final byte hour; private final byte minute; private final byte second; private final int nano;` and

- `private static final LocalTime[] HOURS` (24 cached whole-hour values, filled in a `static {}` block **before**
  the constants below)
- `public static final LocalTime MIN`, `MAX`, `MIDNIGHT`, `NOON`
- `private LocalTime(int hour, int minute, int second, int nano)`, `private static LocalTime create(int hour, int minute, int second, int nano)`
- `public static LocalTime of(int hour, int minute)`, `of(int hour, int minute, int second)`,
  `of(int hour, int minute, int second, int nanoOfSecond)`
- `public static LocalTime ofSecondOfDay(long secondOfDay)`, `public static LocalTime ofNanoOfDay(long nanoOfDay)`
- `public static LocalTime parse(CharSequence text)`
- `public int getHour()`, `getMinute()`, `getSecond()`, `getNano()`, `public int toSecondOfDay()`, `public long toNanoOfDay()`
- `public LocalTime withHour(int)`, `withMinute(int)`, `withSecond(int)`, `withNano(int)`
- `public LocalTime plusHours(long)`, `plusMinutes(long)`, `plusSeconds(long)`, `plusNanos(long)`,
  `minusHours(long)`, `minusMinutes(long)`, `minusSeconds(long)`, `minusNanos(long)`
- `public int compareTo(LocalTime)`, `public boolean isBefore(LocalTime)`, `public boolean isAfter(LocalTime)`
- `public boolean equals(Object)`, `public int hashCode()`, `public String toString()`

## Members to leave out

- `now()`, `now(Clock)`, `now(ZoneId)` (need a clock), `from(TemporalAccessor)`, `get`/`getLong`/`isSupported`/
  `range(TemporalField)`, `with(TemporalField, long)`, `with(TemporalAdjuster)`, `plus`/`minus(TemporalAmount)` and
  `(long, TemporalUnit)`, `truncatedTo(TemporalUnit)`, `until`, `query`, `adjustInto`, `atDate(LocalDate)`,
  `atOffset(ZoneOffset)`, `ofInstant`, `toEpochSecond`, `format(DateTimeFormatter)`,
  `parse(CharSequence, DateTimeFormatter)`. Do **not** add `implements Temporal`/`TemporalAccessor`/`TemporalAdjuster`
  and do not create any other `java/time` file (`LocalDate`, `ChronoField`, `ValueRange`, `DateTimeFormatter`, ...).
  The test checks that `now`, `from`, `atDate`, `format`, `parse(CharSequence, DateTimeFormatter)`,
  `with(TemporalField, long)` and `truncatedTo` stay unlinked.
- Serialization (`writeReplace`, `readObject`, the `Ser` class). Keep `implements java.io.Serializable`.
- Do not edit `DateTimeException.java`, `DateTimeParseException.java` or any other runtime file (`kit guard`
  rejects it).

## JDK behaviour to match

The test prints `toString()`, the four getters, `DateTimeException`/`DateTimeParseException` messages, error
indices, parsed strings and cause class + message, and compares them with the JVM (JDK 17/21 behave the same).

### Values and caching

- `HOURS[i] = new LocalTime(i, 0, 0, 0)` for `i = 0..23`; `MIDNIGHT = MIN = HOURS[0]`, `NOON = HOURS[12]`,
  `MAX = new LocalTime(23, 59, 59, 999_999_999)`. Declare `HOURS` and its `static {}` loop above the constants
  (static initialisers run in source order).
- `create(h, m, s, n)`: `(m | s | n) == 0 ? HOURS[h] : new LocalTime(h, m, s, n)`. Every factory and every
  method that builds a new value goes through `create` (the test checks `LocalTime.of(12, 0) == NOON`,
  `ofSecondOfDay(3600) == of(1, 0)`, `MAX.plusNanos(1) == MIDNIGHT`, `parse("05:00") == of(5, 0)`).

### Validation messages

Every invalid field throws `new DateTimeException("Invalid value for " + name + " (valid values 0 - " + max + "): " + value)`
(`value` printed as a `long`). Names and maxima:

| field | name | max |
|---|---|---|
| hour | `HourOfDay` | `23` |
| minute | `MinuteOfHour` | `59` |
| second | `SecondOfMinute` | `59` |
| nano | `NanoOfSecond` | `999999999` |
| `ofSecondOfDay` | `SecondOfDay` | `86399` |
| `ofNanoOfDay` | `NanoOfDay` | `86399999999999` |

Example: `LocalTime.of(24, 0)` -> `Invalid value for HourOfDay (valid values 0 - 23): 24`. Write one private
helper `check(String name, long max, long value)`.

- `of(h, m)`: check hour; if `m == 0` return `HOURS[h]`; check minute.
- `of(h, m, s)`: check hour; if `(m | s) == 0` return `HOURS[h]`; check minute, then second.
- `of(h, m, s, n)`: check hour, minute, second, nano **in that order** (`of(24, 60, 60, -1)` reports the hour,
  `of(1, 60, 60, -1)` the minute), then `create`.
- `ofSecondOfDay(sod)`: check `SecondOfDay` (also for huge/negative `long`s), then split into h/m/s.
- `ofNanoOfDay(nod)`: check `NanoOfDay`, then split with `long` division by `3_600_000_000_000L`,
  `60_000_000_000L`, `1_000_000_000L`.
- `toSecondOfDay()` = `hour * 3600 + minute * 60 + second`; `toNanoOfDay()` = the same in nanos, computed in `long`
  plus `nano`.
- `withHour(v)`: `v == hour ? this : (check hour; create(v, minute, second, nano))`; same for minute/second/nano.
  The check runs only when the value differs.

### plus / minus (wrap around midnight, never throw)

    plusHours(h):   h == 0 -> this; newHour = ((int) (h % 24) + hour + 24) % 24; create(newHour, minute, second, nano)
    plusMinutes(m): m == 0 -> this; mofd = hour * 60 + minute; newMofd = ((int) (m % 1440) + mofd + 1440) % 1440;
                    mofd == newMofd -> this; create(newMofd / 60, newMofd % 60, second, nano)
    plusSeconds(s): s == 0 -> this; sofd = toSecondOfDay(); newSofd = ((int) (s % 86400) + sofd + 86400) % 86400;
                    sofd == newSofd -> this; create(newSofd / 3600, (newSofd / 60) % 60, newSofd % 60, nano)
    plusNanos(n):   n == 0 -> this; nofd = toNanoOfDay(); newNofd = ((n % NANOS_PER_DAY) + nofd + NANOS_PER_DAY) % NANOS_PER_DAY;
                    nofd == newNofd -> this; otherwise the value of ofNanoOfDay(newNofd)
    minusHours(h)   = plusHours(-(h % 24));      minusMinutes(m) = plusMinutes(-(m % 1440));
    minusSeconds(s) = plusSeconds(-(s % 86400)); minusNanos(n)   = plusNanos(-(n % NANOS_PER_DAY))

`NANOS_PER_DAY = 86_400_000_000_000L`. Take the remainder **before** adding (`Long.MAX_VALUE`/`Long.MIN_VALUE`
must not overflow); `plusHours(24)` returns a new object equal to `this` from `create`, while `plusMinutes(1440)`
returns `this` (the test checks `==` for cases where both agree; use exactly the formulas above).

### Comparison, equality, hash

- `compareTo`: `Integer.compare` of hour, then minute, then second, then nano (the result is -1/0/1).
  `isBefore(o)` = `compareTo(o) < 0`, `isAfter(o)` = `compareTo(o) > 0`.
- `equals`: same object or a `LocalTime` with the same four fields; `false` for `null` and a `String`.
- `hashCode()`: `Long.hashCode(toNanoOfDay())` (`(int) (nod ^ (nod >>> 32))`; `MAX.hashCode()` is `-1857113749`).

### toString

`HH:mm`, then `:ss` only when `second > 0 || nano > 0`, then the fraction only when `nano > 0`:
`'.'` + 3 digits if `nano % 1_000_000 == 0`, else 6 digits if `nano % 1000 == 0`, else 9 digits, always
zero-padded on the left (`Integer.toString(nano / 1_000_000 + 1000).substring(1)` etc.). Examples: `00:00`,
`09:05`, `23:59:01`, `10:00:00.000000001`, `10:00:00.000001`, `10:00:00.001`, `10:00:00.100`, `10:00:00.123400`,
`01:02:03.004005`, `23:59:59.999999999`, `00:10:00.500`. Cast the `byte` fields to `int` before appending (a
`StringBuilder.append(byte)` resolves to `append(int)` anyway, but be explicit).

### parse(CharSequence) (ISO_LOCAL_TIME, strict)

`text == null` throws `NullPointerException` with message `text`. A digit is only `'0'..'9'` (never
`Character.isDigit`; a full-width `'１'` is rejected). Use only `length()`, `charAt()`, `subSequence()` and
`toString()` on `text` (a `StringBuilder` must work). Error messages use `abbr` exactly as in
[T70](T70-instant-parse.md): `text.toString()`, or the first 64 chars plus `"..."` when `length() > 64`.

Grammar, `pos` from 0 ("fail at i" = `new DateTimeParseException("Text '" + abbr + "' could not be parsed at index " + i, text, i)`):

1. Hour: two digits at 0, else fail at 0 (`"1:15"`, `"x0:15"`, `""` -> 0).
2. `':'` at 2, else fail at 2 (`"10"`, `"10-15"` -> 2).
3. Minute: two digits at 3, else fail at 3 (`"10:"`, `"10:1"`, `"10:1x"` -> 3). `pos = 5`.
4. Optional seconds: only when `text.charAt(5) == ':'` **and** two digits follow; then `pos = 8`. Otherwise
   nothing of it is consumed (`"10:15:"`, `"10:15:3"`, `"10:15:3x"` leave `pos = 5`).
5. Optional fraction, only after seconds: if `text.charAt(pos) == '.'`, `pos++`, then read **0 to 9** digits
   (`"10:15:30."` is valid, nano 0); pad the value on the right to 9 digits (`.1` = 100000000). A 10th digit is
   not read. A `','` is not a decimal point.
6. If `pos < len`: `new DateTimeParseException("Text '" + abbr + "' could not be parsed, unparsed text found at index " + pos, text, pos)`
   (`"10:15:"` -> 5, `"10:15:30Z"` -> 8, `"10:15.5"` -> 5, `"10:15:30.x"` -> 9, `"10:15:30.1234567891"` -> 18).
   This comes **before** the value checks (`"23:60x"` reports the unparsed text).
7. Values (strict, `24:00` is **not** accepted): check **minute first, then hour, then second** with the
   validation messages above (`"99:99:99"` reports `MinuteOfHour ... 99`, `"24:00:60"` reports `HourOfDay ... 24`).
   On failure throw `new DateTimeParseException("Text '" + abbr + "' could not be parsed: " + ex.getMessage(), text, 0, ex)`
   where `ex` is the `DateTimeException` from the check (error index 0, the cause's message is compared too).
8. `return create(hour, minute, second, nano)`.

## Steps

1. Read the test, then `java/time/Duration.java`, `java/time/Instant.java` (its `parse` from T70 has the same
   error style) and `java/time/format/DateTimeParseException.java`, all under
   `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `java/time/LocalTime.java` there with the members above (imports: `java.time.format.DateTimeParseException`
   only; `DateTimeException` is in the same package).
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/time/LocalTime.java`
4. `javaw-agent-kit/bin/kit check T82` until PASS.
5. Write `docs/bytecode-local-time.md` (below), the report, then `javaw-agent-kit/bin/kit finish T82`.

## Hints

- `Long.hashCode(long)`, `Integer.compare(int, int)`, `Integer.toString(int)` and `String.substring` exist in the
  runtime.
- The `byte` fields compare fine with `int` arguments (`this.hour == hour`).
- Write a `two(CharSequence text, int pos)` helper that returns `-1` when `pos + 2 > length()` or either char is
  not `'0'..'9'`.
- Calibration pitfalls: the strict resolver checks the minute **before** the hour (`"99:99:99"`, `"24:60"` report
  the minute); unparsed text is reported before any value check; `"10:15:3"` is unparsed text at 5, not a parse
  error at 6; the `'.'` with zero digits is accepted and consumed (`"10:15:30.x"` -> 9); `of(h, m)` and
  `of(h, m, s)` return the cached hour **before** checking a zero minute/second.
- The Wasm runner stops at the first failing fixture line of each `@Test` method; after a fix another fixture may
  fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
factoriesAndValidation, arithmetic, comparisonAndStrings, parsing, leavesOutTemporalMembers: if FIX THE RUNTIME says `link the symbolic class/interface java/time/LocalTime` or names one of its methods, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/time/LocalTime.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constants: MIDNIGHT, MIN and NOON are HOURS[0]/HOURS[12]; create() returns HOURS[h] when (m | s | n) == 0; HOURS must be filled before the constants are assigned (put the static block first); toNanoOfDay is computed in long.
factories: ofSecondOfDay/ofNanoOfDay split the value with integer division; toSecondOfDay is hour*3600+minute*60+second.
invalidValues: the message is `Invalid value for <Name> (valid values 0 - <max>): <value>` with names HourOfDay, MinuteOfHour, SecondOfMinute, NanoOfSecond, SecondOfDay, NanoOfDay; of(h,m,s,n) checks hour, minute, second, nano in that order; withX checks only when the value changes.
withers: withX returns this when the value is unchanged and goes through create() (cached hours) otherwise.
plusMinus: take the remainder (% 24, % 1440, % 86400, % NANOS_PER_DAY) before adding, add the day length and take % again; plusMinutes/Seconds/Nanos return this when the time of day is unchanged; minusX(v) is plusX(-(v % period)).
comparison: compareTo compares hour, minute, second, nano with Integer.compare; hashCode is Long.hashCode(toNanoOfDay()); equals rejects null and non-LocalTime objects; LocalTime is Comparable and Serializable.
strings: seconds are printed when second > 0 or nano > 0; the fraction has 3, 6 or 9 digits (the shortest group size that is exact), left-padded with zeros.
parseValid: seconds and the fraction are optional; `.` with no digits is nano 0; the fraction is padded on the right to 9 digits; the result goes through create().
parseSyntax: a failing field fails at its start index (hour 0, ':' 2, minute 3); an incomplete `:ss` or a 10th fraction digit is not consumed and gives `could not be parsed, unparsed text found at index N`; only '0'..'9' are digits; texts longer than 64 chars are cut to 64 plus `...`; null is NullPointerException "text".
parseValues: unparsed text is reported before values; values are checked minute, hour, second (24:00 is invalid) and the error is `Text '<text>' could not be parsed: <DateTimeException message>` with index 0 and the DateTimeException as cause.
leavesOutTemporalMembers: this fails when now, from, atDate, format, parse(CharSequence, DateTimeFormatter), with(TemporalField, long) or truncatedTo became linked. Do not add Temporal types or these methods.
```

## Docs

Create `docs/bytecode-local-time.md` with this skeleton (fill in the brackets):

    # Bytecode LocalTime

    Revision `local-time-vN` links `java.time.LocalTime` (core subset) in the bundled exception runtime.

    Contract:

    - <hour/minute/second/nano value with 24 cached whole hours (MIDNIGHT, NOON, MIN) and MAX; of/ofSecondOfDay/
      ofNanoOfDay with the JDK's "Invalid value for ..." DateTimeException messages; with/plus/minus wrapping
      around midnight; compareTo/isBefore/isAfter/equals/hashCode (JDK formula); ISO toString with 3/6/9 digit
      fractions; hand-written strict ISO_LOCAL_TIME parse with the JDK's DateTimeParseException messages>

    Known differences: no `Temporal`/`TemporalAccessor`/`TemporalAdjuster` interfaces and no clock, field, unit,
    date, offset or formatter members (`now`, `from`, `get`, `with(TemporalField, long)`, `truncatedTo`, `atDate`,
    `atOffset`, `format`, `parse(CharSequence, DateTimeFormatter)`, ...); no serialization proxy.

    Evidence: `BytecodeLocalTimeTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `now`, `from`, `atDate`, `format`, `parse(CharSequence, DateTimeFormatter)`,
    `with(TemporalField, long)` and `truncatedTo` are rejected before WAT generation.
