# T80 java.time.Month and java.time.DayOfWeek

```kit
id: T80
slug: month-day-of-week
title: link the java.time.Month and java.time.DayOfWeek enums (no TemporalAccessor/TemporalAdjuster)
tests: BytecodeMonthDayOfWeekTest
depends: T71
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/Month.java
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/DayOfWeek.java
allow: docs/bytecode-month-day-of-week.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-month-day-of-week-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeMonthDayOfWeekTest.java`.

## Goal

Add the `java.time.Month` and `java.time.DayOfWeek` enums to the bundled runtime. They are a prerequisite for a
later `LocalDate`. `runtime-target/target/audit/member-references.tsv` lists direct rows in 2 artifacts:
`DayOfWeek.getValue()` (H2 `DateTimeFunction.datediff`, 6 sites); the `MONDAY`, `FRIDAY`, `SATURDAY`, `SUNDAY` constants
(Spring `QuartzCronField.isWeekday` and `QuartzCronField.lambda$weekdayNearestTo$4`); `DayOfWeek.of(int)` (Spring
`QuartzCronField.parseDayOfWeek`); `Month.valueOf(String)` (Spring `MonthFormatter.parse`) and `Month.toString()`
(Spring `MonthFormatter.print`). `DayOfWeek.from(TemporalAccessor)` (`QuartzCronField.lambda$weekdayNearestTo$4`)
is referenced too but needs `TemporalAccessor`, which is not in the runtime; it stays out. `Month.of`, `plus`, `minus`,
`length`, `minLength`, `maxLength`, `firstDayOfYear` and `firstMonthOfQuarter` are what `LocalDate` will need.

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/time/Month.java` (`package java.time;`):
`public enum Month` (no `implements`) with the constants `JANUARY, FEBRUARY, MARCH, APRIL, MAY, JUNE, JULY, AUGUST,
SEPTEMBER, OCTOBER, NOVEMBER, DECEMBER` in that order, and

- `private static final Month[] ENUMS = Month.values();`
- `public static Month of(int month)`
- `public int getValue()`
- `public Month plus(long months)`, `public Month minus(long months)`
- `public int length(boolean leapYear)`, `public int minLength()`, `public int maxLength()`
- `public int firstDayOfYear(boolean leapYear)`, `public Month firstMonthOfQuarter()`

`javaw-bytecode/src/main/resources/exception-runtime/java/time/DayOfWeek.java` (`package java.time;`):
`public enum DayOfWeek` (no `implements`) with `MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY` in
that order, and

- `private static final DayOfWeek[] ENUMS = DayOfWeek.values();`
- `public static DayOfWeek of(int dayOfWeek)`
- `public int getValue()`
- `public DayOfWeek plus(long days)`, `public DayOfWeek minus(long days)`

`values()`, `valueOf(String)`, `name()`, `ordinal()`, `toString()`, `compareTo`, `getDeclaringClass()` come from
`enum` and `java.lang.Enum` (T71 made `valueOf` work for runtime enums).

## Members to leave out

- Everything naming types that are not in the runtime: `from(TemporalAccessor)`, `getDisplayName(TextStyle, Locale)`,
  `isSupported(TemporalField)`, `range(TemporalField)`, `get(TemporalField)`, `getLong(TemporalField)`,
  `query(TemporalQuery)`, `adjustInto(Temporal)`. Do **not** add `implements TemporalAccessor, TemporalAdjuster`
  and do not create any `java/time/temporal` or `java/time/format` file. The test checks that `Month.from`,
  `DayOfWeek.from`, both `getDisplayName`, `Month.range`, `DayOfWeek.getLong`, `Month.adjustInto` and
  `DayOfWeek.query` stay unlinked.
- Do not edit any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

The test prints names, ordinals, values and `DateTimeException` class name **and message**, and compares with the JVM.

- `getValue()`: `ordinal() + 1` (`JANUARY` 1 ... `DECEMBER` 12, `MONDAY` 1 ... `SUNDAY` 7).
- `Month.of(m)`: if `m < 1 || m > 12` throw `new DateTimeException("Invalid value for MonthOfYear: " + m)`, else
  `ENUMS[m - 1]`. `DayOfWeek.of(d)`: if `d < 1 || d > 7` throw `new DateTimeException("Invalid value for DayOfWeek: "
  + d)`, else `ENUMS[d - 1]`. (Exact JDK 21 messages, e.g. `Invalid value for MonthOfYear: 13`, `Invalid value for
  DayOfWeek: 0`, `Invalid value for DayOfWeek: -2147483648`.)
- `Month.plus(long months)`: `int amount = (int) (months % 12); return ENUMS[(ordinal() + (amount + 12)) % 12];`
  `Month.minus(long months)`: `plus(-(months % 12))` (take the remainder **before** negating, so `Long.MIN_VALUE`
  works: `JANUARY.minus(Long.MIN_VALUE)` is `SEPTEMBER`). DayOfWeek is the same with 7.
- `length(leap)`: `FEBRUARY` -> `leap ? 29 : 28`; `APRIL`, `JUNE`, `SEPTEMBER`, `NOVEMBER` -> 30; others 31.
  `minLength()`: `FEBRUARY` 28, the four 30-day months 30, others 31. `maxLength()`: `FEBRUARY` 29, else as `minLength`.
- `firstDayOfYear(leap)`: with `int l = leap ? 1 : 0`: JANUARY 1, FEBRUARY 32, MARCH 60+l, APRIL 91+l, MAY 121+l,
  JUNE 152+l, JULY 182+l, AUGUST 213+l, SEPTEMBER 244+l, OCTOBER 274+l, NOVEMBER 305+l, DECEMBER 335+l.
- `firstMonthOfQuarter()`: `ENUMS[(ordinal() / 3) * 3]` (`MAY` -> `APRIL`, `DECEMBER` -> `OCTOBER`).
- `valueOf("December")` throws `IllegalArgumentException` (compared by class).

## Steps

1. Read the test, then `java/time/temporal/ChronoUnit.java` (a runtime enum) and `java/time/DateTimeException.java`,
   under `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `java/time/Month.java` and `java/time/DayOfWeek.java` there with the members above.
