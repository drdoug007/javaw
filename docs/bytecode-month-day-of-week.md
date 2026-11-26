# Bytecode Month and DayOfWeek

Revision `month-day-of-week-v343` links the `java.time.Month` and `java.time.DayOfWeek` enums in the bundled
exception runtime.

Contract:

- `Month` is an enum with the 12 constants in JDK order (`JANUARY`..`DECEMBER`); `DayOfWeek` is an enum with
  the 7 constants in JDK order (`MONDAY`..`SUNDAY`). Each has a private static `ENUMS` array built from
  `values()`. Neither implements any interface beyond those `Enum` provides (`Comparable`, `Serializable`).
- `Month.of(int)` throws `DateTimeException("Invalid value for MonthOfYear: " + m)` when `m < 1 || m > 12`
  and returns `ENUMS[m - 1]` otherwise. `DayOfWeek.of(int)` throws
  `DateTimeException("Invalid value for DayOfWeek: " + d)` when `d < 1 || d > 7` and returns `ENUMS[d - 1]`
  otherwise. `valueOf(String)` is inherited from `Enum` (wrong-case names throw `IllegalArgumentException`).
- `getValue()` is `ordinal() + 1` (`JANUARY` 1 .. `DECEMBER` 12, `MONDAY` 1 .. `SUNDAY` 7).
- `Month.plus(long)` is `ENUMS[(ordinal() + (amount + 12)) % 12]` with `amount = (int) (months % 12)`;
  `Month.minus(long)` is `plus(-(months % 12))` (the remainder is taken before negating, so
  `Long.MIN_VALUE` works: `JANUARY.minus(Long.MIN_VALUE)` is `SEPTEMBER`). `DayOfWeek.plus`/`minus` are the
  same with 7.
- `Month.length(boolean leapYear)`: `FEBRUARY` is 29 for leap years, 28 otherwise; `APRIL`, `JUNE`,
  `SEPTEMBER` and `NOVEMBER` are 30; the rest are 31. `minLength()`: `FEBRUARY` 28, the four 30-day months
  30, others 31. `maxLength()`: `FEBRUARY` 29, otherwise as `minLength()`.
- `Month.firstDayOfYear(boolean leapYear)`: `JANUARY` 1, `FEBRUARY` 32, and from `MARCH` on the non-leap
  day (`60`, `91`, `121`, `152`, `182`, `213`, `244`, `274`, `305`, `335`) plus 1 in leap years.
  `firstMonthOfQuarter()` is `ENUMS[(ordinal() / 3) * 3]` (`MAY` -> `APRIL`, `DECEMBER` -> `OCTOBER`).

Known differences: no `TemporalAccessor`/`TemporalAdjuster` interfaces and none of their members (`from`,
`get`, `getLong`, `range`, `isSupported`, `query`, `adjustInto`); no `getDisplayName` (no TextStyle/locale data).

Evidence: `BytecodeMonthDayOfWeekTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `from`, `getDisplayName`, `range`, `getLong`, `adjustInto` and `query` are rejected before
WAT generation.
