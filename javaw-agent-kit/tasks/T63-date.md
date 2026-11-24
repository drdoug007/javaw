# T63 java.util.Date millisecond core

```kit
id: T63
slug: date
title: link java.util.Date (millisecond core, Instant conversions)
tests: BytecodeDateTest
depends: T61
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Date.java
allow: docs/bytecode-date.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-date-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeDateTest.java`.

## Goal

Add `java.util.Date` as a plain wrapper around a millisecond count, with the `java.time.Instant` conversions, to
the bundled runtime. `runtime-target/target/audit/member-references.tsv` has 48 rows for `java/util/Date` in 20
artifacts. Construction `<init>(J)V`: 24 sites in Tomcat `RequestDumperFilter$Timestamp.<init>`, 8 each in Jackson
`SerializerProvider.defaultSerializeDateKey(J...)`, Hibernate `CurrentTimestampGeneration` and logback
`RollingCalendar.collision`, 4 in H2 `FunctionsMySQL.fromUnixTime`, Spring `AbstractApplicationContext.toString` and
`ResourceHolderSupport.setTimeoutInMillis`, log4j `StatusData.getFormattedStatus`, Spring Boot `SimpleFormatter`,
logback-classic `SyslogStartConverter`. `getTime()J`: 36 sites in Hibernate (`DateJavaType$DateMutabilityPlan.deepCopyNotNull`),
22 in Tomcat `ExpiresFilter`, 8 Spring JDBC `StatementCreatorUtils.setValue`, 7 logback `DateTokenConverter`, 6
Jackson, 2 H2 `ObjectDataType$DateType.write`, snakeyaml, Spring Data, JAXB, Spring web/tx/context/boot, AspectJ.
`setTime(J)V`: 6 in Tomcat `RequestDumperFilter.getTimestamp`. `compareTo(Ljava/util/Date;)I`: H2
`ObjectDataType$DateType.compare`. `after(Ljava/util/Date;)Z`: Tomcat `SSLHostConfig.certificatesExpiringBefore`.
`toInstant()`: 15 in Hibernate `DateTimeUtils.appendAsTimestampWithMicros`, 5 Spring `TaskScheduler.schedule`, 3
Spring Data `Jsr310Converters$DateToInstantConverter`, Spring Data JPA `AbstractAuditable.getCreatedDate`, log4j
`ParameterFormatter.appendDate`. `from(Ljava/time/Instant;)` (invokestatic): 7 Hibernate `InstantJavaType.unwrap`, 5
Spring `Trigger.nextExecutionTime`, 4 Spring Data `Jsr310Converters$InstantToDateConverter`, 2 Spring Data JPA
`AbstractAuditable.setCreatedDate`. `equals`, `hashCode`, `before` and `clone` complete the value contract (Hibernate's
`DateJavaType` and maps keyed by dates use them).

Depends on T61 (`java.time.Instant`): this task uses only `Instant.ofEpochMilli(long)` and `Instant.toEpochMilli()`
from the runtime; the test also calls `ofEpochSecond(long,long)`, `getEpochSecond()`, `getNano()` and `equals`.

## Members to implement

`javaw-bytecode/src/main/resources/exception-runtime/java/util/Date.java` (`package java.util;`):
`public class Date implements java.io.Serializable, Cloneable, Comparable<Date>` with

- field `private transient long fastTime;`
- `public Date(long date)`
- `public long getTime()`
- `public void setTime(long time)`
- `public boolean before(Date when)`
- `public boolean after(Date when)`
- `public boolean equals(Object obj)`
- `static final long getMillisOf(Date date)` (package-private helper)
- `public int compareTo(Date anotherDate)` (javac adds the `compareTo(Object)` bridge)
- `public int hashCode()`
- `public Object clone()`
- `public static Date from(java.time.Instant instant)`
- `public java.time.Instant toInstant()`

## Members to leave out

- `Date()`: it reads the wall clock (`System.currentTimeMillis()`), which is not deterministic and cannot be
  compared with the JVM. A later task with a host clock contract. The test checks `<init>()V` stays unlinked.
