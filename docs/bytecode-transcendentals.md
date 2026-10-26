# Bytecode transcendental mathematics

Selected OpenJDK 21.0.2 fdlibm algorithms execute as bundled Java bytecode in Wasm. There are no JavaScript math callbacks. The upstream license, Classpath exception, source hash and adaptation notes are shipped in `exception-runtime/legal/fdlibm/`; the original source header is preserved.

The tested target is Java 17 double semantics. The portable algorithms follow StrictMath's fdlibm definition; Math intrinsics may choose different last bits within the JDK accuracy allowance. Tests compare canonical result bits with JVM StrictMath and independently check JVM Math agreement (allowing two ULPs between independently rounded Math and StrictMath implementations). Signed zero, infinities and NaNs have explicit checks; NaN payloads are not promised.

Both JAR orders and independent Wasm instances execute, with method-reference and direct-call paths. Large trigonometric argument reduction allocates temporary arrays. Batches use fresh host instances; sustained allocation reclamation and whole-framework startup are not established by these tests.

The algorithms close inventoried declarations, not the reachable dependency closure. Additional Math APIs, text APIs and the parent runtime milestone remain open.

## Portable exponential (`fdlibm-exponential-v100`)

Math.exp handles finite range reduction, subnormal rounding, overflow/underflow, signed zeros, infinities and NaNs using the fdlibm exponential algorithm.
