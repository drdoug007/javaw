# Bytecode BigInteger foundation

Revision `big-integer-v61` adds `java.math.BigInteger` to the isolated bundled bytecode runtime selected by `compileJarWithExceptionRuntime`, `compileClassWithExceptionRuntime`, or CLI `--exception-runtime`. Its ordinary class-file declarations and implementations are linked and executed in Wasm. It extends the bundled Number class and implements Comparable and Serializable; Serializable is a marker, not an object-stream implementation. Source and bytecode objects retain separate layouts.

Supported APIs:

- ZERO, ONE, TEN; decimal and radix String constructors; signed two's-complement byte arrays and sign/magnitude constructors; valueOf(long).
- Addition, subtraction, multiplication, division, remainder, divideAndRemainder, gcd, nonnegative integer powers, positive-modulus mod, absolute values, negation, min/max and typed/bridge compareTo.
- Decimal/radix text, defensive minimal two's-complement byte arrays, signum, bitLength, testBit and signed left/right shifts (including negative distances).
- Truncating Number conversions and exact byte/short/int/long conversions. Float/double conversions round the integer significand directly to nearest with ties to even, including overflow to infinity.
- Value equality and the JDK magnitude-word hash formula, independent of the decimal storage representation.

Nulls, malformed numerals/signs/radices, zero divisors, invalid moduli, negative bit indices/exponents and inexact conversions raise linked Java exceptions. Failure message strings are not an exact JDK-text contract. Numeric results and byte arrays are independent of constructor input buffers and previously returned arrays.

The implementation starts from the repository's decimal arithmetic algorithms, adapts the actual bytecode API descriptors and Comparable bridge, and supplies new binary/radix, hashing, shifting and IEEE conversion paths. Division performs decimal long division with in-place remainder storage; it does not allocate an object for each trial subtraction. Computation stays inside Wasm. This is not a host BigInt adapter and is not a claim that the source backend's facade establishes binary compatibility.

## Evidence

`BytecodeBigIntegerTest` compiles application fixtures with javac, compares results against the JVM, assembles the generated WAT and executes Node/Wasm in both JAR orders and two independent instances. Tests use the default imported memory limit. Cases include large signed arithmetic, quotient/remainder identities and signs, gcd/powers, exact-conversion boundaries, every radix 2–36, BMP digits, defensive binary round trips, sign extension, shift extremes, and float/double rounding around precision and exponent boundaries. Deterministic mixed-width division cases compare independently with the JVM. An unimplemented `and` call must fail admission.

A separate test executes the **selected original static initializer and fields** of pinned Jackson Core 2.15.4 `com.fasterxml.jackson.core.io.doubleparser.FastIntegerMath`, after verifying the artifact SHA-256 against the pinned lock value. It compares the library's constants and small-power table with JVM execution, including reflected access, exact long construction, multiplication and formatting. The fixture explicitly removes other methods before linking: tree-map and FFT dependencies are still unsupported. This is initializer evidence, **not whole-class/Jackson parser compatibility**. The initializer instructions are not replaced with a source implementation.

## Remaining limits

BigDecimal/MathContext/RoundingMode, general bitwise/modular/primality APIs, random/probable-prime constructors, serialization and other BigInteger overloads remain separate work. Missing method descriptors continue to receive compiler diagnostics. The decimal-magnitude algorithms do not promise JDK performance, and arbitrary precision remains limited by the module's available memory. Managed reclamation, concurrent use and cross-backend marshalling remain separate milestones. This subset does not complete the core JDK milestone or establish H2/Spring/Hibernate startup.

The selected whole-JAR signature inventory now has 681 declared signatures, 205 missing members and 451 missing owners. All 35 inventoried BigInteger signatures have declarations. These figures measure declarations, not proven reachability or compatibility percentages. The 63-JAR opcode/requirement inventory is unchanged; only the compiler revision changes in the audit baseline.

Validation: 267 compiler/runtime regressions passed in the full bytecode run; all five audit regressions pass after updating the missing-owner expectations. The reviewed 63-JAR audit baseline passes with unchanged inventory counts and revision `big-integer-v61`.
