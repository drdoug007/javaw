# Bytecode numeric and collection utility subsets

These APIs belong to the isolated bundled single-threaded bytecode runtime. They do not establish full JDK or framework support.

## 1. Integer bit operations and reductions

Population/zero counts, high/low bits, reversal, rotations (including masked negative distances), sum/min/max preserve 32-bit Java semantics. Deterministic bit-pattern and boundary comparisons execute in Wasm. Revision `integer-bits-v51`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.

## 2. Long bit operations and reductions

64-bit population/zero counts, high/low bits, byte/bit reversal, masked rotations, signum and sum/min/max execute across zero, every single-bit position, extremes and deterministic mixed patterns. Revision `long-bits-v52`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.
