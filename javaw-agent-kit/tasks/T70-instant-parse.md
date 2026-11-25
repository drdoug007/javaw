# T70 Instant.parse for ISO_INSTANT text

```kit
id: T70
slug: instant-parse
title: link java.time.Instant.parse(CharSequence) (ISO_INSTANT text, hand-written parser)
tests: BytecodeInstantParseTest
depends: T61
allow: javaw-bytecode/src/main/resources/exception-runtime/java/time/Instant.java
allow: docs/bytecode-instant-parse.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-instant-parse-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeInstantParseTest.java`.

## Goal

Add `public static Instant parse(CharSequence text)` to the runtime `java.time.Instant` (T61). It is the last
`Instant` member the pinned libraries call that needs no `Temporal`/zone types:
`runtime-target/target/audit/member-references.tsv` has `java/time/Instant parse (Ljava/lang/CharSequence;)Ljava/time/Instant;`
with opcode 184 in `org.springframework:spring-context:6.1.5` (2 sites, `InstantFormatter.parse(String, Locale)`,
used by `@DateTimeFormat`/web data binding for `Instant` values) and `org.springframework.data:spring-data-commons:3.2.4`
(`Jsr310Converters$StringToInstantConverter.convert`). Spring's `TemporalAccessorParser.parse` also reads
`DateTimeParseException.getErrorIndex()` and calls the 4-argument constructor (both already in the runtime), so
the error index and message must be the JDK's. The JDK implements `parse` as
`DateTimeFormatter.ISO_INSTANT.parse(text, Instant::from)`; this runtime has no `DateTimeFormatter`,
`TemporalAccessor` or `LocalDateTime`, so the task writes a small dedicated parser inside `Instant` that
reproduces the JDK 17/21 `ISO_INSTANT` grammar, error indices and messages exactly.

## Members to implement

In `javaw-bytecode/src/main/resources/exception-runtime/java/time/Instant.java`:

- `public static Instant parse(CharSequence text)`

Private static helpers are up to you (suggested: `parseError(CharSequence text, int index)`,
`unparsedError(CharSequence text, int index)`, `resolveError(CharSequence text, String message, RuntimeException cause)`,
`abbreviate(CharSequence)`, `isLeap(long year)`, `daysFromCivil(long year, int month, int day)`). Add
`import java.time.format.DateTimeParseException;`. Do not change any existing member.

## Members to leave out

- `from(TemporalAccessor)`, `now()`, `atOffset(ZoneOffset)`, and any `DateTimeFormatter`, `DateTimeFormatterBuilder`,
  `TemporalAccessor`, `ZoneOffset`, `LocalDate(Time)` or `Parsed` type: do not create them. The test checks that
  `Instant.from`, `Instant.now`, `Instant.atOffset` and `DateTimeFormatter.parse(CharSequence)` stay unlinked.
- Do not edit `DateTimeParseException.java`, `DateTimeException.java` or `Duration.java`: they already have what
  you need (`new DateTimeParseException(String message, CharSequence parsed, int errorIndex)` and the
  4-argument constructor with a `Throwable cause`).
- Do not edit any test. (The T61 test's `rejectsUnusedCall` for `Instant.parse` is removed by the reviewer
  when this task is queued; if `kit check T70` reports it, stop and report it.)

## JDK behaviour to match

The test prints, for every input, either `epochSecond/nano=toString()` or, for a `DateTimeParseException`,
`DTPE@<errorIndex>:<message>:<cause class or ->:<parsedString equals input>:true`. Other exceptions print only
their class name. All indices below are 0-based character positions in `text`.

### Grammar (parse left to right, `pos` starts at 0)

`text == null` throws `NullPointerException` (use `Objects.requireNonNull(text, "text")` or just call
`text.length()`; only the class is compared). Let `len = text.length()`. A *digit* is only `'0'..'9'`
(`ch >= '0' && ch <= '9'`); never use `Character.isDigit` (full-width and Arabic digits must be rejected).
"fail at i" means `throw parseError(text, i)`.

