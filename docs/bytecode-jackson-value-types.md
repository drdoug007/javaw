# Pinned Jackson value types

Fixtures link unmodified class files from Jackson Core 2.15.4, SHA-256 `8dc9210dd285db366f45f518dd1e6a9ccfeb0f1a8e184a899fe96d29edf1fd94`. The JVM oracle loads the original artifact. Javac fixtures target Java 17, and Surefire uses GraalVM CE Java 21.0.2 as the oracle. Each exported case is compared against the JVM in both JAR entry orders and in two fresh dirty-memory instances.

`Version` and `Separators` are linked whole.

`BytecodeJacksonValueTypesTest` covers:
- `versions`: both constructors, `unknownVersion` identity, snapshot and unknown predicates (including the legacy `isUknownVersion`), accessors, `toString`/`toFullString`/`hashCode` with null, empty and snapshot fields, and the full `compareTo`/`equals` matrix across 14 versions (sign-normalized), plus null comparison failures.

`VersionUtil` (resources, `Properties`, regex and reflection) and pretty-printer integration are not linked.
