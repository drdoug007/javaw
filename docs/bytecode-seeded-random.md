# Bytecode Seeded Random

Revision `seeded-random-v308` links `java.util.Random` with an explicit seed in the bundled exception runtime.

Contract:

- The generator is the JDK's 48-bit linear congruential generator: `next(bits)` does
  `seed = (seed * 0x5DEECE66DL + 0xBL) & ((1L << 48) - 1); return (int) (seed >>> (48 - bits));` and
  `initialScramble(seed)` is `(seed ^ 0x5DEECE66DL) & mask`. `Random(long seed)` scrambles directly only
  when `getClass() == Random.class`; a subclass goes through the overridable `setSeed(seed)`, so a subclass
  whose `setSeed` does not call `super` keeps the seed field 0. `setSeed` scrambles the seed and clears the
  `nextGaussian` cache.
- Each method calls only these overridable members of `this`: `nextInt()` → `next(32)`; `nextInt(bound)` →
  `next(31)` only; `nextInt(origin, bound)` → `nextInt()`; `nextLong()` → `next(32)` twice (the low word is
  added signed); `nextBoolean()` → `next(1)`; `nextFloat()` → `next(24) * 0x1.0p-24f`; `nextDouble()` →
  `(((long) next(26) << 27) + next(27)) * 0x1.0p-53`; `nextGaussian()` → `nextDouble()` in pairs; `nextBytes` →
  `nextInt()` (one per 4 bytes, low byte first, signed shift).
- `nextInt(bound)` throws `IllegalArgumentException("bound must be positive")` for `bound <= 0` before any
  `next` call; `nextInt(origin, bound)` throws
  `IllegalArgumentException("bound must be greater than origin")` for `origin >= bound`. Both rejection loops
  are the JDK's (bounded `nextInt` with `bound & (bound - 1)` power-of-2 shortcut; the two-argument range
  version with `n = bound - origin`, which may overflow, and a rejection loop when the range size overflows).
- `nextGaussian()` uses the JDK's polar method with `StrictMath.sqrt(-2 * StrictMath.log(s) / s)`, rejecting
  `s >= 1 || s == 0`; the second value is cached in `nextNextGaussian` and the second call returns it with no
  `next` calls. `setSeed` clears the cache; other methods do not touch it. `nextBytes(null)` throws
  `NullPointerException` from `bytes.length`.

Known differences: `Random` does not implement `java.util.random.RandomGenerator` here, and the seed is a plain
`long` (no `AtomicLong`, no `synchronized`). `Random()` (time and entropy), the `ints`/`longs`/`doubles` streams,
the other `RandomGenerator` defaults, `Random.from` and serialization are not linked.

Evidence: `BytecodeSeededRandomTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `Random()`, `ints()`, `nextLong(long)` and `nextDouble(double,double)` are rejected before WAT
generation.
