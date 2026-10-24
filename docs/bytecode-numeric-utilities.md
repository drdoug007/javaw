# Bytecode numeric and collection utility subsets

These APIs belong to the isolated bundled single-threaded bytecode runtime. They do not establish full JDK or framework support.

## 1. Integer bit operations and reductions

Population/zero counts, high/low bits, reversal, rotations (including masked negative distances), sum/min/max preserve 32-bit Java semantics. Deterministic bit-pattern and boundary comparisons execute in Wasm. Revision `integer-bits-v51`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.

## 2. Long bit operations and reductions

64-bit population/zero counts, high/low bits, byte/bit reversal, masked rotations, signum and sum/min/max execute across zero, every single-bit position, extremes and deterministic mixed patterns. Revision `long-bits-v52`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.

## 3. Unsigned Integer arithmetic and text

Unsigned comparison/division/remainder, zero-extension, radix formatting and String/CharSequence parsing support all 32-bit patterns. Overflow, negative signs (including -0), invalid input/slices and zero divisors raise Java exceptions; failure message text is not promised. Revision `unsigned-integers-v53`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.

## 4. Unsigned Long arithmetic and text

Unsigned 64-bit comparison, quotient/remainder and radix parsing/formatting avoid floating-point approximation. Tests cover high-bit divisors/dividends, maximum unsigned values, every radix, Unicode digits and typed overflow/null/bounds/zero failures. Revision `unsigned-longs-v54`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.

## 5. Checked additive Math operations

Int/long addExact, subtractExact, incrementExact, decrementExact, negateExact and toIntExact throw catchable ArithmeticException on overflow. Cross-products include both signed extrema and narrowing boundaries; exception messages remain unspecified. Revision `checked-additive-v55`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.

## 6. Checked multiplication and full/high products

Int/long multiplyExact (including mixed long/int), multiplyFull and signed multiplyHigh preserve full-width products. Tests compare signed boundaries and 2,000 deterministic high-product pairs; overflow throws ArithmeticException. Revision `checked-products-v56`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.

## 7. Math floor division and modulus

All Java 17 int/long/mixed floorDiv and floorMod overloads preserve divisor-sign remainders, signed overflow behavior and catchable zero-divisor failures. JVM/Wasm tests cross positive/negative and width-boundary values. Revision `floor-division-v57`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.

## 8. Math absolute values, extrema and signs

Int/long abs preserve Java minimum-value overflow; absExact throws. Float/double abs/min/max/copySign/signum preserve signed zero, infinities and NaN behavior. JVM/Wasm comparisons hash canonical NaNs and distinguish both zero signs; no NaN payload guarantee is claimed. Revision `numeric-signs-v58`. `BytecodeNumericTest` compares javac-produced bytecode on the JVM and in Node/Wasm, in both JAR orders and two independent instances. Other APIs remain subject to admission diagnostics.