- `toString()`: the JDK prints the date in the default time zone (`Thu Jan 01 00:00:00 UTC 1970`), which needs
  `TimeZone` and calendar code. Do not override it (the inherited `Object.toString` remains; the test does not call it).
  Also leave out `toGMTString`, `toLocaleString`.
- All deprecated field constructors and accessors (`Date(int,int,int...)`, `Date(String)`, `getYear`...`getSeconds`,
  `setYear`...`setSeconds`, `getDay`, `getTimezoneOffset`), `parse(String)` and `UTC(...)`: they need
  `BaseCalendar`/`TimeZone`. The test checks `<init>(III)V`, `getYear`, `getTimezoneOffset` and `parse` stay unlinked.
- The `cdate` field, `normalize()`, `getCalendarSystem`, `writeObject`/`readObject`. No `java.time` or `sun.util`
  classes beyond `Instant`.
- Do not edit `Instant.java` or any other runtime file (`kit guard` rejects it).

## JDK behaviour to match

JDK 21 source, without the `cdate` calendar cache (it is never set here, so `fastTime` is the whole state):

- `Date(long date)`: `fastTime = date;`. `getTime()`: `return fastTime;`. `setTime(long time)`: `fastTime = time;`.
  Any `long` is accepted, including `Long.MIN_VALUE` and `Long.MAX_VALUE`.
- `getMillisOf(Date date)`: `if (date.getClass() != Date.class) return date.getTime(); return date.fastTime;`.
  So a `null` argument throws `NullPointerException` from `getClass()`, and a subclass's overridden `getTime()` is
  called, while a plain `Date` is read directly (no virtual call).
- `before(when)`: `getMillisOf(this) < getMillisOf(when)`. `after(when)`: `getMillisOf(this) > getMillisOf(when)`.
- `compareTo(another)`: `long thisTime = getMillisOf(this); long anotherTime = getMillisOf(another); return
  thisTime < anotherTime ? -1 : (thisTime == anotherTime ? 0 : 1);` (never subtract: `Long.MIN_VALUE` vs
  `Long.MAX_VALUE` must be `-1`). Through the raw `Comparable` bridge a non-`Date` argument throws
  `ClassCastException`.
- `equals(obj)`: `return obj instanceof Date && getTime() == ((Date) obj).getTime();` — two **virtual** `getTime()`
  calls, `this` first, then the argument; `null` and non-dates are `false` without any call.
- `hashCode()`: `long ht = this.getTime(); return (int) ht ^ (int) (ht >> 32);` (one virtual `getTime()` call; note
  `(int) ht` is cast before the xor, `>>` is the signed shift).
- `clone()`: `return super.clone();` inside `try`/`catch (CloneNotSupportedException e)` returning `null` from the
  catch (never reached: `Date` is `Cloneable`). A subclass's clone has the subclass's class and copies its fields;
  `clone()` calls no `Date` method.
- `toInstant()`: `return Instant.ofEpochMilli(getTime());` (virtual `getTime()`).
- `from(instant)`: `try { return new Date(instant.toEpochMilli()); } catch (ArithmeticException ex) { throw new
  IllegalArgumentException(ex); }`. A `null` instant throws `NullPointerException` (not wrapped). An instant whose
  millisecond value overflows a `long` throws `IllegalArgumentException` whose cause is the `ArithmeticException`
  and whose message is `cause.toString()` (the `Throwable(Throwable)` constructor does that). The test only checks
  that the message starts with `java.lang.ArithmeticException` (the runtime's `Math.multiplyExact` has no
  `long overflow` message).
- Overridable self-calls (the `Spy` subclass logs `getTime` and `setTime`): `equals` -> `this.getTime()` then
  `obj.getTime()`; `hashCode` -> `getTime()`; `toInstant` -> `getTime()`; `compareTo`/`before`/`after` call
  `getTime()` only on the operands that are not exactly `Date.class` (this first, then the argument); `setTime`,
  `getTime` and `clone` call nothing.

## Steps

