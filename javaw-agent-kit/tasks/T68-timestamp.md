# T68 java.sql.Timestamp millisecond/nanosecond core

```kit
id: T68
slug: timestamp
title: link java.sql.Timestamp (millis + nanos core, Instant conversions)
tests: BytecodeTimestampTest
depends: T63
allow: javaw-bytecode/src/main/resources/exception-runtime/java/sql/Timestamp.java
allow: docs/bytecode-timestamp.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-timestamp-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeTimestampTest.java`.

## Goal

Add `java.sql.Timestamp`, a `java.util.Date` subclass that stores whole seconds in the inherited millisecond
value plus a separate `nanos` field, to the bundled runtime. `runtime-target/target/audit/member-references.tsv`
has 20 rows for `java/sql/Timestamp` in 6 artifacts. Construction `<init>(J)V`: 15 sites in Hibernate
`TimestampGenerators.lambda$static$4`, 4 each in H2 `MVStoreTool.formatTimestamp` and Spring JDBC
`StatementCreatorUtils.setValue`, 2 each in Jackson `DateDeserializers$TimestampDeserializer.deserialize` and Tomcat
`JDBCAccessLogValve.log`. `from(Ljava/time/Instant;)` (invokestatic): 11 in Hibernate
`CurrentTimestampGeneration.lambda$static$8`, 1 in Spring Data JPA `Jsr310JpaConverters$InstantToTimestampConverter`.
`toInstant()`: 7 in Hibernate `InstantJavaType.wrap`, 1 in Spring Data JPA
`Jsr310JpaConverters$TimestampToInstantConverter`. `getNanos()I`: 4 Hibernate `SourceGeneration.logResult`, 1 H2
`LegacyDateTimeUtils.fromTimestamp`. `setNanos(I)V`: 4 Hibernate `AbstractPostgreSQLStructJdbcType.parseTimestamp`, 2
H2 `LegacyDateTimeUtils.toTimestamp`. `getTime()J`: 2 each in H2 `LegacyDateTimeUtils.fromTimestamp` and Hibernate
`SourceGeneration.logResult`. `setTime`, `equals`, `hashCode`, `compareTo`, `before` and `after` complete the value
contract (Timestamps are compared and used as map keys through the `Date` API). The rows for `toString()` (H2
`TraceObject.quoteTimestamp`, Tomcat `FileHandler.publish`), `getYear()` and `toLocalDateTime()` (Hibernate
`InstantJavaType.wrap`) and `valueOf(LocalDateTime)` stay out (see below).

