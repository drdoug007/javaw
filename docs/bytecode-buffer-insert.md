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