1. Read the test, then `java/lang/Comparable.java`, `java/lang/Cloneable.java` and, for style, `java/util/UUID.java`
   and `java/time/Instant.java` (from T61) under `javaw-bytecode/src/main/resources/exception-runtime/`.
2. Create `java/util/Date.java` with the members above.
3. Register it (do not edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/Date.java`.
4. `javaw-agent-kit/bin/kit check T63` until PASS.
5. Write `docs/bytecode-date.md` (below), the report, then `javaw-agent-kit/bin/kit finish T63`.

## Hints

- Import `java.time.Instant` in `Date.java` (or use the fully qualified name).
- Keep `getMillisOf` exactly as the JDK writes it (`date.getClass() != Date.class`): that `getClass()` is what makes
  `before(null)`, `after(null)` and `compareTo(null)` throw `NullPointerException`.
- Calibration pitfall: `equals` reading `fastTime` for `this` instead of calling `getTime()` drops the first `getA;`
  from the subclass log (`truegetA;` expected for `a.equals(p)`).
- `equals` must call `getTime()` on both sides even when `obj == this`; there is no `==` shortcut.
- Write `hashCode` exactly as `(int) ht ^ (int) (ht >> 32)`; `Long.hashCode(ht)` is the same value but keep the
  JDK's single `getTime()` call.
- `compareTo`: use `<` and `==` as the JDK does, never `(int) (a - b)`: the fixture compares `Long.MIN_VALUE` with
  `Long.MAX_VALUE`.
- `from` must catch `ArithmeticException` only around `toEpochMilli()`/`new Date`, and wrap it with
  `new IllegalArgumentException(ex)` (cause and message); do not check `instant == null` yourself, let the call throw.
- Do not override `toString()`; the deprecated field members would pull in calendar code.
- The Wasm runner stops at the first failing fixture of each `@Test` method; after a fix another fixture may fail next.

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
millisecondsAndOrdering, instantConversions, overridableSelfCalls, leavesOutClockZonesAndFields: if FIX THE RUNTIME says `link the symbolic class/interface java/util/Date`, `link a complete acyclic type hierarchy` or names one of its methods, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/Date.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement. If it names `java/time/Instant`, T61 is not done yet. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
millis: hashCode is `(int) ht ^ (int) (ht >> 32)` with ht = getTime(); Date implements Serializable, Cloneable and Comparable<Date>.
ordering: compareTo returns -1/0/1 with `<` and `==` (no subtraction); before/after/compareTo with null throw NullPointerException (from getMillisOf's date.getClass()); equals(null) and equals of a non-Date are false.
clones: clone is super.clone() in a try/catch(CloneNotSupportedException); it keeps the subclass and does not call getTime.
instants: toInstant is Instant.ofEpochMilli(getTime()); from(instant) is new Date(instant.toEpochMilli()) with ArithmeticException wrapped as new IllegalArgumentException(ex); a null instant throws NullPointerException unwrapped.
selfCalls: getMillisOf(d) calls d.getTime() only when d.getClass() != Date.class; equals calls this.getTime() then obj.getTime(); hashCode and toInstant call getTime() once.
leavesOutClockZonesAndFields: this fails when Date(), getYear, getTimezoneOffset, parse or Date(int,int,int) became linked. Implement only the members listed.
```

## Docs

Create `docs/bytecode-date.md` with this skeleton (fill in the brackets):

    # Bytecode Date

    Revision `date-vN` links `java.util.Date` in the bundled exception runtime as a millisecond value.

    Contract:

    - <Date(long), getTime/setTime, before/after/compareTo via getMillisOf, equals/hashCode, clone,
      toInstant/from(Instant) with the IllegalArgumentException wrapping, the overridable self-calls>

    Known differences: no `Date()` (no host clock contract yet), no time-zone `toString()` (the inherited
    `Object.toString` is used), no deprecated calendar fields, `parse` or serialization methods.

    Evidence: `BytecodeDateTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two fresh
    instances). `Date()`, `Date(int,int,int)`, `getYear`, `getTimezoneOffset` and `parse` are rejected before WAT
    generation.