Depends on T63 (`java.util.Date`) and T61 (`java.time.Instant`): this task uses `Date(long)`, `Date.getTime()`,
`Date.setTime(long)`, `Date.equals(Object)`, `Date.hashCode()`, and `Instant.ofEpochSecond(long,long)`,
`getEpochSecond()`, `getNano()`. The `java.sql` package already exists in the runtime (T57).

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/sql/Timestamp.java` (`package java.sql;`):
`public class Timestamp extends java.util.Date` with

- field `private int nanos;`
- `private static final int MILLIS_PER_SECOND = 1000;`
- `public Timestamp(long time)`
- `public void setTime(long time)` (overrides `Date`)
- `public long getTime()` (overrides `Date`)
- `public int getNanos()`
- `public void setNanos(int n)`
- `public boolean equals(Timestamp ts)`
- `public boolean equals(Object ts)` (overrides `Date`)
- `public boolean before(Timestamp ts)`
- `public boolean after(Timestamp ts)`
- `public int compareTo(Timestamp ts)`
- `public int compareTo(java.util.Date o)` (overrides `Date`; `Date`'s `compareTo(Object)` bridge dispatches here)
- `public int hashCode()` (overrides `Date`, just `super.hashCode()`)
- `public static Timestamp from(java.time.Instant instant)`
- `public java.time.Instant toInstant()` (overrides `Date`)

`clone()`, `before(Date)` and `after(Date)` are inherited from `Date` unchanged.

## Members to leave out

- `toString()` and `valueOf(String)`: the JDK formats/parses in the **default time zone** through the deprecated
  `Date` fields (`super.getYear()`, `getMonth()`, ... `getSeconds()`), which need `TimeZone`/`BaseCalendar` and are
  not deterministic across hosts. Do not override `toString()` (the inherited `Object.toString` remains; the test
  does not call it). The test checks `valueOf(String)` stays unlinked.
- `Timestamp(int year, int month, int date, int hour, int minute, int second, int nano)` (deprecated, calendar
  code) and the inherited deprecated accessors (`getYear` ...). The test checks `<init>(IIIIIII)V` and `getYear`.
- `toLocalDateTime()` and `valueOf(LocalDateTime)`: there is no `java.time.LocalDateTime` in the runtime. The test
  checks both stay unlinked.
- No `serialVersionUID`, `writeObject`/`readObject`. Do not edit `Date.java`, `Instant.java` or any other runtime
  file (`kit guard` rejects it).

## JDK behaviour to match

Copy the JDK 21 formulas exactly (integer `/` and `%` truncate toward zero; nothing uses `Math.*Exact`):

- `Timestamp(long time)`: `super((time/1000)*1000); nanos = (int)((time%1000) * 1000000); if (nanos < 0) { nanos =
  1000000000 + nanos; super.setTime(((time/1000)-1)*1000); }`. So `new Timestamp(-1L)` stores seconds `-1000` and
  nanos `999000000`, and `getTime()` returns `-1` again. `Long.MIN_VALUE` wraps silently (no exception).
- `setTime(long time)`: the same three statements with `super.setTime((time/1000)*1000)` first. It must call
  `super.setTime` (never `this.setTime` or a field: `Date.fastTime` is private).
- `getTime()`: `long time = super.getTime(); return (time + (nanos / 1000000));` — the sub-millisecond part of
  `nanos` is dropped.
- `getNanos()`: `return nanos;`. `setNanos(n)`: `if (n > 999999999 || n < 0) throw new
  IllegalArgumentException("nanos > 999999999 or < 0"); nanos = n;`. It does **not** touch the millisecond part,
  so `setNanos` on `new Timestamp(2500L)` gives `getTime() == 2000 + n/1000000`.
- `equals(Timestamp ts)`: `if (super.equals(ts)) return nanos == ts.nanos; else return false;`. `Date.equals` does
  `ts instanceof Date && getTime() == ts.getTime()` (both virtual), so `equals((Timestamp) null)` is `false` and
  never throws.
- `equals(Object ts)`: `ts instanceof Timestamp ? this.equals((Timestamp) ts) : false`. So a `Timestamp` never
  equals a plain `Date`, but `date.equals(timestamp)` (Date's method) can be `true`: the relation is asymmetric.
- `compareTo(Timestamp ts)`: `long thisTime = this.getTime(); long anotherTime = ts.getTime(); int i = (thisTime <
  anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1)); if (i == 0) { if (nanos > ts.nanos) return 1; else if
  (nanos < ts.nanos) return -1; } return i;`. Compares `getTime()` (milliseconds) first, then the full `nanos`.
  A `null` argument throws `NullPointerException` from `ts.getTime()`.
- `compareTo(Date o)`: `if (o instanceof Timestamp) return compareTo((Timestamp) o); Timestamp ts = new
  Timestamp(o.getTime()); return this.compareTo(ts);`. A `null` argument throws `NullPointerException` (from
  `o.getTime()`). Through the raw `Comparable` bridge a non-`Date` argument throws `ClassCastException`.
- `before(Timestamp ts)`: `compareTo(ts) < 0`; `after(Timestamp ts)`: `compareTo(ts) > 0`.
- `hashCode()`: `return super.hashCode();` (`Date.hashCode` calls the virtual `getTime()`, so the hash ignores the
  sub-millisecond nanos).
- `from(Instant instant)`: `try { Timestamp stamp = new Timestamp(instant.getEpochSecond() * MILLIS_PER_SECOND);
  stamp.nanos = instant.getNano(); return stamp; } catch (ArithmeticException ex) { throw new
  IllegalArgumentException(ex); }`. The multiplication is plain `long * int` and **wraps silently** for huge
  seconds (the catch is dead code; keep it). A `null` instant throws `NullPointerException`.
- `toInstant()`: `return Instant.ofEpochSecond(super.getTime() / MILLIS_PER_SECOND, nanos);` — `super.getTime()`
  (whole seconds, non-virtual), not `getTime()`.
- Overridable self-calls (the `Spy` subclass logs `getTime`): `compareTo(Timestamp)` calls `this.getTime()` then
  `ts.getTime()`; `compareTo(Date)` with a plain `Date` calls `o.getTime()` (Date's, not logged) then
  `this.getTime()`; `before`/`after` go through `compareTo`; `equals` calls `this.getTime()` then `ts.getTime()`
  (inside `Date.equals`); `hashCode` calls `getTime()` once; `toInstant`, `setTime`, `setNanos`, `getNanos` and the
  constructor call no overridable method. `Date.compareTo(timestamp)` calls `timestamp.getTime()` via
  `getMillisOf`.

## Steps

1. Read the test, then `java/util/Date.java` (T63) and `java/time/Instant.java` (T61) under
   `javaw-bytecode/src/main/resources/exception-runtime/`, and one file in `java/sql/` for the package style.
2. Create `java/sql/Timestamp.java` with the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/sql/Timestamp.java`.
4. `javaw-agent-kit/bin/kit check T68` until PASS.
5. Write `docs/bytecode-timestamp.md` (below), the report, then `javaw-agent-kit/bin/kit finish T68`.

