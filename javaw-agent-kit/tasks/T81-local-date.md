# T81 java.time.LocalDate core

```kit
id: T81
slug: local-date
title: link java.time.LocalDate (core subset with a marker ChronoLocalDate; no Temporal, parse or format)
tests: BytecodeLocalDateTest
depends: T80
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/LocalDate.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/chrono/ChronoLocalDate.java
allow: docs/bytecode-local-date.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-local-date-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeLocalDateTest.java`.

## Goal

Add `java.time.LocalDate` (an ISO year-month-day without time or zone) to the bundled runtime, without the
`Temporal`/`TemporalAccessor`/`TemporalAdjuster` interfaces (they are not in the runtime). javac compiles
`a.isBefore(b)`/`a.compareTo(b)` on `LocalDate` against the JDK signatures `(Ljava/time/chrono/ChronoLocalDate;)`,
so the task also adds a member-less marker interface `java.time.chrono.ChronoLocalDate extends
Comparable<ChronoLocalDate>` that `LocalDate` implements.
`runtime-target/target/audit/member-references.tsv` has 34 `java/time/LocalDate` rows in 6 artifacts. This task
covers: `of(III)` (H2 2 sites, Hibernate 2, Jackson jsr310 1), `getYear()`, `getMonthValue()`, `getDayOfMonth()`
(H2 4 sites each, Jackson 1 each), `toEpochDay()` (Hibernate, Jackson 2), `ofEpochDay(J)` (Jackson), `toString()`
(Jackson 2), the `MIN`/`MAX` fields (H2) and `EPOCH` (Hibernate 3), and `minus(J, TemporalUnit)` (Tomcat;
`TemporalUnit` and `ChronoUnit` are in the runtime). The other factories, getters, plus/minus/with members,
`compareTo`/`equals`/`hashCode` and `isBefore/isAfter/isEqual(LocalDate)` make the value type coherent.
Not covered: `parse(CharSequence)` (Spring `spring-context`; a follow-up task), `parse(CharSequence, DateTimeFormatter)`,
`format(DateTimeFormatter)`, `from(TemporalAccessor)`, `now()`, `atStartOfDay()`/`atStartOfDay(ZoneId)` and
`ofInstant(Instant, ZoneId)`. Tomcat's `isBefore(Ljava/time/chrono/ChronoLocalDate;)Z` row is covered through the
marker interface.

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/time/LocalDate.java` (`package java.time;`):
`public final class LocalDate implements ChronoLocalDate, java.io.Serializable` (import
`java.time.chrono.ChronoLocalDate`) with fields
`private final int year; private final short month; private final short day;` and

- `public static final LocalDate MIN` (`-999999999-01-01`), `MAX` (`+999999999-12-31`), `EPOCH` (`1970-01-01`)
- `private LocalDate(int year, int month, int dayOfMonth)` (no checks)
- `public static LocalDate of(int year, int month, int dayOfMonth)`, `of(int year, Month month, int dayOfMonth)`
- `public static LocalDate ofYearDay(int year, int dayOfYear)`, `public static LocalDate ofEpochDay(long epochDay)`
- `public int getYear()`, `getMonthValue()`, `getDayOfMonth()`, `getDayOfYear()`; `public Month getMonth()`;
  `public DayOfWeek getDayOfWeek()`
- `public boolean isLeapYear()`, `public int lengthOfMonth()`, `public int lengthOfYear()`
- `public long toEpochDay()`
- `public LocalDate withYear(int)`, `withMonth(int)`, `withDayOfMonth(int)`, `withDayOfYear(int)`
- `public LocalDate plusYears(long)`, `plusMonths(long)`, `plusWeeks(long)`, `plusDays(long)` and
  `minusYears(long)`, `minusMonths(long)`, `minusWeeks(long)`, `minusDays(long)`
- `public LocalDate plus(long amountToAdd, TemporalUnit unit)`, `public LocalDate minus(long amountToSubtract, TemporalUnit unit)`
- `public int compareTo(ChronoLocalDate other)`, `public boolean isAfter(ChronoLocalDate)`, `isBefore(ChronoLocalDate)`,
  `isEqual(ChronoLocalDate)` (each casts `other` to `LocalDate`; it is the only implementation)
- `public boolean equals(Object)`, `public int hashCode()`, `public String toString()`

`Month` and `DayOfWeek` come from T80. Use only `Month.of(int)`, `month.getValue()`, `Month.name()` and
`DayOfWeek.of(int)` from them; keep your own month-length and first-day-of-year helpers in `LocalDate`.

`javaw-bytecode/src/main/resources/exception-runtime/java/time/chrono/ChronoLocalDate.java`, exactly:

    package java.time.chrono;

    public interface ChronoLocalDate extends Comparable<ChronoLocalDate> {
    }

No methods (the JDK's has ~30, mostly defaults over `Temporal`/`Chronology`); calls through the interface type
(`ChronoLocalDate.isBefore`, `ChronoLocalDate.toEpochDay`) must stay unlinked.

## Members to leave out

- `now()` (clock), `parse(...)`, `format(...)`, `from(TemporalAccessor)`, `atStartOfDay(...)`, `atTime(...)`,
  `ofInstant(...)`, `until(...)`, `datesUntil(...)`, `with(TemporalAdjuster)`, `with(TemporalField, long)`,
  `get/getLong(TemporalField)`, `range`, `query`, `adjustInto`, `isSupported`, `getChronology`, `getEra`, and the
  `plus/minus(TemporalAmount)` overloads. Do **not** add `implements Temporal` or `TemporalAdjuster`, do not give
  `ChronoLocalDate` any methods, and do not create any other `java/time` file (no `LocalDateTime`, `ChronoField`,
  `IsoChronology`, `DateTimeFormatter`). The test checks that `LocalDate.now`, `parse`, `from`, `atStartOfDay`,
  `until`, `format` and `ChronoLocalDate.isBefore`/`toEpochDay` stay unlinked.
- Serialization (`writeReplace`, `readObject`, `Ser`). Keep `implements java.io.Serializable`.
- Do not edit `Month.java`, `DayOfWeek.java`, `Period.java` or any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

The test prints `toString()=year/month/day`, getters, `DateTimeException` class and **message**, and for other
exceptions the class only (`ArithmeticException` messages are not compared). Constants:
`MIN_YEAR = -999_999_999`, `MAX_YEAR = 999_999_999`, `DAYS_PER_CYCLE = 146097L`, `DAYS_0000_TO_1970 = 719528L`,
epoch-day range `-365243219162L .. 365241780471L` (`MIN.toEpochDay()` / `MAX.toEpochDay()`).

### Validation messages (all `new DateTimeException(msg)`, checks in this order)

- Year outside `MIN_YEAR..MAX_YEAR` (`v` printed as a long): `Invalid value for Year (valid values -999999999 - 999999999): v`
- Month outside 1..12: `Invalid value for MonthOfYear (valid values 1 - 12): v`
- Day of month outside 1..31: `Invalid value for DayOfMonth (valid values 1 - 28/31): v`
- Day of year outside 1..366: `Invalid value for DayOfYear (valid values 1 - 365/366): v`
- Epoch day out of range: `Invalid value for EpochDay (valid values -365243219162 - 365241780471): v`
- `of`: after the three range checks, if `day > 28 && day > lengthOfMonth`: day 29 (only February can fail with 29)
  -> `Invalid date 'February 29' as '<year>' is not a leap year`; otherwise
  `"Invalid date '" + Month.of(month).name() + " " + day + "'"` (upper case: `Invalid date 'FEBRUARY 30'`,
  `Invalid date 'APRIL 31'`).
- `ofYearDay(2023, 366)`: `Invalid date 'DayOfYear 366' as '2023' is not a leap year`.
- `of(int, Month, int)`: `Objects.requireNonNull(month, "month")` (NPE), then `of(year, month.getValue(), day)`.
- Leap year: `(y & 3) == 0 && (y % 100 != 0 || y % 400 == 0)` (works for negative years: -4 and 0 are leap, -1 is not).

### Conversions (JDK algorithms, use `long` arithmetic)

`toEpochDay()`:

    long y = year, m = month, total = 365 * y;
    if (y >= 0) total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
    else total -= y / -4 - y / -100 + y / -400;
    total += (367 * m - 362) / 12;
    total += day - 1;
    if (m > 2) { total--; if (!isLeapYear()) total--; }
    return total - DAYS_0000_TO_1970;

`ofEpochDay(epochDay)`: range check, then

    long zeroDay = epochDay + DAYS_0000_TO_1970 - 60;   // days from 0000-03-01
    long adjust = 0;
    if (zeroDay < 0) { long cycles = (zeroDay + 1) / DAYS_PER_CYCLE - 1; adjust = cycles * 400; zeroDay += -cycles * DAYS_PER_CYCLE; }
    long yearEst = (400 * zeroDay + 591) / DAYS_PER_CYCLE;
    long doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400);
    if (doyEst < 0) { yearEst--; doyEst = zeroDay - (365 * yearEst + yearEst / 4 - yearEst / 100 + yearEst / 400); }
    yearEst += adjust;
    int marchDoy0 = (int) doyEst;
    int marchMonth0 = (marchDoy0 * 5 + 2) / 153;
    int month = (marchMonth0 + 2) % 12 + 1;
    int dom = marchDoy0 - (marchMonth0 * 306 + 5) / 10 + 1;
    yearEst += marchMonth0 / 10;
    return new LocalDate((int) yearEst, month, dom);

`ofYearDay(year, doy)`: year check, day-of-year check, the 366 check, then `m = (doy - 1) / 31 + 1`; if
`doy > firstDayOfYear(m) + lengthOfMonth(m) - 1` then `m++`; `dom = doy - firstDayOfYear(m) + 1`.
`firstDayOfYear` (1-based) for January..December is `1, 32, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335`,
plus 1 from March on in a leap year. `getDayOfYear()` = `firstDayOfYear(month) + day - 1`.
`getDayOfWeek()` = `DayOfWeek.of((int) Math.floorMod(toEpochDay() + 3, 7L) + 1)` (1970-01-01 is a Thursday).
`getMonth()` = `Month.of(month)`. `lengthOfMonth`: February 28/29, April, June, September, November 30, else 31.

### Arithmetic

Private helper `resolvePreviousValid(y, m, d)`: `new LocalDate(y, m, Math.min(d, lengthOfMonth(y, m)))`
(month-end clamping: `2024-01-31.plusMonths(1)` is `2024-02-29`, `2024-02-29.plusYears(1)` is `2025-02-28`).

- `plusYears(n)`: `n == 0` returns `this`; else `resolvePreviousValid(checkYear(year + n), month, day)` (plain
  long `+`, then the Year range check with the long value in the message).
- `plusMonths(n)`: `n == 0` -> `this`; `long calc = year * 12L + (month - 1) + n;` (plain `+`)
  `newYear = checkYear(Math.floorDiv(calc, 12))`, `newMonth = (int) Math.floorMod(calc, 12) + 1`, then
  `resolvePreviousValid`.
- `plusWeeks(n)` = `plusDays(Math.multiplyExact(n, 7))`.
- `plusDays(n)` (the JDK 21 fast path matters for the messages): `n == 0` -> `this`; `long dom = day + n;` if
  `dom > 0`: `dom <= 28` -> `new LocalDate(year, month, (int) dom)`; else if `dom <= 59`: with `len = lengthOfMonth()`,
  `dom <= len` -> same month; else `month < 12` -> `new LocalDate(year, month + 1, (int) (dom - len))`; else
  `new LocalDate(checkYear(year + 1L), 1, (int) (dom - len))` (so `MAX.plusDays(1)` reports **Year** 1000000000, not
  EpochDay). Otherwise `ofEpochDay(Math.addExact(toEpochDay(), n))` (overflow -> `ArithmeticException`, out of range
  -> the EpochDay message: `MIN.minusDays(1)`).
- `minusX(n)` = `n == Long.MIN_VALUE ? plusX(Long.MAX_VALUE).plusX(1) : plusX(-n)` for all four.
- `plus(n, unit)`: `ChronoUnit.DAYS` -> `plusDays`, `WEEKS` -> `plusWeeks`, `MONTHS` -> `plusMonths`, `YEARS` ->
  `plusYears`, `DECADES`/`CENTURIES`/`MILLENNIA` -> `plusYears(Math.multiplyExact(n, 10 / 100 / 1000))`; every
  other unit (time units, `FOREVER`, any non-`ChronoUnit`) throws
  `new UnsupportedTemporalTypeException("Unsupported unit: " + unit)` (`Unsupported unit: Hours`). `ERAS` is a known
  difference (the JDK supports it; not tested). `minus(n, unit)` = `n == Long.MIN_VALUE ? plus(Long.MAX_VALUE, unit).plus(1, unit) : plus(-n, unit)`.
- `withYear(y)`: same year -> `this`; else check year, `resolvePreviousValid(y, month, day)`. `withMonth(m)`: same ->
  `this`; check month; `resolvePreviousValid`. `withDayOfMonth(d)`: same -> `this`; else `of(year, month, d)`.
  `withDayOfYear(d)`: `getDayOfYear() == d` -> `this`; else `ofYearDay(year, d)`.

### Comparison, hash, text

- `compareTo` (cast `other` to `LocalDate`) returns the raw difference: `year - other.year`, then `month - other.month`, then
  `day - other.day` (`2020-01-01` vs `2023-05-01` is `-3`; `2020-01-09` vs `2020-01-01` is `8`).
  `isAfter/isBefore/isEqual` are `compareTo > 0 / < 0 / == 0`. `equals`: a `LocalDate` with `compareTo == 0`.
- `hashCode()`: `(year & 0xFFFFF800) ^ ((year << 11) + (month << 6) + day)`.
- `toString()`: if `Math.abs(year) < 1000`: negative years append `year - 10000` and delete the char at index 1
  (`-1` -> `-0001`), others append `year + 10000` and delete the char at index 0 (`999` -> `0999`); else a `+` when
  `year > 9999` (`+10000`, `+999999999`) then the year (`-1000`, `-999999999`). Then `-MM-dd` with two digits each.

## Steps

1. Read the test, then `java/time/Period.java` and `java/time/Duration.java` (style, `Math` exact helpers),
   `java/time/temporal/ChronoUnit.java`, `UnsupportedTemporalTypeException.java`, and T80's `Month.java` and
   `DayOfWeek.java`, all under `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `java/time/LocalDate.java` there with the members above.
   Create `java/time/chrono/ChronoLocalDate.java` as shown above.
