# T69 java.time.Period core subset

```kit
id: T69
slug: period
title: link java.time.Period (core subset without TemporalAmount)
tests: BytecodePeriodTest
depends: T56
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/Period.java
allow: docs/bytecode-period.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-period-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodePeriodTest.java`.

## Goal

Add `java.time.Period` (years/months/days amount) to the bundled runtime, without the `TemporalAmount`/`ChronoPeriod`
interfaces. `runtime-target/target/audit/member-references.tsv` lists 20 Period rows in 5 artifacts: `parse(CharSequence)`
(Jackson jsr310 `JSR310StringParsableDeserializer._fromString`, Spring Boot `PeriodStyle$2.parse`, Spring Data
`Jsr310Converters$StringToPeriodConverter.convert`, Spring `PeriodFormatter.parse`); `toString()` (Spring Boot
`PeriodStyle$2.print`, Spring Data `Jsr310Converters$PeriodToStringConverter`, Spring `PeriodFormatter.print`);
`of(III)` (H2 `JSR310Utils.valueToPeriod`, Spring Boot `PeriodStyle$1.parse`); `getYears()`, `getMonths()`, `getDays()`
(H2 `JSR310Utils.periodToValue`, and as method handles, opcode -5, in Spring Boot `PeriodStyle$Unit.<clinit>`);
`ofDays(I)`, `ofWeeks(I)`, `ofMonths(I)`, `ofYears(I)` (method handles, opcode -6, in `PeriodStyle$Unit.<clinit>`) and
`isZero()` (Spring Boot `PeriodStyle$1.print`). The with/plus/minus/multipliedBy/negated/normalized/toTotalMonths/
equals/hashCode members make the value type coherent. `between(LocalDate, LocalDate)`, `from(TemporalAmount)`,
`get(TemporalUnit)`, `getUnits()`, `getChronology()`, `addTo/subtractFrom(Temporal)` and `plus/minus(TemporalAmount)`
are not referenced and need types (`LocalDate`, `TemporalAmount`, `Temporal`, `IsoChronology`) that are not in the runtime.

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/time/Period.java` (`package java.time;`):
`public final class Period implements java.io.Serializable` (**not** `Comparable`) with fields
`private final int years; private final int months; private final int days;` and

- `public static final Period ZERO`
- `private Period(int years, int months, int days)`, `private static Period create(int years, int months, int days)`
- `public static Period of(int years, int months, int days)`, `ofYears(int)`, `ofMonths(int)`, `ofWeeks(int)`, `ofDays(int)`
- `public static Period parse(CharSequence text)`
- `public int getYears()`, `getMonths()`, `getDays()`
- `public boolean isZero()`, `public boolean isNegative()`
- `public Period withYears(int)`, `withMonths(int)`, `withDays(int)`
- `public Period plusYears(long)`, `plusMonths(long)`, `plusDays(long)`, `minusYears(long)`, `minusMonths(long)`, `minusDays(long)`
- `public Period multipliedBy(int scalar)`, `public Period negated()`, `public Period normalized()`
- `public long toTotalMonths()`
- `public boolean equals(Object)`, `public int hashCode()`, `public String toString()`

## Members to leave out

- Everything naming types that are not in the runtime: `between(LocalDate, LocalDate)`, `from(TemporalAmount)`,
  `get(TemporalUnit)`, `getUnits()` (it returns `List<TemporalUnit>` of ChronoUnits; leave it out anyway),
  `getChronology()`, `addTo(Temporal)`, `subtractFrom(Temporal)`, `plus(TemporalAmount)`, `minus(TemporalAmount)`.
  Do **not** add `implements TemporalAmount`/`ChronoPeriod` and do not create any other `java/time` file. The test
  checks that `between`, `from`, `get`, `getUnits`, `plus(TemporalAmount)` and `addTo` stay unlinked.
- Serialization (`writeReplace`, `readObject`, the `Ser` class). Keep `implements java.io.Serializable`.
- `java.util.regex` is not in the runtime: `parse` is written by hand.
- Do not edit any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

The test prints `toString()`, the three getters and exception class names, and compares with the JVM.
`ArithmeticException` messages are **not** compared (only the class); `DateTimeParseException` message, parsed
string, error index and cause class **are** compared.

- `create(y, m, d)` returns the **same `ZERO` object** when `(y | m | d) == 0`, else `new Period(y, m, d)`. Every
  factory and every method that builds a new value goes through `create` (the test checks `Period.ofWeeks(0) ==
  Period.ZERO`, `p.minusYears(1).minusMonths(2).minusDays(3) == Period.ZERO`, `Period.parse("P1W-7D") == Period.ZERO`).
  `ZERO` itself is `new Period(0, 0, 0)` (not `create`, which would read the still-null `ZERO`).
- `of`, `ofYears`, `ofMonths`, `ofDays` store the values as given (no normalisation: `Period.of(0, -14, 0)` stays
  `P-14M`). `ofWeeks(w)` = `create(0, 0, Math.multiplyExact(w, 7))` (int overflow -> `ArithmeticException`).
- `isZero()`: `this == ZERO`. `isNegative()`: any of the three fields `< 0` (`P1Y-1M` is negative).
- `withYears(y)`: `y == years ? this : create(y, months, days)`; same for months/days.
- `plusYears(v)`: `v == 0 ? this : create(Math.toIntExact(Math.addExact(years, v)), months, days)` (a long `v`
  whose int sum fits works: `Period.ofMonths(-1).plusMonths(Integer.MAX_VALUE + 1L)` is `P2147483647M`); same for
  months/days. `minusYears(v)`: `v == Long.MIN_VALUE ? plusYears(Long.MAX_VALUE).plusYears(1) : plusYears(-v)` (it
  always overflows for `Long.MIN_VALUE`, with `ArithmeticException`); same for months/days.
- `multipliedBy(s)`: `this == ZERO || s == 1` returns `this`; otherwise `create(Math.multiplyExact(years, s),
  Math.multiplyExact(months, s), Math.multiplyExact(days, s))` (so `multipliedBy(0)` gives `ZERO`). `negated()` =
  `multipliedBy(-1)` (`Period.ofDays(Integer.MIN_VALUE).negated()` throws `ArithmeticException`).
- `toTotalMonths()`: `years * 12L + months` (long, never throws).
- `normalized()`: `long total = toTotalMonths(); long y = total / 12; int m = (int) (total % 12);` (Java division,
  so both get the sign of the total: `P1Y-25M` -> `P-1Y-1M`, `P-1Y13M` -> `P1M`); if `y == years && m == months`
  return `this`, else `create(Math.toIntExact(y), m, days)`. Days are never touched. `Period.of(Integer.MAX_VALUE,
  12, 0).normalized()` throws `ArithmeticException`.
- `equals`: same object, or a `Period` with all three fields equal (`ofDays(14)` equals `ofWeeks(2)`, both are `P14D`;
  `ofMonths(12)` does not equal `ofYears(1)`). `false` for `null` and a `String`.
  `hashCode()`: `years + Integer.rotateLeft(months, 8) + Integer.rotateLeft(days, 16)`.
- `toString()`: `ZERO` prints `P0D`; otherwise `"P"` then `years + "Y"` if non-zero, `months + "M"` if non-zero,
  `days + "D"` if non-zero (`P1Y2M3D`, `P-14M`, `P1Y-2M3D`). Weeks never appear.
- `parse(CharSequence text)`: `Objects.requireNonNull(text, "text")` (NPE message `text`). The JDK matches the whole
  text against this case-insensitive regex:

      ([-+]?)P(?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?
        1          2 years           3 months          4 weeks           5 days

  Hand parser: optional `+`/`-`, then `P`/`p`, then zero or more groups, each a number (an optional `+`/`-` and
  **at least one** ASCII digit; a lone sign is not a number, so `P-Y` is rejected) followed by `Y`, `M`, `W` or `D`
  in either case, each letter at most once and in that order (`P1D2Y`, `P1Y1Y`, `P1D1W` rejected), then end of text.
  Reject when it does not match or when no group is present (`P`, `-P`). Otherwise (`negate` is -1 for a leading `-`):

      try {
          int years = parseNumber(text, yearStart, yearEnd, negate);
          int months = parseNumber(text, monthStart, monthEnd, negate);
          int weeks = parseNumber(text, weekStart, weekEnd, negate);
          int days = parseNumber(text, dayStart, dayEnd, negate);
          days = Math.addExact(days, Math.multiplyExact(weeks, 7));
          return create(years, months, days);
      } catch (NumberFormatException ex) {
          throw new DateTimeParseException("Text cannot be parsed to a Period", text, 0, ex);
      }

  `parseNumber` returns 0 for an absent group, else `int v = Integer.parseInt(text, start, end, 10);` then
  `Math.multiplyExact(v, negate)`, turning an `ArithmeticException` from that multiply into `new
  DateTimeParseException("Text cannot be parsed to a Period", text, 0, ex)`. Note the JDK does **not** catch the
  `ArithmeticException` of the weeks/days `addExact`/`multiplyExact` line: `P306783379W` and `P1W2147483641D` throw a
  plain `ArithmeticException`. Rejection: `new DateTimeParseException("Text cannot be parsed to a Period", text, 0)`
  (no cause). The message has no suffix (unlike Duration's `: days`).

## Steps

1. Read the test, then `java/time/Duration.java` (style and its hand-written `parse`),
   `java/time/format/DateTimeParseException.java`, and the `addExact`, `multiplyExact`, `toIntExact` lines of
   `java/lang/Math.java`, all under `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `java/time/Period.java` under `javaw-bytecode/src/main/resources/exception-runtime/` with the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/time/Period.java`
4. `javaw-agent-kit/bin/kit check T69` until PASS.
5. Write `docs/bytecode-period.md` (below), the report, then `javaw-agent-kit/bin/kit finish T69`.

## Hints

- Imports in `Period.java`: `java.time.format.DateTimeParseException`, `java.util.Objects`. No other imports needed.
- `Integer.parseInt(CharSequence, int, int, int)` exists in the runtime and accepts a leading `+` or `-`;
  `Math.multiplyExact(int,int)`, `Math.addExact(int,int)`, `Math.addExact(long,long)`, `Math.toIntExact(long)` and
  `Integer.rotateLeft` exist. `Math.addExact(years, v)` with an int and a long picks the long overload.
- A simple parser: after `P`, loop while `i < length`: remember `start`, skip one optional sign, require at least one
  digit, require a letter, map it to group 0..3 (`Y`,`M`,`W`,`D`, else reject) and reject when the group is not
  greater than the previous one. Store start (including the sign) and end (before the letter) per group.
- Pass the start index **including** the group's own sign to `Integer.parseInt` (`P-1Y+2M` is `P-1Y2M`); the leading
  sign before `P` is applied by `multiplyExact(v, negate)` (`-P-1Y` is `P1Y`, `-P-2147483648Y` overflows with an
  `ArithmeticException` cause).
- Calibration pitfalls: `"P2147483648Y"` fails in `Integer.parseInt` (cause `NumberFormatException`) but
  `"P306783379W"` throws a bare `ArithmeticException` (not wrapped); `Period.of(1, 2, 3).withYears(1)` and
  `plusYears(0)` must return `this`; `multipliedBy` on `ZERO` returns `ZERO` even for a scalar that would overflow.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
factoriesAndAccessors, arithmetic, equalityAndHash, parsing, leavesOutTemporalMembers: if FIX THE RUNTIME says `link the symbolic class/interface java/time/Period` or names one of its methods, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/time/Period.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
factories: create() must return the ZERO object when (years | months | days) == 0; ofWeeks uses Math.multiplyExact(weeks, 7); of() stores the values unnormalised.
accessors: isZero() is this == ZERO; isNegative() is true when any field is negative; toTotalMonths() is years * 12L + months; toString prints P0D for ZERO, otherwise only non-zero Y, M, D parts.
withAndPlus: withX returns this when the value is unchanged; plusX(0) returns this; plusX uses Math.toIntExact(Math.addExact(field, long)); minusX(Long.MIN_VALUE) is plusX(Long.MAX_VALUE).plusX(1).
multiplyNegateNormalize: multipliedBy returns this for ZERO or scalar 1 and otherwise multiplies each field with Math.multiplyExact(int,int); normalized uses total / 12 and total % 12 (Java division, sign of the total), returns this when unchanged, and Math.toIntExact on the years.
equality: equals compares the three fields and rejects null and non-Period objects; hashCode is years + Integer.rotateLeft(months, 8) + Integer.rotateLeft(days, 16); Period is Serializable but not Comparable.
parseValid: letters are case-insensitive, groups come in the order Y, M, W, D, each number keeps its own sign, a leading '-' multiplies every number by -1, weeks are added to days as weeks * 7, and the result goes through create() (ZERO for all zeros).
parseInvalid: a rejection is DateTimeParseException "Text cannot be parsed to a Period" with errorIndex 0 and no cause; Integer.parseInt failures add a NumberFormatException cause, the negate multiplyExact adds an ArithmeticException cause, and the weeks*7 / days addExact overflow is a bare ArithmeticException; parse(null) throws NullPointerException "text".
leavesOutTemporalMembers: this fails when between, from, get, getUnits, plus(TemporalAmount) or addTo became linked. Do not add TemporalAmount or these methods.
```

## Docs

Create `docs/bytecode-period.md` with this skeleton (fill in the brackets):

    # Bytecode Period

    Revision `period-vN` links `java.time.Period` (core subset) in the bundled exception runtime.

    Contract:

    - <years/months/days value with a shared ZERO; of/ofYears/ofMonths/ofWeeks/ofDays; with/plus/minus with exact
      int arithmetic; multipliedBy/negated/normalized/toTotalMonths; equals/hashCode (JDK formula); ISO-8601
      toString; hand-written parse with the JDK's DateTimeParseException messages>

    Known differences: no `TemporalAmount`/`ChronoPeriod` interface and no LocalDate/Temporal members (`between`,
    `from`, `get`, `getUnits`, `addTo`, `plus(TemporalAmount)`, ...); no serialization proxy. ArithmeticException
    messages come from the runtime's `Math` and differ from the JDK's.

    Evidence: `BytecodePeriodTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `between`, `from`, `get`, `getUnits`, `plus(TemporalAmount)` and `addTo` are rejected before
    WAT generation.
