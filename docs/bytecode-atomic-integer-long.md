# Bytecode AtomicInteger and AtomicLong

Revision `atomic-integer-long-v286` links `java.util.concurrent.atomic.AtomicInteger` and `AtomicLong` in the
bundled exception runtime. This contract applies only to the single-threaded deployment, like
`AtomicReferenceArray`: every access mode is a plain field access and no Java Memory Model ordering is claimed.

Contract:

- `AtomicInteger(int)` / `AtomicLong(long)` store the given value; the no-argument constructors start at `0`.
  Both classes are non-final, extend `Number`, implement `java.io.Serializable`, and hold one `private` value field.
- Every access mode (plain, opaque, acquire, release, volatile) reads or writes that one field: `get`,
  `getPlain`, `getOpaque`, `getAcquire` return the value; `set`, `lazySet`, `setPlain`, `setOpaque` and
  `setRelease` assign it. `getAndSet` returns the old value and stores the new one.
- `compareAndSet` and every `weakCompareAndSet*` variant store `newValue` and return `true` only when
  `value == expectedValue`; otherwise they change nothing and return `false`. `compareAndExchange` and its
  `Acquire`/`Release` variants always return the old value (the witness) and store `newValue` only on a match.
- Arithmetic uses plain wrapping `int`/`long` operations and never throws: `getAndAdd(d)` returns the value
  before `value = value + d`, `addAndGet(d)` returns it after, and the increment/decrement forms are those with
  `1`/`-1` (`1L`/`-1L` for `AtomicLong`). `Integer.MAX_VALUE + 1` wraps to `Integer.MIN_VALUE`, and the same for
  `long`.
- `getAndUpdate(f)`, `updateAndGet(f)`, `getAndAccumulate(x, f)` and `accumulateAndGet(x, f)` follow the JDK
  retry loop: the operator is called with the current value (the accumulator as `f(current, x)` — current first,
  `x` second). If the operator changes the atomic to a different value, the result is discarded and the operator
  is called again with that value; if it leaves the value (or sets the same value) the result is stored, so an
  ordinary call invokes the operator exactly once. `getAnd*` forms return the old value, `*AndGet` forms the new
  one. A `null` function throws `NullPointerException` and an exception thrown by the function propagates
  unchanged; in both cases the value is left untouched.
- `toString()` is `Integer.toString(value)` / `Long.toString(value)`. `intValue()`, `longValue()`, `floatValue()`
  and `doubleValue()` read the value directly and never call one another (`AtomicLong.intValue()` is the low 32
  bits). `byteValue()` and `shortValue()` are the inherited `Number` versions, `(byte) intValue()` and
  `(short) intValue()`. `equals` and `hashCode` are not overridden, so equality is identity.

Evidence: `BytecodeAtomicIntegerLongTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR
orders, two fresh instances).
