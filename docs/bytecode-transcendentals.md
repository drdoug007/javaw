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
