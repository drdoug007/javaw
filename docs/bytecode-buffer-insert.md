# Bytecode mutable text insertion

The bundled StringBuffer runtime supports sequential text operations. These APIs do not provide monitor synchronization, cross-worker visibility, or general Java Memory Model guarantees. Source facades and complete third-party library execution remain separate contracts.

`BytecodeBufferInsertTest` compiles Java 17 fixtures, runs an independent Java 17 JVM oracle, then compares Wasm results in both JAR orders and two fresh instances whose allocator fills new memory with nonzero bytes. Tests check receiver identity, text, length, negative/extreme offsets, exact exception types and unchanged content after precondition failures. Array/sequence ranges also test invalid starts, lengths/ends and nulls; sequence tests include receiver aliasing. No new real-library execution is claimed by these API fixtures.

Supported additions:

- v180: `Object` insertion overload (`insertObject` regression).
- v181: `Boolean` insertion overload (`insertBoolean` regression).
- v182: `Int` insertion overload (`insertInt` regression).
- v183: `Long` insertion overload (`insertLong` regression).
- v184: `Float` insertion overload (`insertFloat` regression).
- v185: `Double` insertion overload (`insertDouble` regression).
- v186: `Array` insertion overload (`insertArray` regression).
- v187: `ArrayRange` insertion overload (`insertArrayRange` regression).
- v188: `Sequence` insertion overload (`insertSequence` regression).
- v189: `SequenceRange` insertion overload (`insertSequenceRange` regression).

The two array and two sequence overloads are also available on StringBuilder. Primitive/Object StringBuffer overloads use the existing string insertion path after Java value conversion. Array slices check destination offset, then dereference the source, then check slice bounds. Null sequences mean the four characters `null`; null arrays fail. Sequence insertion preserves live receiver aliasing and increments the destination length before invoking `charAt`, including partial mutation when that callback throws. Sequence slice errors use IndexOutOfBoundsException; array slice errors and invalid destination offsets use StringIndexOutOfBoundsException.

The fixture with a custom CharSequence whose text differs from `toString()` proves character reads, and a throwing sequence observes updated length and verifies failure-time content. These are API conformance fixtures, not new pinned-library carriers. Allocation stability and concurrent access remain unproven.

Validation at v189: all ten focused differential tests and all 480 tests in the required offline bytecode suite pass (four forks; zero failures, errors or skips). `python3 runtime-target/audit.py --check-baseline` passes after reviewing each revision-only baseline update: 63 JARs, 26,625 classes, 734 resources, and unchanged selected signature counts.