3. Register both (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/time/chrono/ChronoLocalDate.java` and
   `javaw-agent-kit/bin/kit register java/time/LocalDate.java`
4. `javaw-agent-kit/bin/kit check T81` until PASS.
5. Write `docs/bytecode-local-date.md` (below), the report, then `javaw-agent-kit/bin/kit finish T81`.

## Hints

- Write the private static helpers first: `checkYear(long)` returning `int`, `checkMonth(long)`, `isLeap(long)`,
  `monthLength(int month, boolean leap)`, `firstDayOfYear(int month, boolean leap)`, `create(y, m, d)` (the
  `Invalid date` checks) and `resolvePreviousValid(y, m, d)`. Everything else is a few lines on top.
- Copy the two epoch-day algorithms literally. The negative-year branches (`y / -4 ...`, the `adjust` cycles) are
  what make `MIN`, `-0001-...` and `-719529` (`-0001-12-31`) right; "simpler" formulas break them.
- `MIN`, `MAX` and `EPOCH` must use the private constructor (not `of`), and `month`/`day` are `short` fields, so
  `compareTo` and `hashCode` work on promoted ints.
- The `Invalid date` month name is the enum constant name (`FEBRUARY`), except the fixed `'February 29'` text.
- `withDayOfMonth`/`withDayOfYear` reuse `of`/`ofYearDay` so they produce the same messages.
- `Math.floorDiv(long, long)` / `Math.floorMod(long, long)` exist; write `7L`/`12L` so the long overloads are chosen.
- Calibration pitfalls: (1) without the marker `ChronoLocalDate` the fixture cannot link at all, because javac emits
  `LocalDate.isBefore(Ljava/time/chrono/ChronoLocalDate;)Z` and `compareTo(Ljava/time/chrono/ChronoLocalDate;)I`
  (and `Arrays.sort` calls `Comparable.compareTo(Object)`); declaring them with a `LocalDate` parameter does not
  help. (2) A plain `ofEpochDay(addExact(...))` `plusDays` gives `MAX.plusDays(1)` the EpochDay message; the JDK's
  small-step fast path gives the Year message.
- The Wasm runner stops at the first failing fixture line of each `@Test` method; after a fix another input may
  fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
factoriesAndEpochDay, fieldAccess, arithmetic, withersAndComparison: if FIX THE RUNTIME names `java/time/LocalDate`, `ChronoLocalDate` or `link`, check that LocalDate implements the marker java.time.chrono.ChronoLocalDate and that compareTo/isBefore/isAfter/isEqual take a ChronoLocalDate; otherwise a member is missing or calls a class/method the runtime does not have (no ChronoField, IsoChronology, LocalDateTime); if it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
factories: range checks run Year, then MonthOfYear, then DayOfMonth (1..31), then the month length; day 29 in February gives `Invalid date 'February 29' as 'Y' is not a leap year`, other days `Invalid date 'APRIL 31'` with the upper-case enum name; of(int, Month, int) with null is a NullPointerException; toString pads |year| < 1000 to 4 digits and puts `+` before years > 9999.
yearDayAndEpochDay: copy the JDK toEpochDay/ofEpochDay algorithms exactly (negative years use `y / -4 - y / -100 + y / -400` and the 400-year cycle adjust); ofYearDay checks Year, DayOfYear 1..366, then `Invalid date 'DayOfYear 366' as 'Y' is not a leap year`; out-of-range epoch days give the EpochDay message.
fields: getDayOfWeek is floorMod(toEpochDay() + 3, 7) + 1; getDayOfYear adds 1 from March on in leap years; leap years use `(y & 3) == 0 && (y % 100 != 0 || y % 400 == 0)` also for negative years.
plusMinus: plusMonths/plusYears clamp the day to the month length (resolvePreviousValid); a zero amount returns `this`; plus(n, unit) supports DAYS..MILLENNIA only and throws UnsupportedTemporalTypeException `Unsupported unit: <unit>` otherwise.
outOfRange: plusDays has the JDK fast path for day + n in 1..59 (crossing the year end checks the Year: `MAX.plusDays(1)` is the Year message), otherwise Math.addExact then ofEpochDay (EpochDay message); plusWeeks and DECADES..MILLENNIA use Math.multiplyExact (ArithmeticException); plusMonths/plusYears use plain long arithmetic then the Year check with the long value in the message; minusX(Long.MIN_VALUE) is plusX(Long.MAX_VALUE).plusX(1).
withers: same value returns `this`; withYear/withMonth clamp the day; withDayOfMonth uses of(...), withDayOfYear uses ofYearDay(...), so their messages are those factories'.
comparison: compareTo(ChronoLocalDate) casts to LocalDate and returns the raw year, then month, then day difference (not -1/0/1); hashCode is `(year & 0xFFFFF800) ^ ((year << 11) + (month << 6) + day)`; equals is false for null and Strings.
leavesOutTemporalMembers: this fails when now, parse, from, atStartOfDay, until or format became linked, or ChronoLocalDate got methods (isBefore, toEpochDay). Do not add those members or types.
```

## Docs

Create `docs/bytecode-local-date.md` with this skeleton (fill in the brackets):

    # Bytecode LocalDate

    Revision `local-date-vN` links `java.time.LocalDate` and a marker `java.time.chrono.ChronoLocalDate` in the
    bundled exception runtime.

    Contract:

    - <factories and validation messages; epoch-day algorithms; getters; month-end clamping in plus/with;
      supported ChronoUnits; raw-difference compareTo; JDK hashCode; ISO toString forms>

    Known differences: no `Temporal`, `TemporalAccessor` or `TemporalAdjuster` interfaces; `ChronoLocalDate` has
    no methods of its own (calls through the interface stay unlinked); `plus/minus(long, ERAS)` are unsupported;
    no `parse`, `format`, `now`, `atStartOfDay` or `until`.

    Evidence: `BytecodeLocalDateTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `now`, `parse`, `from`, `atStartOfDay`, `until`, `format` and the
    `ChronoLocalDate.isBefore`/`toEpochDay` interface calls are rejected before WAT generation.
