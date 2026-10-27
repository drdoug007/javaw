# Pinned Jackson numeric output

Fixtures link unmodified NumberOutput, DoubleToDecimal, FloatToDecimal and MathUtils class files from Jackson Core 2.15.4, SHA-256 `8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94`. No library methods are replaced or removed. This selects four original classes, not the entire Jackson JAR or serializer. The JVM oracle uses the original artifact; javac fixtures target Java 17 and the Surefire JVM oracle is GraalVM CE Java 21.0.2 (Maven itself launches on Java 25.0.3).

`BytecodeJacksonNumberOutputTest` compares observable hashes against the JVM for both JAR entry orders and two fresh dirty-memory instances per exported invocation. Integer array cases include decimal group boundaries, negative values, minimum/maximum values, offsets, return positions, the entire output buffer (including untouched sentinels), null and insufficient-buffer failures. Array failures are compared at the IndexOutOfBoundsException family level; exact subclasses/messages and partial writes on invalid inputs are not claimed. String cases cover cache boundaries and full-width values. Floating string cases exercise default, explicit JDK and explicit Schubfach paths, including signed zero, subnormals, normal boundaries, extrema, infinities and NaN. Non-finite predicates include deterministic raw-bit patterns.

Completed focused cases (each passes):

- `intChars`
- `intBytes`
- `longChars`
- `longBytes`
- `intStrings`
- `longStrings`
- `doubleStrings`
- `floatStrings`
- `finiteDouble`
- `finiteFloat`

No runtime or admission semantics change in this batch; compiler revision remains v250. The 63-JAR audit baseline passes unchanged (26,625 classes and 734 resources). Complete Jackson serializer/parser execution, whole-JAR admission, sustained allocation stability and the larger text/numeric/JDK/framework milestones remain open.

Batch validation: all 561 tests pass in the prescribed offline bytecode selection (four forks, JVMCI compilation disabled), with zero failures, errors or skips, including all ten new output cases. The audit baseline passes unchanged: 63 JARs, 26,625 classes and 734 resources. Compiler revision remains v250. Fixtures target Java 17 bytecode; Surefire uses GraalVM CE Java 21.0.2 as the JVM oracle. Whole-JAR, sustained-memory, dependency-closure and framework execution remain open.
