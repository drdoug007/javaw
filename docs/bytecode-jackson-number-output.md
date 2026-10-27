# Pinned Jackson numeric output

Fixtures link unmodified NumberOutput, DoubleToDecimal, FloatToDecimal and MathUtils class files from Jackson Core 2.15.4, SHA-256 `8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94`. No library methods are replaced or removed. This selects four original classes, not the entire Jackson JAR or serializer. The JVM oracle uses the original artifact; javac fixtures target Java 17 and the host oracle is Java 25.0.3.

`BytecodeJacksonNumberOutputTest` compares observable hashes against the JVM for both JAR entry orders and two fresh dirty-memory instances per exported invocation. Integer array cases include decimal group boundaries, negative values, minimum/maximum values, offsets, return positions, the entire output buffer (including untouched sentinels), null and insufficient-buffer failures. Array failures are compared at the IndexOutOfBoundsException family level; exact subclasses/messages and partial writes on invalid inputs are not claimed. String cases cover cache boundaries and full-width values. Floating string cases exercise default, explicit JDK and explicit Schubfach paths, including signed zero, subnormals, normal boundaries, extrema, infinities and NaN. Non-finite predicates include deterministic raw-bit patterns.

Completed focused cases (each passes):

- `intChars`
- `intBytes`
- `longChars`
