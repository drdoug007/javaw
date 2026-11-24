# Bytecode Date

Revision `date-v326` links `java.util.Date` in the bundled exception runtime as a millisecond value.

Contract:

- `Date(long date)` stores the value in `private transient long fastTime`; `getTime()`/`setTime(long)` read
  and write it directly, and any `long` is accepted (`Long.MIN_VALUE` and `Long.MAX_VALUE` included).
- `before(Date)`, `after(Date)` and `compareTo(Date)` compare through the package-private
  `getMillisOf(Date)`, which mirrors the JDK exactly: `date.getClass() != Date.class ? date.getTime() :
  date.fastTime`. So a `null` argument throws `NullPointerException` from `getClass()`, an exact `Date` is
  read without a virtual call, and a subclass is read through its overridden `getTime()`. `compareTo`
  returns `-1`/`0`/`1` using `<` and `==` (never subtraction), and the javac-generated raw
  `compareTo(Object)` bridge throws `ClassCastException` for non-`Date` arguments.
- `equals(Object)` is `obj instanceof Date && getTime() == ((Date) obj).getTime()`: two virtual
  `getTime()` calls (`this` first), no `==` shortcut; `null` and non-`Date` values give `false`.
  `hashCode()` is `(int) ht ^ (int) (ht >> 32)` with one virtual `getTime()` call and the signed `>>`.
- `clone()` returns `super.clone()` in a `try`/`catch (CloneNotSupportedException)` (the catch returns
  `null` and is unreachable because `Date` implements `Cloneable`); it copies the exact subclass and calls
  no `Date` method.
- `toInstant()` is `Instant.ofEpochMilli(getTime())` (virtual `getTime()`). `from(Instant)` is
  `new Date(instant.toEpochMilli())` with the `ArithmeticException` from the millisecond conversion wrapped
  in `new IllegalArgumentException(ex)` (cause attached, message `cause.toString()`); a `null` instant
  throws `NullPointerException` unwrapped.
- `Date` implements `java.io.Serializable`, `Cloneable` and `Comparable<Date>` and overrides no
  `toString()`; the inherited `Object.toString` is used. Overridable self-calls follow the JDK:
  `equals` -> `this.getTime()` then `obj.getTime()`; `hashCode` and `toInstant` -> one `getTime()`;
  `compareTo`/`before`/`after` call `getTime()` only on operands whose class is not exactly `Date`
  (`this` first, then the argument); `setTime`, `getTime` and `clone` call nothing.

Known differences: no `Date()` (no host clock contract yet), no time-zone `toString()` (the inherited
`Object.toString` is used), no deprecated calendar fields, `parse` or serialization methods.

Evidence: `BytecodeDateTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
instances). `Date()`, `Date(int,int,int)`, `getYear`, `getTimezoneOffset` and `parse` are rejected before WAT
generation.
