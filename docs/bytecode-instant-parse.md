# Bytecode Instant.parse

Revision `instant-parse-v332` links `java.time.Instant.parse(CharSequence)` in the bundled exception runtime.

Contract:

- Parses the `DateTimeFormatter.ISO_INSTANT` grammar left to right, with `pos` starting at 0 and every syntax
  failure reporting the position where the failing element *starts*: an optional `+`/`-` sign, then the year
  (4-10 ASCII digits; ASCII only — `Character.isDigit` is never used, so full-width and Arabic digits are
  rejected). Year sign rules: fewer than 4 digits fails at the first digit position; a `-` sign with value 0
  (`-0000`) fails at the sign; a `+` sign with 4 or fewer digits (`+2011`) fails at the sign; an unsigned 5-10
  digit year (`12011`, `01970`) fails at the sign; `+01970` is valid; 11+ digits read only 10 and the next
  step fails at the 11th digit.
- Then `-` month(2) `-` day(2), `T`/`t` (case-insensitive), hour(2) `:` minute(2) `:` second(2) — seconds are
  required. The 2-digit reader accepts `00`-`99` (no range check); a missing or non-digit char fails at the
  field's start, a missing or wrong literal fails at the literal's position.
- Optional fraction: only `.` (`,` is not a decimal point), 0-9 ASCII digits (`.Z` is nano 0); the digits are
  padded on the right to 9 (`.5` = 500000000, `.000000001` = 1). A 10th digit is not consumed, so the offset
  step fails at its position.
- Required offset: `Z`/`z` (offset 0), or `+`/`-` followed by `HH` then `:MM`, each group exactly two digits
  with value <= 59 (any missing or invalid group fails at the offset sign), then an optional `:ss` group
  consumed only when `:` and two digits <= 59 follow; anything else fails at the offset sign. After the groups,
  offset hours 24-59 throw the resolve error `Value out of range: Hour[0-23], Minute[0-59], Second[0-59]`
  with a `DateTimeException` cause (before any other validation).
- `24:00:00` (exactly, nano 0) becomes hour 0 of the next day; `23:59:60` becomes `23:59:59` (leap second,
  nanos kept, any offset); `22:59:60`, `24:00:01`, ... remain invalid.
- Value checks run before the unparsed-text check and all fail at index 0: month 1-12, day 1-`lengthOfMonth`
  (February 29 when the year, taken as `year % 10_000` so it works for signed years, is a Gregorian leap year),
  hour <= 23, minute <= 59, second <= 59, offset within +-18:00. Epoch second is
  `(epochDay(year, month, day) + days) * 86400 + hour * 3600 + minute * 60 + second - offsetSecs` in `long`;
  `epochDay` is the proleptic Gregorian count from 1970-01-01 using the full signed year
  (`LocalDate.toEpochDay` formula).
- Leftover text after a complete parse is `DateTimeParseException("Text '<abbr>' could not be parsed, unparsed
  text found at index N", text, N)`. Three message forms: parse errors `... could not be parsed at index N`
  (no cause), the two resolve errors above `... could not be parsed: <message>` with a `DateTimeException`
  cause and index 0. Messages abbreviate to 64 chars plus `...` when longer; `getParsedString()` is
  `text.toString()`. `parse(null)` throws `NullPointerException("text")`.
- A final range check rejects `epochSecond` outside `MIN_SECOND`..`MAX_SECOND` with the resolve error
  `Unable to obtain Instant from TemporalAccessor: {InstantSeconds=<n>}` (a `DateTimeException` cause, index 0).
  Otherwise the result goes through `create`, so `parse("1970-01-01T00:00:00Z")` is `EPOCH` itself.

Known differences: implemented by a dedicated parser inside `Instant`; there is no `DateTimeFormatter`,
`TemporalAccessor` or `Parsed`. The "Unable to obtain Instant from TemporalAccessor" message has a different
suffix than the JDK's (which dumps a HashMap in an unspecified order).

Evidence: `BytecodeInstantParseTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders,
two fresh instances). `Instant.from`, `Instant.now`, `Instant.atOffset` and `DateTimeFormatter.parse` are
rejected before WAT generation.