1. **Year.** `start = pos`. If `text.charAt(pos)` (only when `pos < len`) is `'+'` or `'-'`, remember the sign
   and `pos++`. `digitsStart = pos`. Read up to **10** digits (stop at the first non-digit or end of text).
   Let `count` be the number read and `year` their value (a `long`).
   - `count < 4`: fail at `digitsStart` (`""` -> 0, `"+"` -> 1, `"-201-..."` -> 1, `"201-..."` -> 0).
   - sign `'-'` and `year == 0`: fail at `start` (`"-0000"` -> 0); otherwise `year = -year`.
   - sign `'+'` and `count <= 4`: fail at `start` (`"+2011"`, `"+0000"` -> 0). `"+01970"` is valid (year 1970).
   - no sign and `count > 4`: fail at `start` (`"12011"`, `"01970"`, `"1000000000"` -> 0).
   - 11 or more digits: only 10 are read, so the next step fails at the 11th digit (`"+10000000000-..."` -> 11).
2. **Literal `'-'`**: if `pos >= len` or `text.charAt(pos) != '-'` fail at `pos`; else `pos++`.
3. **Month**: exactly 2 digits. If fewer than 2 digits are available at `pos`, fail at `pos` (`"2011-1a"` -> 5).
   No range check here.
4. **Literal `'-'`**, then **day** (2 digits), the same way (`"2011-12-3T"` -> 8).
5. **Literal `'T'`**, compared **case-insensitively** (`'T'` or `'t'`); otherwise fail at `pos`
   (`"2011-12-03X..."` and `"2011-12-03 ..."` -> 10).
6. **Hour** (2 digits), literal `':'`, **minute** (2 digits), literal `':'`, **second** (2 digits). Seconds are
   **required**: `"2011-12-03T10:15Z"` fails at 16. `"T1:15"` fails at 11 (the hour start).
7. **Fraction** (optional): if `pos < len` and `text.charAt(pos) == '.'` (only `'.'`; a `','` is not a decimal
   point), `pos++`, then read **0 to 9** digits. Zero digits is fine (`"10:15:30.Z"` is valid, nano 0). The nano
   value is the digits padded on the right to 9 (`.5` = 500000000, `.000000001` = 1). A 10th digit is not
   consumed: the offset step then fails on it (`".1234567891Z"` -> 29).
8. **Offset** (required), at `offStart = pos`:
   - `pos >= len`: fail at `pos` (`"2011-12-03T10:15:30"` -> 19).
   - `'Z'` or `'z'`: offset 0, `pos++`.
   - `'+'` or `'-'`: read hours `HH` = two digits right after the sign, then `':'`, then minutes `MM` = two digits.
     Each 2-digit group is accepted only if both chars are digits **and its value is <= 59**. If the hour group or
     the `:MM` group is missing or invalid, fail at `offStart` (`"+01"`, `"+0130"`, `"+1:00"`, `"+01:3"`, `"+"`,
     `"+00:"`, `"+01:60"`, `"+60:00"` -> 19). Then an **optional** `:ss` group: consumed only when a `':'` and two
     digits with value <= 59 follow; otherwise nothing more is consumed (`"+00:00:"`, `"+01:30:1"`,
     `"+01:00:60"` leave the rest as unparsed text).
     After that, if `HH > 23` (24..59 passed the group check), throw the **resolve error** (below) with
     message `Value out of range: Hour[0-23], Minute[0-59], Second[0-59]` and a `DateTimeException` with that
     message as cause. `offsetSecs = sign * (HH * 3600 + MM * 60 + ss)`.
   - anything else: fail at `offStart` (`"10:15:30 Z"` -> 19, `"10:15:30,5Z"` -> 19).
### Values (steps 9-10 fail with `parseError(text, 0)`, index **0**)

The value checks run **before** the unparsed-text check of step 12: `"2011-13-03T10:15:30ZZ"` fails at index 0
(month 13), not with "unparsed text". Only the offset-hour error of step 8 comes earlier.

9. `days = 0`. If `hour == 24 && minute == 0 && second == 0 && nano == 0`: `hour = 0; days = 1` (next day).
    Else if `hour == 23 && minute == 59 && second == 60`: `second = 59` (leap second, keeps the nanos; any offset).
