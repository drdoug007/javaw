# Bytecode Period

Revision `period-v331` links `java.time.Period` (core subset) in the bundled exception runtime.

Contract:

- years/months/days `int` value with a shared `ZERO`; `create(y, m, d)` returns the same `ZERO` object when
  `(y | m | d) == 0`, and every factory and every value-building method goes through it, so all-zero results
  (`of(0,0,0)`, `parse("P1W-7D")`, `multipliedBy(0)`) are `ZERO` itself. `of`/`ofYears`/`ofMonths`/`ofDays`
  store the values unnormalised (`Period.of(0, -14, 0)` stays `P-14M`); `ofWeeks(w)` is
  `create(0, 0, Math.multiplyExact(w, 7))`.
- `isZero()` is `this == ZERO`; `isNegative()` is any of the three fields `< 0`. `withYears/withMonths/
  withDays` return `this` when unchanged, else `create(...)`. `plusYears/plusMonths/plusDays(v)` return
  `this` for `v == 0`, else `create(Math.toIntExact(Math.addExact(field, v)), ...)` (long sum, so
  `ofMonths(-1).plusMonths(Integer.MAX_VALUE + 1L)` is `P2147483647M`); `minusX(Long.MIN_VALUE)` is
  `plusX(Long.MAX_VALUE).plusX(1)`.
- `multipliedBy(int)` returns `this` for `ZERO` or scalar 1, else `create` of each `Math.multiplyExact(field,
  scalar)` (so `multipliedBy(0)` is `ZERO`); `negated()` is `multipliedBy(-1)`; `toTotalMonths()` is
  `years * 12L + months`; `normalized()` is `create(Math.toIntExact(total / 12), (int)(total % 12), days)`
  with Java division (both parts take the sign of the total: `P1Y-25M` -> `P-1Y-1M`), returning `this` when
  unchanged; days are never touched.
- `equals` compares the three fields (and rejects `null` and non-`Period`); `hashCode` is
  `years + Integer.rotateLeft(months, 8) + Integer.rotateLeft(days, 16)` (the JDK formula). The class
  `implements java.io.Serializable` but is not `Comparable` and has no serialization proxy.
- `toString()` prints `P0D` for `ZERO`, else `"P"` plus the non-zero `Y`/`M`/`D` parts (`P1Y2M3D`, `P-14M`);
  weeks never appear.
- `parse(CharSequence)` is a hand-written (case-insensitive) reader of the JDK's pattern `([-+]?)P
  (?:([-+]?[0-9]+)Y)?(?:([-+]?[0-9]+)M)?(?:([-+]?[0-9]+)W)?(?:([-+]?[0-9]+)D)?`: an optional leading
  `+`/`-`, then `P`/`p`, then zero or more groups each with an optional sign, at least one ASCII digit and
  one of `Y`/`M`/`W`/`D` in that order, each letter at most once; a group that is only a sign (`P-Y`) is not a
  number, and a missing group set is rejected. Each group's own sign is passed to `Integer.parseInt`
  (`P-1Y+2M` is `P-1Y2M`); the leading sign multiplies every group by `-1`. Rejection is
  `DateTimeParseException("Text cannot be parsed to a Period", text, 0)` with no cause; a
  `NumberFormatException` from `Integer.parseInt` adds a `NumberFormatException` cause and an
  `ArithmeticException` from the leading-sign `Math.multiplyExact` adds an `ArithmeticException` cause
  (message, parsed string, error index 0 and cause class match the JDK). The JDK does not wrap the weeks/days
  combination, so `Math.multiplyExact(weeks, 7)` / `Math.addExact(days, ...)` overflow throws a bare
  `ArithmeticException` (`P306783379W`, `P1W2147483641D`). `parse(null)` throws `NullPointerException("text")`.
  The result goes through `create`, so an all-zero parse (including `P1W-7D`) is `ZERO`.

Known differences: no `TemporalAmount`/`ChronoPeriod` interface and no LocalDate/Temporal members (`between`,
`from`, `get`, `getUnits`, `addTo`, `plus(TemporalAmount)`, ...); no serialization proxy. `ArithmeticException`
messages come from the runtime's `Math` and differ from the JDK's (tests compare the class only for those).

Evidence: `BytecodePeriodTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `between`, `from`, `get`, `getUnits`, `plus(TemporalAmount)` and `addTo` are rejected before
WAT generation.
