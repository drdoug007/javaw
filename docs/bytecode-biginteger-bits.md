# Linked BigInteger bit operations

The isolated bytecode runtime supports `and`, `or`, `xor`, `andNot`. Operations use infinite two’s-complement sign extension, preserve operands, and return arbitrary-width results. Binary null operands throw NullPointerException; negative bit indices throw ArithmeticException.

BytecodeBigIntegerBitsTest compiles Java 17 fixtures and compares decimal result hashes and typed failures against the JVM, in both JAR orders and two fresh instances with dirty allocation memory. Cases cover zero, signs, byte/word boundaries and values beyond long. Method references exercise linked invocation handles. This is runtime API evidence, not a claim of complete numeric APIs, pinned-library reachability or H2/Spring/Hibernate execution. Extremely large bit positions remain subject to the host memory limit; the decimal-magnitude runtime is not optimized for large bitwise workloads.