10. Check, with `y = year % 10_000` (Java `%`, sign of `year`; leap years are the same in every 10000-year cycle):
    `1 <= month <= 12`, `1 <= day <= lengthOfMonth(y, month)` (February has 29 days when
    `(y & 3) == 0 && (y % 100 != 0 || y % 400 == 0)`; this works for negative `y` too), `hour <= 23`,
    `minute <= 59`, `second <= 59`, and `-64800 <= offsetSecs <= 64800` (+-18:00). Any failure: fail at 0
    (`"2011-02-29"`, `"1900-02-29"`, `"-0001-02-29"`, `"-10001-02-29"`, month 00/13, day 00/32, `25:15`, `10:60`,
    `:61`, `22:59:60`, `24:00:01`, `24:00:00.000000001`, `+18:01`, `+19:00`, `+18:00:01` -> 0).
11. `epochSecond = (epochDay(year, month, day) + days) * 86400 + hour * 3600 + minute * 60 + second - offsetSecs`,
    computed with `long`s. `epochDay` is the proleptic Gregorian day count from 1970-01-01 for any year
    (`LocalDate.toEpochDay`, below). No overflow is possible for 10-digit years.
12. **End**: if `pos < len`, throw `new DateTimeParseException("Text '" + abbr + "' could not be parsed, unparsed text found at index " + pos, text, pos)`
    (`"...30ZX"` -> 20, `"...+01:00Z"` -> 25). This comes after the value checks above and before the range
    check below.
13. If `epochSecond < MIN_SECOND || epochSecond > MAX_SECOND`, throw the resolve error with message
    `"Unable to obtain Instant from TemporalAccessor: " + <anything>` and a `DateTimeException` cause
    (`"+1000000001-01-01T00:00:00Z"`, `"+1000000000-12-31T23:59:59-00:00:01"`, `"-9999999999-12-31T23:59:59Z"`).
    The test compares the message only up to `TemporalAccessor`, because the JDK appends a `HashMap` dump in a
    non-deterministic order; e.g. append `"{InstantSeconds=" + epochSecond + "}"`. Otherwise
    `return create(epochSecond, nano)` (so `"1970-01-01T00:00:00Z"` returns the `EPOCH` object; the test checks `==`).

### Messages

- `abbr` is `text.toString()`, or, when `text.length() > 64`, `text.subSequence(0, 64).toString() + "..."`.
- Parse error: `new DateTimeParseException("Text '" + abbr + "' could not be parsed at index " + i, text, i)`
  (no cause).
- Unparsed text: see step 12 (no cause).
- Resolve error: `new DateTimeParseException("Text '" + abbr + "' could not be parsed: " + cause.getMessage(), text, 0, cause)`
  where `cause` is a `new DateTimeException(message)`. Error index 0.
- `getParsedString()` must equal `text.toString()` (the existing constructor does this). A `StringBuilder` input
  must work: use only `length()`, `charAt()`, `subSequence()` and `toString()` on `text`.

### epochDay (copy of `LocalDate.toEpochDay`)

    long y = year, m = month, total = 365 * y;
    if (y >= 0) total += (y + 3) / 4 - (y + 99) / 100 + (y + 399) / 400;
    else total -= y / -4 - y / -100 + y / -400;
    total += (367 * m - 362) / 12;
    total += day - 1;
    if (m > 2) { total--; if (!isLeap(year)) total--; }
    return total - 719528;   // days from 0000-01-01 to 1970-01-01

Here `year` is the **full** parsed year (not `year % 10000`), so `+12011` and `-12345` come out right.

## Steps