## Hints

- Import `java.time.Instant` in `Timestamp.java` (or use the fully qualified name).
- The constructor and `setTime` must call `super.setTime(...)` for the negative correction. Calling `setTime` (the
  override) from the constructor would recurse into the Timestamp logic and break the result.
- `getTime()` must use `super.getTime()`; calling `getTime()` recurses forever.
- `toInstant()` must use `super.getTime() / MILLIS_PER_SECOND` (whole seconds), not `getTime() / 1000`: with
  `getTime()` the `Spy` log gains a `getA;` and seconds can be off for negative values.
- `from` assigns `stamp.nanos` directly (after the constructor); do not call `setNanos` and do not use
  `Math.multiplyExact` (the JDK wraps on overflow and the fixture checks a wrapped value).
- `equals(Timestamp)` must go through `super.equals(ts)` (that is what calls `getTime()` on both sides and handles
  `null`); compare `nanos` only afterwards.
- `compareTo` uses `<` and `==` (no subtraction); the fixture compares `Long.MIN_VALUE` with `Long.MAX_VALUE`.
- Calibration: the reference built from this spec passed the first `kit check` run; the formulas above are the
  ones that matter. The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another
  fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
millisAndNanos, equalityAndOrdering, instantConversions, overridableSelfCalls, leavesOutZonesAndLocalDateTime: if FIX THE RUNTIME says `link the symbolic class/interface java/sql/Timestamp`, `link a complete acyclic type hierarchy` or names one of its methods, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/sql/Timestamp.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it names `java/util/Date` or `java/time/Instant`, T63/T61 is not done yet. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
construct: the constructor and setTime are super(... (time/1000)*1000), nanos=(int)((time%1000)*1000000), and if nanos<0 then nanos+=1000000000 and super.setTime(((time/1000)-1)*1000); getTime is super.getTime()+nanos/1000000; hashCode is super.hashCode().
nanos: setNanos throws IllegalArgumentException("nanos > 999999999 or < 0") for n>999999999 or n<0 and otherwise only sets the field; clone is inherited from Date.
equality: equals(Timestamp) is super.equals(ts) && nanos==ts.nanos; equals(Object) is false unless the argument is a Timestamp; Date.equals(timestamp) may still be true.
ordering: compareTo(Timestamp) compares getTime() with < and ==, then nanos; compareTo(Date) wraps a non-Timestamp as new Timestamp(o.getTime()); before/after(Timestamp) are compareTo(ts)<0 / >0; null arguments throw NullPointerException.
instants: toInstant is Instant.ofEpochSecond(super.getTime()/1000, nanos); from is new Timestamp(instant.getEpochSecond()*1000) (plain multiply, wraps) then stamp.nanos=instant.getNano(); a null instant throws NullPointerException.
selfCalls: compareTo(Timestamp) calls this.getTime() then ts.getTime(); equals goes through Date.equals (this.getTime() then ts.getTime()); hashCode calls getTime() once; toInstant, setTime, setNanos call no overridable method.
leavesOutZonesAndLocalDateTime: this fails when valueOf, toLocalDateTime, Timestamp(int,...,int) or getYear became linked. Implement only the members listed; do not override toString.
```

## Docs

Create `docs/bytecode-timestamp.md` with this skeleton (fill in the brackets):

    # Bytecode Timestamp

    Revision `timestamp-vN` links `java.sql.Timestamp` in the bundled exception runtime as whole-second `Date`
    milliseconds plus a nanosecond field.

    Contract:

    - <Timestamp(long)/setTime with the negative correction, getTime, getNanos/setNanos and its message,
      equals(Timestamp)/equals(Object) asymmetry with Date, compareTo(Timestamp)/compareTo(Date), before/after,
      hashCode, from/toInstant (wrapping multiply), the overridable self-calls>

    Known differences: no time-zone `toString()`/`valueOf(String)` (the inherited `Object.toString` is used), no
    `LocalDateTime` conversions, no deprecated field constructor, no serialization methods.

    Evidence: `BytecodeTimestampTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `valueOf(String)`, `valueOf(LocalDateTime)`, `toLocalDateTime`, `Timestamp(int,...,int)` and
    `getYear` are rejected before WAT generation.
