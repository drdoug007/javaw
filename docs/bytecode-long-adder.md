# Bytecode LongAdder and LongAccumulator

Revision `long-adder-v306` links `java.util.concurrent.atomic.LongAdder` and `LongAccumulator` in the bundled
exception runtime. This contract applies only to the single-threaded deployment, like `AtomicLong`: one plain
`long` field, no cells and no Java Memory Model ordering claimed.

Contract:

- `LongAdder()` starts at `0`. `add(x)` is `base += x` with plain wrapping arithmetic (`Long.MAX_VALUE` then
  `increment()` gives `Long.MIN_VALUE`); `increment()` and `decrement()` are virtual calls to
  `add(1L)` / `add(-1L)`. `sum()` returns `base`, `reset()` sets `base = 0`, and `sumThenReset()` returns the
  old `base` and sets it to `0` directly without calling `sum()` or `reset()`.
- `LongAccumulator(f, identity)` stores both, sets `base = identity`, does not check `f` for null and does not
  call it. `accumulate(x)` calls `function.applyAsLong(base, x)` exactly once (current value first, `x` second)
  and stores the result only after the call returned, so a `null` function throws `NullPointerException` and a
  throwing function propagates unchanged, each leaving `base` untouched. `get()` returns `base` without calling
  the function, `reset()` sets `base = identity`, and `getThenReset()` returns the old `base`, sets
  `base = identity` and calls neither `get()`, `reset()` nor the function.
- `toString()` is `Long.toString(sum())` / `Long.toString(get())`. `longValue()` returns `sum()` / `get()`,
  `intValue()` is `(int)` of it (low 32 bits), `floatValue()` is `(float)` and `doubleValue()` is `(double)`;
  each calls the virtual `sum()` / `get()` exactly once and none calls another `Number` method.
  `byteValue()` and `shortValue()` are the inherited `Number` versions, `(byte) intValue()` and
  `(short) intValue()`. Neither class declares `equals` or `hashCode`, so equality is identity.

Known differences: both classes extend `Number` directly (the JDK superclass is the package-private
`Striped64`), so `getSuperclass()` differs. Serialization (`writeReplace`, `SerializationProxy`) is not linked.

Evidence: `BytecodeLongAdderTest` compares Java 17 javac fixtures on the JVM and in Wasm (both JAR orders, two
fresh instances). `LongAdder.writeReplace()` is rejected before WAT generation.