1. Read the test, then `java/time/Instant.java`, `java/time/Duration.java` (its `parse` is a hand-written
   parser in the same style) and `java/time/format/DateTimeParseException.java`, all under
   `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Add `import java.time.format.DateTimeParseException;` and `parse(CharSequence)` with its private helpers to
   `Instant.java`. No registration is needed: `Instant.java` and `DateTimeParseException.java` are already
   registered. Do not touch `BytecodeExceptionRuntime.java`.
3. `javaw-agent-kit/bin/kit check T70` until PASS.
4. Write `docs/bytecode-instant-parse.md` (below), the report, then `javaw-agent-kit/bin/kit finish T70`.

## Hints

- Write one small private helper that reads exactly two digits at a position and returns `-1` when either char is
  missing or not `'0'..'9'`; use it for month/day/hour/minute/second. Write a second one for the offset groups
  that also returns `-1` when the value is > 59.
- Every syntax failure (steps 1-8) is `throw parseError(text, index)` where `index` is the position where the
  failing element **starts** (the literal, the 2-digit number, the offset sign), never where the bad char is
  (`"2011-1a"` -> 5, not 6). The only exceptions are the sign rules of step 1.
- Every value failure (steps 9-10) has index 0, whatever field is wrong.
- Year sign rules are the confusing part: `-201` fails at 1, `+2011` and `12011` fail at 0, `+01970` is valid.
- Compute `epochSecond` with `long` arithmetic; `hour * 3600` in `int` is fine but `epochDay * 86400` needs `long`.
- `DateTimeException` is in the same package; only `DateTimeParseException` needs an import.
- Calibration pitfalls: checking for unparsed text right after the offset (before the month/day/time checks)
  gives `"...ZZ"` the wrong message; the JDK validates the date-time first (its nested parser fails at index 0
  before the outer formatter looks for leftover text), but the `Instant` range check comes after it.
- The Wasm runner stops at the first failing fixture line of each `@Test` method; after a fix another input may
  fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
parsesValidText, reportsSyntaxErrorIndex, reportsValueErrors, roundTripsToString: if FIX THE RUNTIME names `java/time/Instant.parse` or `link`, the method is missing or calls a class/method the runtime does not have (no DateTimeFormatter, LocalDate, ZoneOffset, Character.isDigit); if it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
valid: lower-case `t`/`z` are accepted; `.` with no digits is nano 0; the fraction is padded on the right to 9 digits; `23:59:60` becomes `:59`, `24:00:00` is the next day; the offset is subtracted (`10:15:30+01:00` is 09:15:30Z); epochDay uses the full year with the LocalDate.toEpochDay formula; a + sign with 5+ digits is valid (`+01970`).
syntaxErrors: the error index is where the failing element starts (literal, 2-digit field, offset sign); year: fewer than 4 digits fails at the first digit position, `-0000`, `+` with <= 4 digits and an unsigned 5+ digit year fail at 0; only 10 year digits are read; leftover text after a complete offset is `could not be parsed, unparsed text found at index N`; offset groups above 59 are invalid and the `:ss` group is optional.
valueErrors: invalid month/day/hour/minute/second and offsets beyond +-18:00 fail at index 0, and these checks run before the unparsed-text check (`2011-13-03T10:15:30ZZ` is index 0); offset hours 24..59 give `could not be parsed: Value out of range: Hour[0-23], Minute[0-59], Second[0-59]` with a DateTimeException cause; an epoch second outside MIN_SECOND..MAX_SECOND gives `could not be parsed: Unable to obtain Instant from TemporalAccessor...` with a DateTimeException cause; texts longer than 64 chars are cut to 64 plus `...` in the message; null is a NullPointerException.
roundTrip: parse(i.toString()) must equal i for years beyond 9999 and before 0: use the full signed year in epochDay and the `+` rule of the year step.
leavesOutTemporalMembers: this fails when from, now, atOffset or a DateTimeFormatter became linked. Do not add those members or types.
```

## Docs

Create `docs/bytecode-instant-parse.md` with this skeleton (fill in the brackets):

    # Bytecode Instant.parse

    Revision `instant-parse-vN` links `java.time.Instant.parse(CharSequence)` in the bundled exception runtime.

    Contract:

    - <ISO_INSTANT grammar: signed 4-10 digit year, 2-digit fields, case-insensitive T/Z, required seconds,
      optional 0-9 digit fraction, Z or +-HH:MM[:ss] offset; 24:00 and 23:59:60 handling; error index and the
      three message forms; 64-char abbreviation; epoch day calculation>

    Known differences: implemented by a dedicated parser inside `Instant`; there is no `DateTimeFormatter`,
    `TemporalAccessor` or `Parsed`. The "Unable to obtain Instant from TemporalAccessor" message has a different
    suffix than the JDK's (which dumps a HashMap in an unspecified order).

    Evidence: `BytecodeInstantParseTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
    two fresh instances). `Instant.from`, `Instant.now`, `Instant.atOffset` and `DateTimeFormatter.parse` are
    rejected before WAT generation.
