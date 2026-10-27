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
- v195: Long.getLong nullable lookup with decimal/hexadecimal/octal decoding and malformed/overflow fallback.
- v196: Long.getLong primitive default overload and small-value cache identity.
- v197: Long.getLong boxed default overload preserving fallback identity and null.
- v198: Pinned Spring CGLIB property-dependent class initialization and naming execution.

Pinned execution uses original, unmodified `org.springframework.cglib.core.DefaultNamingPolicy`, `NamingPolicy` and `Predicate` class files from Spring Core 6.1.5 (SHA-256 `476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e`). The JVM oracle and Wasm execute those same classes. Separate JVM processes and fresh Wasm instances cover both initial values of `org.springframework.cglib.test.stressHashCodes`, class-initialization capture after later property mutation, deterministic naming, Java package prefix handling, collision callbacks, equality and hash codes. This proves this selected naming-policy path; CGLIB class generation, proxies, the complete Spring container and security-manager behavior remain outside the claim.
- v199: Malformed property call and handle admission, unsupported platform APIs and runtime redefinition rejection.

Wrapper lookups decode the stored string using Integer.decode/Long.decode, including decimal, hexadecimal and octal prefixes. Missing properties, invalid keys and malformed/overflow values return false, null or the supplied fallback as the JDK specifies. Boxed fallbacks retain identity, and decoded small values use wrapper caches. Property writes and removal return the previous string value. Empty values are retained; null keys/values and empty keys follow Java failure types and precedence.

Malformed ASM fixtures cover wrong descriptors, virtual/interface misuse, malformed static handles, handles nested in dynamic constants/bootstrap arguments, and invalid unused wrapper calls. Diagnostics identify the source, line, method and required static descriptor. Bundled-store redefinition and unsupported System.getProperties are rejected. The store uses a sequential linked list; removal unlinks entries but does not establish bytecode heap reclamation or bounded-memory behavior.
