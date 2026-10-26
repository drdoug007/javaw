# Decimal arithmetic, unsigned bytes and UTF-8 text

These APIs execute as bundled bytecode inside Wasm. They do not use source facades or host arithmetic/charset callbacks.

## context decimal division (`context-division-v85`)

BigDecimal.divide(BigDecimal, MathContext) rounds the rational quotient directly, preserves preferred scales for exact results, retains precision for inexact results and checks scale overflow. Precision zero uses exact division; zero divisors, nonterminating unlimited division and UNNECESSARY rounding failures raise ArithmeticException.

The focused JVM/Wasm comparisons pass in both JAR orders and independent instances. The reviewed 63-JAR audit baseline changes only the compiler revision, with unchanged dependency inventory.

## context decimal powers (`context-powers-v86`)

BigDecimal.pow(int, MathContext) uses the Java working precision (requested precision plus exponent digits plus one), the specified repeated-squaring order, reciprocal division for negative powers and final rounding. Exponent range/digit limits and unlimited negative powers raise ArithmeticException.

The focused JVM/Wasm comparisons pass in both JAR orders and independent instances. The reviewed 63-JAR audit baseline changes only the compiler revision, with unchanged dependency inventory.
