# Bytecode Timestamp

Revision `timestamp-v330` links `java.sql.Timestamp` in the bundled exception runtime as whole-second `Date`
milliseconds plus a nanosecond field.

Contract:

- `Timestamp(long time)` stores `(time/1000)*1000` through `super(...)` and `nanos = (int)((time%1000) *
  1000000)`; when that product is negative (negative `time` with a sub-millisecond part) it corrects with
  `nanos = 1000000000 + nanos` and `super.setTime(((time/1000)-1)*1000)`, so `new Timestamp(-1L)` stores
  seconds `-1000` and nanos `999000000` and `getTime()` returns `-1` again. `setTime(long)` repeats the same
  three statements with `super.setTime(...)` first. Both call `super.setTime` for the correction (never the
  override), and `Long.MIN_VALUE`/`Long.MAX_VALUE` wrap silently with no exception.
- `getTime()` is `super.getTime() + (nanos / 1000000)` (int division, the sub-millisecond part of `nanos` is
  dropped); `getNanos()` returns the field. `setNanos(int)` throws
  `IllegalArgumentException("nanos > 999999999 or < 0")` for `n > 999999999 || n < 0` and otherwise sets
  only the field, leaving the millisecond value untouched. `clone()` is inherited from `Date`.
- `equals(Timestamp)` is `super.equals(ts) ? nanos == ts.nanos : false` (`Date.equals` does both virtual
  `getTime()` calls and handles `null`), and `equals(Object)` is `ts instanceof Timestamp ?
  equals((Timestamp) ts) : false`, so a `Timestamp` never equals a plain `Date` while
  `date.equals(timestamp)` can be `true` (the relation is asymmetric). `hashCode()` is `super.hashCode()`
  (one virtual `getTime()`, ignoring sub-millisecond nanos).
- `compareTo(Timestamp)` compares `this.getTime()` and `ts.getTime()` with `<` and `==` (never subtraction,
  so `Long.MIN_VALUE` against `Long.MAX_VALUE` is safe) and only on a millisecond tie falls back to the full
  `nanos`; `compareTo(Date)` returns `compareTo((Timestamp) o)` when `o instanceof Timestamp` and otherwise
  wraps `new Timestamp(o.getTime())` first. `before(Timestamp)`/`after(Timestamp)` are `compareTo(ts) < 0` /
  `> 0`; null arguments throw `NullPointerException` from `getTime()`, and through the raw `Comparable`
  bridge a non-`Date` argument throws `ClassCastException`.
- `from(Instant)` is `new Timestamp(instant.getEpochSecond() * 1000)` with a plain wrapping `long * int`
  multiply (the `ArithmeticException` catch is dead code, kept for shape) followed by the direct field
  assignment `stamp.nanos = instant.getNano()`; a `null` instant throws `NullPointerException`. `toInstant()`
  is `Instant.ofEpochSecond(super.getTime() / 1000, nanos)` — the non-virtual whole-second value, so a
  subclass never sees an extra `getTime()` and negative values stay exact.
- Overridable self-calls follow the JDK: `compareTo(Timestamp)` -> `this.getTime()` then `ts.getTime()`;
  `compareTo(Date)` with a plain `Date` -> the argument's `Date.getTime()` then `this.getTime()`;
  `before`/`after` go through `compareTo`; `equals` -> `this.getTime()` then the argument's `getTime()`
  inside `Date.equals`; `hashCode` -> one `getTime()`; `toInstant`, `setTime`, `setNanos`, `getNanos` and
  the constructor call no overridable method.

Known differences: no time-zone `toString()`/`valueOf(String)` (the inherited `Object.toString` is used), no
`LocalDateTime` conversions, no deprecated field constructor, no serialization methods.

Evidence: `BytecodeTimestampTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `valueOf(String)`, `valueOf(LocalDateTime)`, `toLocalDateTime`, `Timestamp(int,...,int)` and
`getYear` are rejected before WAT generation.
