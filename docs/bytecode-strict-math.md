# Linked bytecode StrictMath

These adapters expose the existing bundled numeric implementations to linked
`java.lang.StrictMath` callers. Source facades are a separate compiler target.
The fdlibm implementations retain the provenance and licenses listed in NOTICE.
`random()` is excluded: entropy and reproducibility require a separate host contract.
No full framework execution or concurrent runtime support is implied.

`BytecodeStrictMathTest` compiles Java 17 fixtures and compares JVM checksums with
Wasm in both input JAR orders and two fresh instances with dirty allocated memory.
Floating checksums canonicalize NaNs and preserve signed zeros; NaN payloads are
not promised. Fixtures cover boundary values and deterministic raw-bit patterns,
plus method references. Arithmetic failures are compared by catchable Java type;
exception message text is unspecified. These are API conformance tests, not
additional third-party library execution evidence.

Completed subsets:

- v150: square root, angle conversion and constants (`rootsAndAngles`).
- v151: floor, ceil, ties-to-even rint and saturating round (`rounding`).
- v152: integral and floating absolute values, including checked overflow (`absoluteValues`).
- v153: int, long, float and double min/max with signed zeros and NaNs (`extrema`).
- v154: signum and StrictMath NaN-normalized copySign (`signOperations`).
- v155: exponent extraction and ulp for both precisions (`exponentsAndUlps`).
- v156: nextUp, nextDown and mixed-precision nextAfter (`adjacentValues`).
- v157: scalb including extreme scale factors and subnormal rounding (`binaryScaling`).
- v158: checked integer arithmetic, high/full products and floor division/modulus (`integerArithmetic`).