3. Register both (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/time/Month.java` and `javaw-agent-kit/bin/kit register java/time/DayOfWeek.java`
4. `javaw-agent-kit/bin/kit check T80` until PASS.
5. Write `docs/bytecode-month-day-of-week.md` (below), the report, then `javaw-agent-kit/bin/kit finish T80`.

## Hints

- No imports are needed (`DateTimeException` is in the same package).
- Do **not** use a `switch` on the enum inside `Month.java` (javac would generate a synthetic `Month$1` switch-map
  class that is not registered). Use `if`/`==` comparisons or arithmetic on `ordinal()`.
- `ENUMS` must be declared after the constants (enum constants are always first, so a plain field is fine).
- The `% 12` and `% 7` of a negative long are negative in Java; `amount + 12` keeps the index non-negative.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.
- Calibration pitfalls: `EnumSet`/`EnumMap` of a runtime enum fail in Wasm (`Class.getEnumConstants` needs
  declared-method metadata that runtime classes lack), so the test does not use them; do not try to fix that here.
  A `switch` on `Month`/`DayOfWeek` in application code works. `firstDayOfYear` adds the leap day only from `MARCH` on.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
constantsAndOf, arithmetic, enumSwitch, leavesOutTemporalMembers: if FIX THE RUNTIME says `link the symbolic class/interface java/time/Month` or `java/time/DayOfWeek` or names one of their methods, a class is missing or not registered: run `javaw-agent-kit/bin/kit register java/time/Month.java` and `javaw-agent-kit/bin/kit register java/time/DayOfWeek.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
constants: constants must be in JDK order (JANUARY..DECEMBER, MONDAY..SUNDAY); getValue() is ordinal() + 1; the enums implement no interface besides what Enum gives (Comparable, Serializable).
ofAndValueOf: of() checks the range 1..12 / 1..7 and throws DateTimeException "Invalid value for MonthOfYear: " + m / "Invalid value for DayOfWeek: " + d; otherwise returns ENUMS[value - 1].
plusMinus: plus is ENUMS[(ordinal() + (int) (amount % N) + N) % N]; minus is plus(-(amount % N)) so Long.MIN_VALUE does not overflow.
monthLengths: FEBRUARY is 28/29; APRIL, JUNE, SEPTEMBER, NOVEMBER are 30; firstDayOfYear adds 1 for leap years from MARCH on; firstMonthOfQuarter is ENUMS[ordinal() / 3 * 3].
collections: a switch on Month/DayOfWeek in the fixture uses values() and ordinal(); constants must be the same objects returned by of() and valueOf().
leavesOutTemporalMembers: this fails when from, getDisplayName, range, getLong, adjustInto or query became linked. Do not add TemporalAccessor/TemporalAdjuster or these methods.
```

## Docs

Create `docs/bytecode-month-day-of-week.md` with this skeleton (fill in the brackets):

    # Bytecode Month and DayOfWeek

    Revision `month-day-of-week-vN` links the `java.time.Month` and `java.time.DayOfWeek` enums in the bundled
    exception runtime.

    Contract:

    - <constants in JDK order; of(int) with the JDK DateTimeException messages; getValue; plus/minus with
      wrap-around; Month length/minLength/maxLength/firstDayOfYear/firstMonthOfQuarter>

    Known differences: no `TemporalAccessor`/`TemporalAdjuster` interfaces and none of their members (`from`,
    `get`, `getLong`, `range`, `isSupported`, `query`, `adjustInto`); no `getDisplayName` (no TextStyle/locale data).

    Evidence: `BytecodeMonthDayOfWeekTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `from`, `getDisplayName`, `range`, `getLong`, `adjustInto` and `query` are rejected before
    WAT generation.
