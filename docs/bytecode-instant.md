# Bytecode Instant

Revision `instant-v324` links `java.time.Instant` (core subset) in the bundled exception runtime.

Contract:

- `Instant` is a final value type with `private final long seconds` (the floor second) and
  `private final int nanos` in `[0, 999_999_999]`, `implements Comparable<Instant>, java.io.Serializable`
  (no `Temporal`/`TemporalAdjuster`). `EPOCH` is `new Instant(0, 0)`; `MIN` and `MAX` are
  `ofEpochSecond(MIN_SECOND, 0)` and `ofEpochSecond(MAX_SECOND, 999_999_999)`, initialised after
  `MIN_SECOND = -31557014167219200L` and `MAX_SECOND = 31556889864403199L`.
- `create` returns the shared `EPOCH` object when `(seconds | nanos) == 0`, throws
  `DateTimeException("Instant exceeds minimum or maximum instant")` outside `[MIN_SECOND, MAX_SECOND]`,
  and otherwise returns a new value. `ofEpochSecond(s)` is `create(s, 0)`; `ofEpochSecond(s, adj)` is
  `create(Math.addExact(s, floorDiv(adj, 10^9)), (int) floorMod(adj, 10^9))` (`ArithmeticException` on
  long overflow, `DateTimeException` out of range); `ofEpochMilli(m)` is
  `create(floorDiv(m, 1000), (int) floorMod(m, 1000) * 10^6)` and never throws.
- Private `plus(s, n)` returns `this` when `(s | n) == 0` and otherwise
  `ofEpochSecond(addExact(addExact(seconds, s), n / 10^9), nanos + n % 10^9)`, so the range check runs
  on the result. `plusSeconds/plusMillis/plusNanos` are `plus(s, 0)`,
  `plus(m / 1000, (m % 1000) * 10^6)` and `plus(0, n)`; `minusX(v)` is
  `v == Long.MIN_VALUE ? plusX(Long.MAX_VALUE).plusX(1) : plusX(-v)` for Seconds/Millis/Nanos.
- `toEpochMilli()` is `addExact(multiplyExact(seconds + 1, 1000), nanos / 10^6 - 1000)` when
  `seconds < 0 && nanos > 0`, otherwise `addExact(multiplyExact(seconds, 1000), nanos / 10^6)`, dropping
  the sub-millisecond part; overflow throws `ArithmeticException`.
- `compareTo` is `Long.compare(seconds, other.seconds)`, and when the seconds are equal the raw
  `nanos - other.nanos`; `isAfter`/`isBefore` compare it against zero; `equals` is `this == other` or an
  `Instant` with both fields equal (false for `null` and for non-`Instant` values); `hashCode` is
  `(int)(seconds ^ (seconds >>> 32)) + 51 * nanos`.
- `toString()` is the `ISO_INSTANT` form `yyyy-MM-ddTHH:mm:ss[.sss...][Z]`: seconds are always printed
  (`1970-01-01T00:00:00Z`), the fraction is printed only when `nanos > 0` in groups of three digits
  (`.100`, `.123456700`, `.000000001`), and the era split follows the JDK
  (`SECONDS_PER_10000_YEARS = 146097 * 25 * 86400`, `SECONDS_0000_TO_1970`): floor division for year 0 or
  later (a `+hi` prefix concatenated with the in-era year), truncating division with the
  `replace(pos, pos + 2, hi - 1)` / `insert(pos, hi)` / `insert(pos + 1, Math.abs(hi))` fix-ups before it. The
  date is a hand-written proleptic Gregorian calendar (`LocalDate.ofEpochDay`, with `floorDiv`/`floorMod`
  for negative epoch seconds) and the year is padded like `LocalDate.toString` (`0000`, `-0001`, `-0999`,
  `+10000`, `-10000`): `MIN` is `-1000000000-01-01T00:00:00Z` and `MAX` is
  `+1000000000-12-31T23:59:59.999999999Z`.

Known differences: no `Temporal`/`TemporalAdjuster` interfaces and no TemporalAmount/TemporalUnit/zone members
(`plus(Duration)` compiles to `plus(TemporalAmount)` and is not linked); no `now()` (no clock) and no `parse`;
no serialization proxy. ArithmeticException messages come from the runtime's `Math` and differ from the JDK's.

Evidence: `BytecodeInstantTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `plus(TemporalAmount)`, `minus(TemporalAmount)`, `now()`, `parse`, `truncatedTo` and `from`
are rejected before WAT generation.
