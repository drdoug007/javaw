# Bytecode LocalTime

Revision `local-time-v345` links `java.time.LocalTime` (core subset) in the bundled exception runtime.

Contract:

- `LocalTime` is a final class holding `byte hour`, `byte minute`, `byte second`, `int nano`; it implements
  `Comparable<LocalTime>` and `java.io.Serializable` (no `Temporal`/`TemporalAccessor`/`TemporalAdjuster`).
  A `static {}` block fills 24 cached whole-hour values `HOURS[i]` before the constants are assigned:
  `MIN = MIDNIGHT = HOURS[0]`, `NOON = HOURS[12]`, `MAX = 23:59:59.999999999`.
- Every factory and value-building method goes through `create(h, m, s, n)`, which returns `HOURS[h]` when
  `(m | s | n) == 0`, so whole hours are shared instances (`of(12, 0) == NOON`,
  `ofSecondOfDay(3600) == of(1, 0)`, `MAX.plusNanos(1) == MIDNIGHT`).
- Factories `of(II)`, `of(III)`, `of(IIII)`, `ofSecondOfDay(long)` and `ofNanoOfDay(long)` throw the JDK
  `DateTimeException` messages `Invalid value for HourOfDay (valid values 0 - 23): v`,
  `Invalid value for MinuteOfHour (valid values 0 - 59): v`,
  `Invalid value for SecondOfMinute (valid values 0 - 59): v`,
  `Invalid value for NanoOfSecond (valid values 0 - 999999999): v`,
  `Invalid value for SecondOfDay (valid values 0 - 86399): v` and
  `Invalid value for NanoOfDay (valid values 0 - 86399999999999): v`; `of(IIII)` checks hour, minute,
  second, nano in that order; `of(II)`/`of(III)` return the cached hour before checking a zero minute/second.
- Getters `getHour`, `getMinute`, `getSecond`, `getNano`; `toSecondOfDay()` = `hour*3600 + minute*60 + second`;
  `toNanoOfDay()` in long arithmetic.
- `withHour`/`withMinute`/`withSecond`/`withNano` return `this` when the value is unchanged, otherwise run
  the field check and go through `create` (so whole-hour results are the cached instances).
- `plusHours/plusMinutes/plusSeconds/plusNanos` wrap around midnight and never throw; the remainder is taken
  before adding (`% 24`, `% 1440`, `% 86400`, `% 86400000000000L`) so `Long.MIN_VALUE`/`Long.MAX_VALUE` do not
  overflow; `plusMinutes`/`plusSeconds`/`plusNanos` return `this` when the time of day is unchanged,
  `plusHours` returns a value built through `create`, so a whole-hour result is the cached instance from `create`. `minusX(v)` = `plusX(-(v % period))`.
- `compareTo` compares hour, minute, second, nano with `Integer.compare` (so the result is -1/0/1);
  `isBefore`/`isAfter` are sign tests; `equals` is same-instance or another `LocalTime` with the same four
  fields (false for `null` and other types); `hashCode()` is `Long.hashCode(toNanoOfDay())`.
- `toString()` is the JDK ISO form: `HH:mm`, `:ss` only when `second > 0 || nano > 0`, then the fraction only
  when `nano > 0` with 3, 6 or 9 digits (the shortest group size that is exact), left zero-padded
  (`10:00:00.001`, `10:00:00.000001`, `10:00:00.000000001`, `10:00:00.123400`).
- `parse(CharSequence)` is a hand-written strict ISO_LOCAL_TIME parser: `null` throws
  `NullPointerException` with message `text`; only ASCII `'0'..'9'` are digits; hour and minute are exactly
  two digits (`'1:15'` fails at 0, `'10-15'` at 2, `'10:1'` at 3); seconds are optional and only consumed when
  `:ss` is complete (`'10:15:3'` is unparsed text at 5); the fraction is optional only after seconds, accepts
  0 to 9 digits (`'10:15:30.'` is nano 0), pads on the right to 9 digits and leaves a 10th digit for the
  unparsed-text error; a remaining tail is `could not be parsed, unparsed text found at index N` and is
  reported before the value checks; values are then checked minute, hour, second (`'99:99:99'` reports
  `MinuteOfHour`, `'24:00'` is rejected) as
  `Text '<abbr>' could not be parsed: <DateTimeException message>` at index 0 with the `DateTimeException`
  as cause. Texts longer than 64 chars are abbreviated to 64 chars plus `...` in messages; the result goes
  through `create`, so `parse("05:00") == of(5, 0)`.

Known differences: no `Temporal`/`TemporalAccessor`/`TemporalAdjuster` interfaces and no clock, field, unit,
date, offset or formatter members (`now`, `from`, `get`, `with(TemporalField, long)`, `truncatedTo`, `atDate`,
`atOffset`, `format`, `parse(CharSequence, DateTimeFormatter)`, ...); no serialization proxy.

Evidence: `BytecodeLocalTimeTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `now`, `from`, `atDate`, `format`, `parse(CharSequence, DateTimeFormatter)`,
`with(TemporalField, long)` and `truncatedTo` are rejected before WAT generation.
