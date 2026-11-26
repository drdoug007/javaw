# Bytecode LocalDate

Revision `local-date-v344` links `java.time.LocalDate` and a marker `java.time.chrono.ChronoLocalDate` in the
bundled exception runtime.

Contract:

- `LocalDate` is a final class holding `int year`, `short month`, `short day`; it implements the marker
  `ChronoLocalDate` (which extends `Comparable<ChronoLocalDate>` and declares no members of its own) and
  `java.io.Serializable`. Constants: `MIN_YEAR` -999999999, `MAX_YEAR` 999999999, `MIN` -999999999-01-01,
  `MAX` +999999999-12-31, `EPOCH` 1970-01-01.
- Factories `of(int,int,int)` (also via `of(int,Month,int)`, which NPEs on a null month with message
  "month"), `ofYearDay(int,int)` and `ofEpochDay(long)` throw the exact JDK `DateTimeException` messages:
  year `Invalid value for Year (valid values -999999999 - 999999999): v`, month
  `Invalid value for MonthOfYear (valid values 1 - 12): v`, day
  `Invalid value for DayOfMonth (valid values 1 - 28/31): v`, day-of-year
  `Invalid value for DayOfYear (valid values 1 - 365/366): v`, epoch-day
  `Invalid value for EpochDay (valid values -365243219162 - 365241780471): v`, impossible dates
  `Invalid date '<MONTH> <day>'` (upper-case enum name, e.g. `FEBRUARY 30`) and
  `Invalid date 'February 29' as '<year>' is not a leap year` /
  `Invalid date 'DayOfYear 366' as '<year>' is not a leap year`. Leap years use
  `(y & 3) == 0 && (y % 100 != 0 || y % 400 == 0)`, which is correct for negative years (-4 and 0 are leap,
  -100 and -1 are not).
- Getters: `getYear`, `getMonthValue`, `getDayOfMonth`, `getMonth` (the T80 `Month` enum), `getDayOfWeek`
  (`DayOfWeek.of(floorMod(toEpochDay() + 3, 7) + 1)`, 1970-01-01 being Thursday), `getDayOfYear` (first day of
  the month in that year plus the day, 1-based; the first day of year adds 1 from March on in leap years),
  `isLeapYear`, `lengthOfMonth`, `lengthOfYear`.
- `toEpochDay()` and `ofEpochDay(long)` use the JDK 400-year-cycle algorithms in long arithmetic
  (`DAYS_PER_CYCLE` 146097, `DAYS_0000_TO_1970` 719528, cycle adjustment only for pre-1 AD zero days);
  `MIN.toEpochDay()` is -365243219162 and `MAX.toEpochDay()` 365241780471.
- Arithmetic: `plusYears/plusMonths/plusWeeks/plusDays` and the four minus forms. A zero amount returns
  `this`; `Long.MIN_VALUE` subtractions use the `plus(MAX) + 1` idiom; month-end days clamp to the target
  month length (`2024-01-31.plusMonths(1)` is `2024-02-29`, `2023-03-31.minusMonths(1)` is `2023-02-28`);
  `plusWeeks` is `plusDays(multiplyExact(weeks, 7))`; `plusDays` uses the JDK fast path (day 1..28 in the same
  month, 29..59 across one month boundary, otherwise `ofEpochDay(addExact(toEpochDay(), n))`), so
  `MAX.plusDays(1)` reports the Year overflow (year 1000000000) rather than an EpochDay overflow, and
  `MIN.minusDays(1)` reports the EpochDay overflow. Overflowing multiplies/sums throw `ArithmeticException`.
- `plus(long, TemporalUnit)`/`minus(long, TemporalUnit)` support `DAYS`, `WEEKS`, `MONTHS`, `YEARS`,
  `DECADES`, `CENTURIES`, `MILLENNIA` (the last three via `Math.multiplyExact(n, 10/100/1000)`); every other
  unit (`ERAS`, `FOREVER`, `HALF_DAYS`, `HOURS`..`NANOS`, non-`ChronoUnit` units) throws
  `UnsupportedTemporalTypeException("Unsupported unit: " + unit)`.
- Withers `withYear/withMonth/withDayOfMonth/withDayOfYear` return `this` when unchanged, otherwise clamp the
  day the same way as arithmetic and throw the factory messages (`2023-02-29` from
  `of(2023,2,1).withDayOfMonth(29)` is rejected with the not-a-leap-year message).
- `compareTo(ChronoLocalDate)` casts to `LocalDate` and returns the raw year difference, else the month
  difference, else the day difference (e.g. `MIN.compareTo(MAX)` is -1999999998, not -1); `isAfter`,
  `isBefore`, `isEqual` are compare-to sign tests; `equals` is same-instance or another `LocalDate` with
  `compareTo == 0`; `hashCode()` is the JDK `(year & 0xFFFFF800) ^ ((year << 11) + (month << 6) + day)`.
- `toString()` is the JDK ISO form: years -999..999 zero-padded to four digits (`0999`, `-0001`, `0000`),
  larger positive years with an explicit plus (`+10000-02-29`, `+999999999-12-31`), out-of-range negatives
  as-is (`-999999999-01-01`), then two-digit month and day.

Known differences: no `Temporal`/`TemporalAccessor`/`TemporalAdjuster` interfaces — `LocalDate` implements
none of them and `ChronoLocalDate` has no methods of its own, so calls through the interface
(`ChronoLocalDate.isBefore`, `ChronoLocalDate.toEpochDay`) stay unlinked; `plus/minus(long, ERAS)` and all
time-based units are unsupported (they throw); no `parse`, `format`, `now`, `atStartOfDay`, `until`,
`datesUntil`, `with(TemporalAdjuster/TemporalField)`, `get`/`getLong`, `range`, `query`, `adjustInto`,
`isSupported`, `getChronology`, `getEra` or serialization hooks.

Evidence: `BytecodeLocalDateTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `now`, `parse`, `from`, `atStartOfDay`, `until`, `format` and the
`ChronoLocalDate.isBefore`/`toEpochDay` interface calls are rejected before WAT generation.
