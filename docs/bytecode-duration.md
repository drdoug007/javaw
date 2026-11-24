# Bytecode Duration

Revision `duration-v319` links `java.time.Duration` (core subset), `java.time.DateTimeException` and
`java.time.format.DateTimeParseException` in the bundled exception runtime.

Contract:

- Normalised value: whole `seconds` (long) plus `nanos` in `[0, 999999999]`, with a shared `ZERO` object
  returned by every zero-valued factory and arithmetic result (`create` checks `(seconds | nanos) == 0`).
- Factories `ofDays/ofHours/ofMinutes/ofSeconds(long)/ofSeconds(long,long)/ofMillis/ofNanos`: floor-second
  normalisation via `Math.floorDiv`/`Math.floorMod`; `ofDays/ofHours/ofMinutes` use `Math.multiplyExact` and
  `ofSeconds(long,long)` uses `Math.addExact`, so overflow throws `ArithmeticException`.
- `plus(Duration)/plusDays/plusHours/plusMinutes/plusSeconds/plusMillis/plusNanos` and the matching
  `minus` variants (each handling the `Long.MIN_VALUE` argument as `plusX(MAX_VALUE).plusX(1)`); a zero
  adjustment returns `this`.
- `multipliedBy(0)` is `ZERO`, `multipliedBy(1)`/`dividedBy(1)` return `this`; the general path scales
  seconds + nanos/10^9 as a `BigDecimal` and normalises; `dividedBy` truncates toward zero
  (`RoundingMode.DOWN`), `dividedBy(0)` throws `ArithmeticException("Cannot divide by zero")`, and a result
  outside `long` seconds throws `ArithmeticException`. `negated()` is `multipliedBy(-1)`; `abs()` returns
  `this` or the negation.
- `toDays/toHours/toMinutes` divide the seconds field only; `toSeconds` is the field; `toMillis`/`toNanos`
  add 1 to negative seconds and subtract 1_000_000_000 from nanos before `Math.multiplyExact`/`addExact`.
- `compareTo` (seconds then `nanos - other.nanos`), `equals`, `hashCode`
  (`(int)(seconds ^ (seconds >>> 32)) + 51 * nanos`), `isZero`, `isNegative`, `getSeconds`, `getNano`.
- ISO-8601 `toString`: hours are the largest unit (never days), zero prints `PT0S`, negative values with
  nanos use the `-0` seconds branch and write the fraction from `2_000_000_000 - nanos` with trailing zeros
  removed (positive values from `nanos + 1_000_000_000`).
- `parse(CharSequence)`: hand-written (no `java.util.regex`), case-insensitive letters, `,` like `.`,
  at most 9 fraction digits (padded with zeros, signed by the seconds number), leading `+`/`-` negates the
  whole result, each number group is `[-+]?[0-9]+` (a lone sign is the plain rejection), `"P1Dt"` accepted and an empty uppercase `T` rejected. Every rejection is
  `DateTimeParseException("Text cannot be parsed to a Duration", text, 0)`; number errors add
  `": days"/": hours"/": minutes"/": seconds"` with the original `NumberFormatException`/
  `ArithmeticException` as cause, and `addExact`/negated overflow adds `": overflow"`; `parse(null)` throws
  `NullPointerException("text")`.
- `DateTimeException` extends `RuntimeException` (`(String)` and `(String,Throwable)` constructors);
  `DateTimeParseException` extends it and stores `parsedData.toString()` and `errorIndex`
  (`getParsedString`/`getErrorIndex`).

Known differences: no `TemporalAmount` interface and no TemporalUnit/Temporal members (`of(long, TemporalUnit)`,
`between`, `get`, `addTo`, ...); no Java 9 `toXxxPart`/`withSeconds`/`dividedBy(Duration)`; no serialization
proxy. ArithmeticException messages come from the runtime's `Math` and differ from the JDK's.

Evidence: `BytecodeDurationTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `Duration.of(long, TemporalUnit)`, `between`, `get(TemporalUnit)`, `dividedBy(Duration)`,
`withSeconds` and `toHoursPart` are rejected before WAT generation.
