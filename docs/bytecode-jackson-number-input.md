# Pinned Jackson numeric parsing

The compatibility fixtures use Jackson Core 2.15.4, SHA-256 `8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94`. The JVM oracle loads the original artifact. Wasm links selected NumberInput and BigDecimalParser carriers: retained fields and method instructions are unchanged, unrelated entry points are removed. NumberInput keeps its original class initializer. This is selected-method execution, not whole-JAR admission or general Jackson parser execution.

Completed paths: `bigIntegerDecimal`, `intString`, `intArray`, `longString`, `longArray`. BytecodeJacksonNumberInputTest compares results and typed failures in both JAR orders and two independent dirty-memory instances. Hashes include numeric value/representation and decimal scale where relevant; exception messages are not claimed identical. Array helpers retain Jackson's trusted-digit/caller-validated-input preconditions; they are not general validating parsers.

Boolean fast-parser overloads, parseAsInt/Long/Double coercion, floating parsers and their transitive fast-parser classes are excluded. The selected default decimal parser retains its long-input recursive branch. Testing that branch and extending malformed input/host-memory coverage remain open until separately recorded. No source facade stands in for Jackson methods or JDK bytecode runtime APIs.
