# T45 Seeded Random

```kit
id: T45
slug: seeded-random
title: link java.util.Random with an explicit seed
tests: BytecodeSeededRandomTest
depends: -
allow: javaw-bytecode/src/main/resources/exception-runtime/java/util/Random.java
allow: docs/bytecode-seeded-random.md
```

`slug` becomes the compiler revision name (`1.0-SNAPSHOT-seeded-random-vN`). The test is
`javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeSeededRandomTest.java`.

## Goal

Add `java.util.Random` to the bundled runtime, for callers that pass a seed. The pinned libraries make 20
references to it (`runtime-target/target/audit/member-references.tsv`): H2 `Optimizer.calculateBestPlan` uses
`<init>(long)` and `nextInt(int)` (7 calls in H2), `RandFunction.getValue` uses `setSeed(long)` and `nextDouble()`,
`AggregateDataAnyValue` uses `nextLong()` and `nextBoolean()`; Byte Buddy `ByteBuddy.<clinit>` uses
`<init>(long)` and `nextInt()`; Tomcat `CsrfPreventionFilterBase` and `WsWebSocketContainer` use `nextBytes(byte[])`.
Several of them (H2 `SessionLocal.getRandom`, Tomcat, Spring `MimeTypeUtils`) also call the no-argument `Random()`,
which needs entropy (`System.nanoTime` and a global seed uniquifier). The runtime forbids entropy, so `Random()` stays
unlinked in this task.

The numbers must be bit-for-bit the JDK's: the same 48-bit linear congruential generator, the same constants, and
the same calls between the methods. The test has subclasses that override `next(int)`, `nextInt()`,
`nextDouble()` and `setSeed(long)` and records which of them `Random` calls.

## Members to implement

One new file `javaw-bytecode/src/main/resources/exception-runtime/java/util/Random.java` (`package java.util;`, no
imports): `public class Random implements java.io.Serializable` (do NOT implement
`java.util.random.RandomGenerator`, which is not in the runtime) with

- fields: `private long seed;`, `private static final long multiplier = 0x5DEECE66DL;`,
  `private static final long addend = 0xBL;`, `private static final long mask = (1L << 48) - 1;`,
  `private static final double DOUBLE_UNIT = 0x1.0p-53;`, `private static final float FLOAT_UNIT = 0x1.0p-24f;`,
  `private double nextNextGaussian;`, `private boolean haveNextNextGaussian;`
- `public Random(long seed)`
- `private static long initialScramble(long seed)`
- `public void setSeed(long seed)`
- `protected int next(int bits)`
- `public void nextBytes(byte[] bytes)`
- `public int nextInt()`
- `public int nextInt(int bound)`
- `public int nextInt(int origin, int bound)` (in the JDK a `RandomGenerator` default method; here a plain method)
- `public long nextLong()`
- `public boolean nextBoolean()`
- `public float nextFloat()`
- `public double nextDouble()`
- `public double nextGaussian()`

Nothing else.

## Members to leave out

- `public Random()`: it seeds from `System.nanoTime()` and an `AtomicLong` uniquifier. Time and entropy are
  forbidden in the runtime (`kit guard`). The test checks with `rejectsUnusedCall` that `Random.<init>()V` stays
  unlinked.
- The streams `ints`, `longs`, `doubles` (all overloads): they need `RandomSupport` spliterators. The test checks
  that `ints()` stays unlinked. Never mention `IntStream`, `LongStream` or `DoubleStream` in `Random.java`.
- The other `RandomGenerator` defaults: `nextLong(long)`, `nextLong(long,long)`, `nextFloat(float)`,
  `nextFloat(float,float)`, `nextDouble(double)`, `nextDouble(double,double)`, `nextExponential()`,
  `nextGaussian(double,double)`, `isDeprecated()`. The test checks that `nextLong(long)` and
  `nextDouble(double,double)` stay unlinked.
- `Random.from(RandomGenerator)`, the nested `RandomWrapper`, serialization (`readObject`, `writeObject`,
  `serialPersistentFields`, `Unsafe`) and `java.util.random.RandomGenerator` itself.
