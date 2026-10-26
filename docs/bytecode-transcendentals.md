# Bytecode transcendental mathematics

Selected OpenJDK 21.0.2 fdlibm algorithms execute as bundled Java bytecode in Wasm. There are no JavaScript math callbacks. The upstream license, Classpath exception, source hash and adaptation notes are shipped in `exception-runtime/legal/fdlibm/`; the original source header is preserved.

The tested target is Java 17 double semantics. The portable algorithms follow StrictMath's fdlibm definition; Math intrinsics may choose different last bits within the JDK accuracy allowance. Tests compare canonical result bits with JVM StrictMath and independently check JVM Math agreement (allowing two ULPs between independently rounded Math and StrictMath implementations). Signed zero, infinities and NaNs have explicit checks; NaN payloads are not promised.

`python3 javaw-bytecode/tools/import-fdlibm.py /path/to/openjdk-21.0.2/lib/src.zip --check` verifies the exact selected source and adaptations against the pinned source hash.

Both JAR orders and independent Wasm instances execute, with method-reference and direct-call paths. Large trigonometric argument reduction allocates temporary arrays. Batches use fresh host instances; sustained allocation reclamation and whole-framework startup are not established by these tests.

The algorithms close inventoried declarations, not the reachable dependency closure. Additional Math APIs, text APIs and the parent runtime milestone remain open.

## Portable exponential (`fdlibm-exponential-v100`)

Math.exp handles finite range reduction, subnormal rounding, overflow/underflow, signed zeros, infinities and NaNs using the fdlibm exponential algorithm.

## Portable natural logarithm (`fdlibm-natural-log-v101`)

Math.log performs fdlibm normalization and split-logarithm evaluation across subnormal/normal ranges; zero yields negative infinity, and negative inputs yield NaN.

## Portable decimal logarithm (`fdlibm-decimal-log-v102`)

Math.log10 uses split base-conversion constants and the portable natural logarithm, preserving exact powers-of-ten results and special values.

## Portable floating powers (`fdlibm-powers-v103`)

Math.pow handles negative-base parity, signed zero, nonintegral domains, extreme exponents and subnormal/overflow results. An isolated carrier also executes the complete original pinned ANTLR 4.13.0 power-rounding method body; buffer/parser execution is excluded.

## Portable sine and shared argument reduction (`fdlibm-sine-v104`)

Math.sin uses split-remainder reduction for moderate inputs and multiprecision reduction for huge finite arguments, with shared sine/cosine kernels. Signed zeros are preserved and nonfinite arguments produce NaN.

## Portable cosine (`fdlibm-cosine-v105`)

Math.cos uses compensated cosine evaluation and the shared large-argument reducer, returns one for both zeros and produces NaN for nonfinite arguments.

## Portable tangent (`fdlibm-tangent-v106`)

Math.tan uses split argument reduction, kernel transformations and compensated reciprocal evaluation near odd-quadrant poles. It preserves signed zero and rejects nonfinite arguments with NaN.

## Portable inverse sine (`fdlibm-inverse-sine-v107`)

Math.asin uses interval-specific rational approximation and compensated square-root transforms near +/-1. It preserves signed zero and returns NaN outside [-1,1]; tests add dense unit-interval and pole-neighbor samples.

## Portable inverse cosine (`fdlibm-inverse-cosine-v108`)

Math.acos evaluates separate positive/negative endpoint transforms with compensated square roots, preserving endpoint values and returning NaN outside [-1,1].

## Portable inverse tangent (`fdlibm-inverse-tangent-v109`)

Math.atan uses interval reduction and odd/even polynomial splitting, preserves signed zeros and handles infinities and NaNs. Malformed ASM descriptors for all ten APIs are rejected before WAT generation.

## Evidence and remaining scope

`BytecodeTranscendentalTest` uses canonical-bit JVM StrictMath oracles for the portable algorithms and independently compares the JVM Math results within two ULPs, with exact special-value/zero checks. Unary inputs span every exponent and sign, deterministic raw patterns, dense unit intervals and adjacent endpoint/pole samples. Power tests cross special values and integer parity boundaries, plus arbitrary and bounded random pairs. Method references must agree with direct calls. Malformed ASM fixtures exercise rejection of nonexistent float descriptors for every API.

The pinned ANTLR 4.13.0 artifact hash is checked against the runtime lock. Its complete original private `CodePointBuffer.Builder.roundUpToNextPowerOfTwo` instruction body is copied into a public test carrier, with only owner/name/access metadata changed. JVM and Wasm execute that same body over integer and power-boundary inputs. This proves selected-method execution; NIO buffers, builders and ANTLR parser startup remain excluded. Other new APIs have numerical conformance evidence, not additional pinned-library execution claims.

The ten audited declarations reduce missing members from 57 to 47 (978 to 988 declared signatures); missing owners remain 302. These are whole-JAR signature counts, not reachable-closure proof. Atan2, hyperbolic functions, random sources, broader numeric/text APIs, sustained managed allocation and full H2/Spring/Hibernate execution remain open.

## Validation

Combined v100–v109 validation: all 384 tests pass in the required offline bytecode suite (four test forks), with zero failures, errors or skips. All 12 new numerical/admission tests, the pinned ANTLR method-body comparison and the source reproduction check pass. The reviewed 63-JAR audit has unchanged dependency inventory totals; selected signature coverage is 988 declared, 47 missing members and 302 missing owners.

## Two-argument inverse tangent (`fdlibm-atan2-v110`)

Math.atan2: Quadrants, signed-zero axes, infinities and extreme ratios use the portable fdlibm atan2 algorithm. Canonical-bit JVM StrictMath comparisons cover both JAR orders, direct calls, method references and fresh Wasm instances. Malformed float descriptors are rejected. No new pinned-library execution claim is made.

## Exponential minus one (`fdlibm-expm1-v111`)

Math.expm1: Compensated exponential evaluation avoids cancellation around zero and handles range endpoints. Canonical-bit JVM StrictMath comparisons cover both JAR orders, direct calls, method references and fresh Wasm instances. Malformed float descriptors are rejected. No new pinned-library execution claim is made.
