# Bytecode system properties and wrapper lookups

Properties are explicit string state isolated to each Wasm instance. The store starts empty; it does not import JVM defaults, operating-system environment, Node globals or browser state. Applications can initialize supported properties using System.setProperty. System.getProperties/setProperties, security-manager permission checks, host seeding, concurrent access, and reflective discovery of System members remain unsupported. This is separate from source-backed facades and complete framework execution.

Exact static System.getProperty (both overloads), setProperty and clearProperty calls and static handles are normalized to the bundled store before admission. A linked System definition suppresses this normalization. Other System APIs retain existing admission/intrinsic behavior; arraycopy and identityHashCode keep their current contracts. Redefinition of the bundled property store is rejected.

BytecodePropertyLookupTest compares Java 17 JVM results with Wasm in both JAR orders and two fresh instances whose allocator fills memory with nonzero bytes. Fixtures use dedicated javaw.test.* keys, not JVM platform defaults. Wasm-only checks prove the empty initial platform store and per-instance property isolation.

Completed subsets:

- v190: Explicit per-instance System string property reads, defaults, writes and removal, exact static calls and handles, key/null failures and fresh-instance isolation.
- v191: Boolean.getBoolean case-insensitive true parsing and false fallback for absent values and invalid keys.
- v192: Integer.getInteger nullable lookup with decimal/hexadecimal/octal decoding and malformed/overflow fallback.
- v193: Integer.getInteger primitive default overload and small-value cache identity.
- v194: Integer.getInteger boxed default overload preserving fallback identity and null.