- Do not use `AtomicLong` for the seed (a plain `long` field; the runtime is single-threaded) and do not edit any
  other runtime file.

## JDK behaviour to match

Constructor `Random(long seed)`:

    if (getClass() == Random.class) this.seed = initialScramble(seed);
    else setSeed(seed);        // a subclass may override setSeed; the seed field stays 0 if it does not call super

`initialScramble(seed)` is `(seed ^ multiplier) & mask`. So `new Random(0x5DEECE66DL)` has seed field 0 and its first
`nextInt()` is 0. A subclass whose `setSeed` does nothing (the test's `NoSeed`) also keeps seed field 0, so
`new NoSeed(42L)` gives `0 4232237 178803790`, the same as `new Random(0x5DEECE66DL)`. The test's `Spy(42L)` sees
`S42` (one `setSeed(42)` call from the constructor). Seeds that differ only above bit 47 give the same sequence
(`0`, `1L<<48` and `Long.MIN_VALUE` all give `-1155484576 -723955400 1033096058 -1690734402`; `-1` and
`Long.MAX_VALUE` give `1155099827 1887904451 52699159 -1941176418`).

`setSeed(seed)`: `this.seed = initialScramble(seed); haveNextNextGaussian = false;` (the JDK method is
`synchronized`; here it is a plain method).

`next(bits)`:

    seed = (seed * multiplier + addend) & mask;
    return (int) (seed >>> (48 - bits));

`new Random(42L)` gives `nextInt()` values `-1170105035 234785527 -1360544799 205897768`.

Each public method calls only these overridable methods of `this` (the test records them):

- `nextInt()`: `return next(32);`
- `nextInt(bound)`: `if (bound <= 0) throw new IllegalArgumentException("bound must be positive");` (no call when
  it throws; 0, -1 and `Integer.MIN_VALUE`), then

        int r = next(31);
        int m = bound - 1;
        if ((bound & m) == 0) r = (int) ((bound * (long) r) >> 31);   // bound is a power of 2
        else { for (int u = r; u - (r = u % bound) + m < 0; u = next(31)); }
        return r;

  Only `next(31)` is called, never `nextInt()`. With `next` returning `Integer.MAX_VALUE`, `Integer.MAX_VALUE - 1`,
  `Integer.MAX_VALUE - 2`, `nextInt(3)` rejects the first two and returns 2 (three `next(31)` calls); `nextInt(8)` of
  `Integer.MAX_VALUE` is 7.
- `nextInt(origin, bound)`: `if (origin >= bound) throw new IllegalArgumentException("bound must be greater than origin");`
  (no call when it throws), then (the JDK `RandomSupport.boundedNextInt`):

        int r = nextInt();
        int n = bound - origin, m = n - 1;
        if ((n & m) == 0) r = (r & m) + origin;                         // range size is a power of 2
        else if (n > 0) {
            for (int u = r >>> 1; u + m - (r = u % n) < 0; u = nextInt() >>> 1);
            r += origin;
        } else {                                                        // range size overflows int
            while (r < origin || r >= bound) r = nextInt();
        }
        return r;

  It calls `nextInt()` (the overridable one; the `Spy` log is `I n32`), never `next(31)` directly. `n` can overflow:
  `(Integer.MIN_VALUE, 0)` and `(-1, Integer.MAX_VALUE)` have `n == Integer.MIN_VALUE`, a power of 2 by the test
  `(n & m) == 0`; `(Integer.MIN_VALUE, Integer.MAX_VALUE)` and `(Integer.MIN_VALUE + 1, Integer.MAX_VALUE)` take the
  last loop.
- `nextLong()`: `return ((long) next(32) << 32) + next(32);` (the low word is added signed: `next` giving 1 then 0
  is 4294967296; -1 then -1 is -4294967297).
- `nextBoolean()`: `return next(1) != 0;`
- `nextFloat()`: `return next(24) * FLOAT_UNIT;`
- `nextDouble()`: `return (((long) next(26) << 27) + next(27)) * DOUBLE_UNIT;`
- `nextBytes(bytes)`:

        for (int i = 0, len = bytes.length; i < len; )
            for (int rnd = nextInt(), n = Math.min(len - i, 4); n-- > 0; rnd >>= 8)
                bytes[i++] = (byte) rnd;

  One `nextInt()` per 4 bytes, low byte first, signed shift; the rest of the last int is dropped (5 bytes use two
  `nextInt()` calls). A length 0 array makes no call; a null array throws `NullPointerException`.
- `nextGaussian()` (Knuth, polar method; the JDK method is `synchronized`, here plain):

        if (haveNextNextGaussian) { haveNextNextGaussian = false; return nextNextGaussian; }
        double v1, v2, s;
        do {
            v1 = 2 * nextDouble() - 1;
            v2 = 2 * nextDouble() - 1;
            s = v1 * v1 + v2 * v2;
        } while (s >= 1 || s == 0);
        double multiplier = StrictMath.sqrt(-2 * StrictMath.log(s) / s);
        nextNextGaussian = v2 * multiplier;
        haveNextNextGaussian = true;
        return v1 * multiplier;

  It calls `nextDouble()` (overridable, the `Spy` log is `D n26 n27 D n26 n27`); the second call returns the cached
  value with no call at all. The local `multiplier` hides the static field of the same name; that is fine.
  `setSeed` clears the cache: `new Random(8L)`, `nextGaussian()`, `setSeed(9L)`, `nextGaussian()` equals the first
  `nextGaussian()` of `new Random(9L)`. Other methods do not touch the cache.

## Steps

1. Read `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/BytecodeSeededRandomTest.java`. `Spy` logs
   `S<seed>`, `n<bits>`, `I` (nextInt()) and `D` (nextDouble()); `Script` returns scripted `next` values and logs the
   bits; `NoSeed` has an empty `setSeed`. Doubles and floats are compared as bit patterns.
2. Check that `StrictMath.sqrt`, `StrictMath.log` and `Math.min` exist in
   `javaw-bytecode/src/main/resources/exception-runtime/java/lang/` (they do).
3. Create `javaw-bytecode/src/main/resources/exception-runtime/java/util/Random.java` with the members above,
   following the code in "JDK behaviour to match".
4. Register it (never edit `BytecodeExceptionRuntime.java` by hand):
   `javaw-agent-kit/bin/kit register java/util/Random.java`
5. `javaw-agent-kit/bin/kit check T45` until PASS.
6. Write `docs/bytecode-seeded-random.md` (below), the report, then `javaw-agent-kit/bin/kit finish T45`.

## Hints

- Style: copy the compact layout of `java/util/StringJoiner.java` or `java/util/BitSet.java`.
- The constructor must test `getClass() == Random.class` and call `setSeed(seed)` only for subclasses. Calling
  `setSeed` always gives the same numbers for plain `Random`, but the `Spy` log must show exactly one `S42`, and
  `NoSeed` (empty `setSeed`) must keep seed field 0.
- `nextInt(int,int)` must call `nextInt()`, and `nextBytes` must call `nextInt()`, not `next(32)`: `spyCalls` checks
  this. `nextGaussian` must call `nextDouble()`.
- Expected `spyCalls` logs on JDK 21 (each entry is `name[log]`, entries end with a space inside the brackets):
  `new[S42 ] nextInt[I n32 ] nextInt(5)[n31 ] nextInt(16)[n31 ] nextInt(1,4)[I n32 ] nextLong[n32 n32 ]
  nextBoolean[n1 ] nextFloat[n24 ] nextDouble[D n26 n27 ] nextGaussian[D n26 n27 D n26 n27 ] nextGaussian[]
  nextBytes(5)[I n32 I n32 ] setSeed[S3 ]` (one line in the real output).
- Keep `(long)` casts exactly as written: `(bound * (long) r) >> 31`, `(long) next(32) << 32`, `(long) next(26) << 27`.
  Without them the int arithmetic overflows.
- Keep `>>` (not `>>>`) in `nextBytes` (`rnd >>= 8`); with 4 bytes per int it makes no difference, but write it as
  the JDK does.
- Calibration: the reference passed kit check after 1 run (about 70 new lines, 68 of them in Random.java).

## Failure hints

`kit check` prints the matching line under a failure. Follow it first.

```kit-hints
lcgSequences, boundedValues, gaussianAndBytes, subclassCalls, leavesOutEntropyAndStreams: if FIX THE RUNTIME says `link the symbolic class/interface java/util/Random`, `link a complete acyclic type hierarchy: java/util/Random`, `link the descriptor class/interface java/util/Random` or a method of java/util/Random, the class is missing or not registered: run `javaw-agent-kit/bin/kit register java/util/Random.java` (never edit BytecodeExceptionRuntime.java by hand) and add every member listed under Members to implement with the exact signature. If it says `Cannot build bytecode exception runtime`, fix the javac error it quotes.
leavesOutEntropyAndStreams: remove `Random()`, every `ints`/`longs`/`doubles` overload, `nextLong(long)` and `nextDouble(double,double)` from Random.java; do not add a no-argument constructor even a private one.
seedSequences, setSeedRestarts, subclassSeed: next(bits) is `seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1); return (int) (seed >>> (48 - bits));` and initialScramble is `(seed ^ 0x5DEECE66DL) & mask`; the constructor scrambles directly only when `getClass() == Random.class` and otherwise calls `setSeed(seed)` (a subclass with an empty setSeed keeps seed 0: `0 4232237 178803790`).
longsAndBooleans, floatsAndDoubles: nextLong is `((long) next(32) << 32) + next(32)`, nextBoolean `next(1) != 0`, nextFloat `next(24) * 0x1.0p-24f`, nextDouble `(((long) next(26) << 27) + next(27)) * 0x1.0p-53`.
boundedInts: nextInt(bound) throws `new IllegalArgumentException("bound must be positive")` for bound <= 0 before calling next; a power of 2 gives `(int) ((bound * (long) r) >> 31)`; otherwise loop `for (int u = r; u - (r = u % bound) + m < 0; u = next(31));` with m = bound - 1.
rangeInts: nextInt(origin, bound) throws `new IllegalArgumentException("bound must be greater than origin")` when origin >= bound; then r = nextInt(), n = bound - origin (may overflow), m = n - 1; `(n & m) == 0` gives `(r & m) + origin`; n > 0 uses `u = r >>> 1` and `u = nextInt() >>> 1`; otherwise repeat `r = nextInt()` while r < origin or r >= bound.
gaussians: cache the second value in nextNextGaussian and set haveNextNextGaussian; setSeed must clear haveNextNextGaussian; use StrictMath.sqrt(-2 * StrictMath.log(s) / s) and reject s >= 1 or s == 0.
bytes: one nextInt() per 4 bytes, low byte first (`bytes[i++] = (byte) rnd; rnd >>= 8`), `Math.min(len - i, 4)` bytes from each int; a null array throws NullPointerException from `bytes.length`.
spyCalls, scriptedRejection: the call pattern is fixed: nextInt() -> next(32); nextInt(bound) -> next(31) only; nextInt(o,b) -> nextInt(); nextLong -> next(32) twice; nextBoolean -> next(1); nextFloat -> next(24); nextDouble -> next(26), next(27); nextGaussian -> nextDouble() pairs, then nothing for the cached value; nextBytes -> nextInt(); the constructor of a subclass -> setSeed(seed) once.
```

## Docs

Create `docs/bytecode-seeded-random.md` with this skeleton (fill in the brackets):

    # Bytecode Seeded Random

    Revision `seeded-random-vN` links `java.util.Random` with an explicit seed in the bundled exception runtime.

    Contract:

    - <the 48-bit LCG and its constants; the constructor's getClass() test and setSeed call for subclasses; which
      overridable methods each method calls; the two IllegalArgumentException messages; the nextGaussian cache and
      setSeed clearing it; nextBytes byte order>

    Known differences: `Random` does not implement `java.util.random.RandomGenerator` here, and the seed is a plain
    `long` (no `AtomicLong`, no `synchronized`). `Random()` (time and entropy), the `ints`/`longs`/`doubles` streams,
    the other `RandomGenerator` defaults, `Random.from` and serialization are not linked.

    Evidence: `BytecodeSeededRandomTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
    fresh instances). `Random()`, `ints()`, `nextLong(long)` and `nextDouble(double,double)` are rejected before WAT
    generation.
