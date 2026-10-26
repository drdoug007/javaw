# Bytecode square roots and angle conversion

Revision `sqrt-angle-math-v78` adds ordinary bundled `java.lang.Math.sqrt(double)`, `toRadians(double)` and `toDegrees(double)` declarations, together with the Java 17 `E` and `PI` constants. These APIs belong to the isolated bytecode runtime selected by `compileJarWithExceptionRuntime`, `compileClassWithExceptionRuntime` or CLI `--exception-runtime`; source facades do not supply them.

## Runtime contract

Square roots use a descriptor-qualified internal `ExceptionSupport.doubleSqrt(D)D` adapter lowered to WebAssembly `f64.sqrt`. This performs correctly rounded IEEE-754 binary64 square roots without a host import or iterative approximation. Positive and negative zero retain their signs; positive infinity stays infinite. Negative finite values, negative infinity and NaNs produce NaN. NaN payloads and signs are unspecified, as in the Java Math contract. Existing Double bit conversions allow callers to distinguish signed zeros and canonicalize NaNs.

Angle conversions multiply by the Java 17 binary64 constants `0.017453292519943295` (radians per degree) and `57.29577951308232` (degrees per radian), in that order of operations. They preserve Java rounding, overflow, underflow and special values. They do not promise exact mathematical inverses or exact trigonometric identities.

Public calls and method references pass through ordinary linked Math declarations. A malformed `sqrt(J)D` call fails admission rather than being accepted as the square-root intrinsic. No JavaScript math bridge or additional host capability is required.

## Executable evidence

`BytecodeNumericTest` supplies four regressions:

- `squareRootsMatchIeeeBoundariesAndRawPatterns` compares canonical result-bit hashes with the JVM, including signed zero, infinities/NaNs, negative values, subnormals, maximum finite input, both neighbors of every binary exponent boundary and 10,000 deterministic raw-bit patterns. Direct calls and `DoubleUnaryOperator` method references execute in Wasm.
- `angularConversionsPreserveRoundingAndSpecialValues` compares both conversion directions with JVM output across special values, familiar angles and 10,000 deterministic raw patterns, including method references.
- `executesOriginalPinnedH2StandardDeviationBodies` verifies the SHA-256 of H2 2.2.224 and retains the original QueryStatisticsData$QueryEntry constructor, fields and two standard-deviation method bodies. A test-only setter supplies the private accumulated moments and public count; the retained H2 methods compute division followed by Math.sqrt. The same selected class runs on the JVM and in Wasm with ordinary, zero, overflow, invalid and NaN states. The clock-dependent update method is excluded; this does not execute complete query-statistics updates, SQL or the H2 engine.
- `malformedSquareRootDescriptorDoesNotBypassLinkage` uses an ASM fixture to prove that a wrong public method descriptor is rejected before WAT generation.

Executable cases use both JAR orders, two independent Wasm instances and the default imported-memory limit. Hashes canonicalize NaNs and preserve signed-zero distinctions. The selected whole-JAR inventory now reports 973 declared signatures, 62 missing members and 302 missing owners. The newly declared Math signatures are referenced by H2 and Jackson; counts are not a reachability or full-library execution claim. The reviewed 63-JAR dependency inventory remains unchanged apart from the compiler revision.

## Remaining scope

Trigonometric, logarithmic, exponential, general powers and fused multiply-add APIs remain unsupported. Jackson's FFT multiplier and H2 SQL math dispatch require other APIs and are not established by these tests. Broader text/numeric coverage, context decimal arithmetic, concurrency, full core-JDK closure and framework startup remain open.

## Validation

All 343 bytecode tests pass with zero failures, errors or skips. The required offline reactor test command used `-DforkCount=4` to run test classes in four isolated JVMs; each runtime fixture still verifies independent Wasm instances. The reviewed `python3 runtime-target/audit.py --check-baseline` gate passes, with only the compiler revision changed in the committed dependency baseline.
