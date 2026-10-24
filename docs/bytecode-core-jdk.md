# Bytecode core JDK coverage

The core JDK milestone is incremental. Revision `core-text-v46` supplies the first text subset required by the pinned dependency closure. These are binary-compatible bundled bytecode runtime classes, separate from source compiler facades and the compiler host's JDK.

## Strings and builders

String now implements CharSequence, Comparable<String> and Serializable. In addition to the existing equality, hash, concatenation, interning and primitive formatting operations, it supports copy and character-slice constructors, isEmpty, defensive toCharArray, getChars, substring/subSequence, UTF-16 lexicographic compareTo, startsWith/endsWith, contains, string/code-point indexOf with starting offsets, character replacement and legacy trim (characters at or below U+0020). Invalid character and substring indices produce StringIndexOutOfBoundsException. Code-point search recognizes surrogate pairs; indexing and lengths remain UTF-16 code units.

StringBuilder supplies default/capacity/String/CharSequence constructors, length/capacity/isEmpty, ensureCapacity/trimToSize, setLength, charAt/setCharAt, append for String/Object/CharSequence/character slices/all primitives, delete/deleteCharAt, String insertion, reverse, substring/subSequence, getChars, indexOf and immutable toString snapshots. Null String/Object/CharSequence append values become `null`; null character arrays fail. Self-append preserves the source range across capacity growth. Growth checks count overflow before mutation; extending length zero-fills the new range. Reverse preserves valid surrogate pairs. Storage is mutable and single-threaded; builder equality remains identity-based.

This is a selected text API, not the complete JDK. At `mutable-text-v65`, StringBuilder also implements Comparable<StringBuilder>, StringBuffer and additional code-point/search/replacement APIs are supplied; see [the mutable text contract](bytecode-mutable-text.md). Appendable, AbstractStringBuilder, character streams and unlisted overloads remain unsupported. String regex, charset encoding, locale/case conversion, formatting, streams and additional Unicode APIs remain open. Serializable is a marker here; no stream serialization is implied. Collections/iterators/streams/Optional, comparators, functional interfaces and broad numeric/big-number APIs remain pending in the parent milestone. Missing APIs retain the compiler's ordinary source-site linkage diagnostics rather than host-JDK fallback.

## Real dependency evidence

The pinned audit identifies StringBuilder construction/appending in Logback JSON encoders and escaping utilities, Jackson annotations, Hibernate and other linked libraries. The regression executes the **unchanged entire** Logback Core 1.4.14 `ch.qos.logback.core.encoder.JsonEscapeUtil` class, including its static escape-table initializer, constructors and method declarations. The artifact SHA-256 is `f8c2f05f42530b1852739507c1792f0080167850ed8f396444c6913d6617a293`.

BytecodeCoreTextTest compares JVM/Wasm escaping of all ASCII characters, non-ASCII BMP characters, supplementary characters, empty strings and null failure. It also checks mutable builder operations, immutable snapshots, self-append, growth, trimming/zero-fill, surrogate reversal, primitive formatting, defensive string arrays/slices, search, comparison and typed bounds/null failures. Both tests run with normal/reversed JAR order and two independent Wasm instances. They compare against the pinned library's actual behavior, including its escape choices; they do not substitute a new JSON implementation or establish whole-Logback/framework startup.

Validation: all 242 bytecode tests pass; the reviewed 63-JAR audit baseline passes with unchanged inventory counts and revision `core-text-v46`.

## Integral wrappers and radix conversion

Revision `integral-values-v47` adds Integer.parseInt and Long.parseLong for String (decimal or explicit radix) and bounded CharSequence slices. Signed accumulation handles the minimum negative value without overflow. Radices 2–36, leading ASCII plus/minus, leading zeroes and BMP decimal/fullwidth digits follow Java's numeric conversion rules. Whitespace, underscores, prefixes in parse calls, invalid digits/radices, empty/sign-only values and overflow throw NumberFormatException. String null input throws NumberFormatException; null CharSequence slices throw NullPointerException and invalid slice bounds throw IndexOutOfBoundsException. Failure messages are not an exact host-JDK text contract.

Integer/Long now supply string constructors, valueOf(String[,radix]), decode, toString(value,radix), MIN_VALUE/MAX_VALUE/SIZE/BYTES and hexadecimal/octal/binary formatting. Decode accepts an optional sign followed by decimal, leading-zero octal, #hex or 0x/0Xhex syntax; embedded signs and overflow fail. ValueOf retains the existing wrapper cache. Invalid formatting radices fall back to decimal. Hexadecimal/octal/binary functions format unsigned two's-complement bit patterns at the original 32- or 64-bit width.

Byte/Short supply string constructors, parseByte/parseShort, valueOf(String[,radix]), decode and min/max constants, enforcing their narrower signed ranges. Character.digit(char,radix) recognizes BMP decimal digit blocks and ASCII/fullwidth letters; Character.forDigit supplies lowercase radix digits or NUL for invalid arguments. The int-code-point digit overload and broader Unicode classification/case operations remain unsupported. Unsigned parsing/general unsigned division/formatting, remaining wrapper/numeric APIs, floating parsing, big numbers, collections and functional/stream APIs remain open.

BytecodeIntegralValuesTest compares JVM/Wasm signed limits and radix round trips across 2–36, unsigned bit-pattern text, string constructors, cache reuse, prefixed decode, CharSequence slices, malformed/overflow/null/bounds failures, all 65,536 BMP digit inputs and forDigit argument combinations. It also executes the **unchanged entire** pinned Logback Core 1.4.14 `ch.qos.logback.core.pattern.FormatInfo` class, preserving all constructors/methods, width parsing, sign/truncation behavior, equality/hash/text and library failure types. It uses the same SHA-verified artifact as the escape test above. Both fixtures run with both JAR orders and independent Wasm instances. This is a real library parsing primitive, not full Logback or framework startup.

Validation: all 244 bytecode tests pass; the reviewed 63-JAR audit baseline passes with unchanged inventory counts and revision `integral-values-v47`.

## Lists and iterators

Revision `array-lists-v48` begins the collection contract with Iterable, Iterator, ListIterator, Collection, List, AbstractCollection, AbstractList, RandomAccess and ArrayList. The bundled hierarchy supports ordinary linked bytecode invocation through concrete classes and erased collection interfaces. It remains a selected single-threaded API, not the complete JDK collections library.

ArrayList supports default/capacity/Collection constructors, indexed access and mutation, null elements, contains/indexOf/lastIndexOf, indexed and append addAll, remove/clear, containsAll/removeAll/retainAll, list equality/hash/text, growth/trim, and shallow clone preserving the concrete subclass with independent element storage. Collection construction and addAll snapshot source elements, including self-addition. Removed slots are cleared. Growth checks count overflow before allocating storage.

Iterators support forward traversal/removal; list iterators also support backward traversal, indices, set and add. Exhaustion, invalid mutation state, invalid indices and structural changes produce NoSuchElementException, IllegalStateException, IndexOutOfBoundsException and ConcurrentModificationException. Set is nonstructural. Fail-fast checks are single-threaded consistency checks, not concurrency guarantees. Backed subList views, including nested views, propagate mutations to their parent; structural mutations outside the view invalidate it and its iterators. RandomAccess is retained by views over random-access lists.

Object[] toArray returns defensive storage. Typed toArray preserves the supplied runtime component type, reuses a sufficiently large destination, writes a terminating null when needed, and throws ArrayStoreException for incompatible elements. When a new typed array is needed, the compiler prepares the closed reflection array factories at original toArray entry points, including calls through application subclasses. The array-rank limits of the [reflection contract](bytecode-reflection.md) still apply.

Arrays.asList returns a fixed-size view over the original reference array; set writes through and structural changes fail. Collections.emptyList and singletonList supply the small read-only list factories required by actual dependency sites, retaining null singleton elements and list equality/hash behavior. No general Collections algorithms, unmodifiable/synchronized wrappers, List.of/copyOf, functional traversal/removal, sorting, spliterators, streams, sequenced collections, maps, sets or concurrent collections are supplied. Serializable remains a marker; serialization is separate work.

BytecodeArrayListTest compares JVM/Wasm copying, self-addition, indexed/bulk mutations, nested views, list iterators, exhaustion and invalidation, clone subclass identity/independent storage, typed-array allocation/reuse/type failures and fixed-size backing-array aliasing. It executes the **unchanged entire** pinned Byte Buddy 1.14.11 `net.bytebuddy.utility.CompoundList` class and all four public composition overloads, including empty/singleton branches, null elements, copy independence, equality/hash/text and mutation/null failures. The artifact SHA-256 is `62ae28187ed2b062813da6a9d567bfee733c341582699b62dd980230729a0313`. Both fixtures run in normal/reversed JAR order and two independent Wasm instances. This proves this list-composition utility, not Byte Buddy runtime generation or full framework startup.

Validation: all 246 bytecode tests pass; the reviewed 63-JAR audit baseline passes with unchanged inventory counts and revision `array-lists-v48`.


## Functional interfaces, comparators and Optional

Revision `core-functions-maps-v49` bundles the standard `java.util.function` interface families, including generic and primitive consumers, predicates, suppliers, functions, conversions and operators, plus Runnable. Composition preserves callback order, short-circuiting, exceptions and eager null checks on composition arguments. Function/operator identity and predicate/min/max factories are available. These are ordinary bytecode declarations and implementations; the compiler's existing lambda linkage supplies application lambda objects.

Comparator supports natural/reverse ordering, nullable keys, generic and primitive key extraction, and chained/reversed comparison. Factory comparators carry the Serializable marker; object serialization remains a separate unsupported runtime capability. Iterable/Iterator traversal, Collection.removeIf, List.replaceAll/sort and stable comparator-based Arrays object sorting use these contracts. Sorting currently uses insertion sort, without JDK performance guarantees.

Optional, OptionalInt, OptionalLong and OptionalDouble support value construction, presence/access, conditional callbacks, eager/lazy fallbacks, supplied exceptions, equality/hash/text, and generic filter/map/flatMap/or. Null callback behavior depends on whether the operation actually invokes the callback, as on the JVM. OptionalDouble equality distinguishes signed zeros and equates NaNs. **Optional.stream and the stream families remain missing.** Numeric rendering retains the existing floating-point runtime's limits.

BytecodeFunctionalTest compares JVM/Wasm generic and primitive composition, callback short-circuiting/failure identity, comparator sorting, collection traversal/mutation, Optional null/fallback/checked-exception behavior and floating equality. It executes the unchanged pinned Spring Core 6.1.5 `ThrowingFunction` and its entire anonymous helper class, including checked-exception wrapping and replacement. Artifact SHA-256: `476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e`. Tests use both JAR orders and two independent Wasm instances. This utility does not establish Spring startup.

## Maps and sets

The same revision supplies Map/Entry, AbstractMap, HashMap, LinkedHashMap, Set, AbstractSet, HashSet and LinkedHashSet. Maps support null keys/values, stored key hash/equality lookup (including view operations after key mutation), live key/value/entry views, iterator removal, mutable entries, map/entry equality/hash/text, conditional updates, compute/merge, callback traversal and clone isolation. Entry factories provide key/value comparators; SimpleEntry and SimpleImmutableEntry retain their separate JDK type hierarchies. Sets support equality/hash and bulk removal, with insertion-order iteration for LinkedHashSet.

LinkedHashMap preserves insertion or access ordering, including successful conditional updates and getOrDefault, invalidates iterators on access reordering, and honors removeEldestEntry. Bulk insertion and compute/merge bypass overridden public put methods; insertion hashes a key once. Cloning copies entry storage without calling subclass insertion/eviction hooks and preserves the concrete subtype. HashMap callback methods detect structural mutation and raise ConcurrentModificationException; callback side effects remain visible. Fail-fast iteration is a single-threaded contract, without a concurrency guarantee.

HashMap currently scans stored-hash nodes linearly; it does not provide JDK hash-table complexity. Its iteration order is unspecified to callers. These classes supply a selected API subset, with no serialization engine, concurrent collections, sorted/identity/enum/weak maps, immutable factory maps/sets or general collection wrappers yet.

BytecodeMapTest compares collisions, nulls, live views, mutation/failure behavior, access order, eviction and independent clones with the JVM. It executes the unchanged pinned ANTLR 4.13.0 `OrderedHashSet` class, including subtype cloning, indexed replacement and its unsupported removal contract. Artifact SHA-256: `bd7f7b5d07bc0b047f10915b32ca4bb1de9e57d8049098882e4453c88c076a5d`. Both JAR orders and two independent Wasm instances are tested. This establishes the utility's tested paths, not complete ANTLR or framework execution.

## Signature inventory and remaining work

The pinned audit now writes `core-jdk-linkage.tsv` and `core-jdk-linkage-summary.tsv` under `runtime-target/target/audit/`. Each selected member-reference row retains its artifact, instruction/handle kind, descriptor, occurrence count and first site, with one of these declaration statuses:

- `declared-signature`: the bundled runtime or an inherited declaration supplies the exact member name and descriptor, including intrinsic Object declarations.
- `missing-owner`: there is no bundled declaration for the referenced owner.
- `missing-member`: the owner exists, but the exact member descriptor is absent. Constructors are never inherited.

Summary counts deduplicate owner/name/descriptor across artifacts and instruction kinds. The selected scope covers core text/wrapper/numeric types, java.math, the main collection families, functional interfaces and streams. I/O, resources, locale and concurrency remain separate milestones. The report inventories references across whole selected JARs; it does **not** establish method-level runtime reachability, access/invocation-kind validity, successful compiler admission or executable semantics. A declaration is not evidence that all its paths run.

At this revision, 1,337 distinct selected signatures comprise 531 declared signatures, 281 missing members and 525 missing owners. Remaining work includes streams/collectors/spliterators, Math and big-number APIs, collection families/wrappers/factories, broader Arrays operations and remaining text/wrapper signatures. The parent milestone remains open until the required reachable APIs have execution evidence; these counts must not be represented as a compatibility percentage.

Validation: all 252 bytecode tests pass; the final nine focused functional/map/audit tests pass after the last behavioral fixes. The reviewed 63-JAR audit baseline passes with unchanged inventory counts and revision `core-functions-maps-v49`.


## Array utilities and integral extrema

Revision `array-utilities-v50` adds primitive/reference Arrays copying, fill, whole-array equality/hash/text, acyclic nested equality/hash, cycle-safe deep text, sequential sorting and binary search. Primitive overloads cover all eight kinds for copy/fill/equality/hash/text and the seven sortable/searchable kinds for sort/search. Reference copies preserve the actual array type or allocate the explicitly requested type, retain shallow element aliases, pad extended ranges with defaults, and use checked arraycopy for incompatible elements. Object sorting supports natural/comparator ordering and subranges; primitive floating sorting/search/equality/hash follow Java's NaN and signed-zero conventions. Sorting uses stable insertion sort and does not provide JDK sorting complexity.

Objects adds deepEquals, varargs hash, null predicates, toString overloads and requireNonNullElse. Math supplies only int/long min/max; broader numeric utilities remain open. The runtime's existing floating formatting limits continue to apply to array text.

Generic copy calls and method references now enable the existing reflective array-allocation registry. Object and Object-array component routes are registered alongside linked component types. Copying does not initialize the component class, and multidimensional copies share their inner arrays. The existing closed-world reflection component/rank contract remains in effect; this does not add arbitrary runtime array-rank generation.

BytecodeArraysTest compares JVM/Wasm algorithms across all primitive kinds, natural/comparator sorting, binary-search insertion points, null/type/bounds failures, typed/reference covariance, private passive components, shared nested arrays and cyclic deep text. A separate method-reference-only fixture verifies allocation discovery without explicit application reflection calls. Tests execute the **unchanged entire** pinned H2 2.2.224 `org.h2.util.IntArray` class: zero-capacity growth, capacity expansion, element/range removal, array export, equality/hash/text and original error messages. Artifact SHA-256: `b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497`. All fixtures use both JAR orders and two independent Wasm instances. This proves this H2 utility's tested paths, not the SQL engine or JDBC runtime.

The selected whole-JAR signature inventory now records 601 declared signatures, 250 missing members and 486 missing owners: 70 additional exact declarations. Every inventoried Arrays/Objects signature now has a declaration except Arrays.stream(Object[]). These are declaration counts, not method-level reachability or execution coverage. Streams/collectors/spliterators, big-number/broader Math APIs, remaining collection families/wrappers and text/wrapper gaps keep the parent milestone open. Parallel/stream array operations, additional range equality/mismatch/comparison overloads and full JDK Arrays coverage are not claimed.

Validation: all 255 bytecode tests pass; the reviewed 63-JAR audit baseline passes with unchanged inventory counts and revision `array-utilities-v50`.


## Numeric helpers and collection algorithms

The ten subsequent subsets (`integer-bits-v51` through `collection-algorithms-v60`) are specified in [the numeric and utility contract](bytecode-numeric-utilities.md). They add integral bit/unsigned APIs, checked arithmetic/products, division/sign/rounding helpers and collection algorithms. The selected signature inventory now has 646 declarations, 205 missing members and 486 missing owners across the same 1,337 signatures. These counts describe declarations only. Streams/collectors, big numbers, transcendental Math, broader collection wrappers/factories and remaining text/wrapper APIs remain open.

## BigInteger foundation

Revision `big-integer-v61` links the [selected BigInteger APIs](bytecode-big-integer.md) as ordinary bytecode runtime implementations. JVM/Wasm regressions and selected original Jackson initializer evidence do not establish full BigInteger, Jackson parser or framework coverage. MathContext and the other remaining core APIs are still open.

## BigDecimal and rounding foundation

Revision `big-decimal-v62` links [selected BigDecimal and RoundingMode APIs](bytecode-big-decimal.md) as ordinary bytecode runtime classes. JVM/Wasm comparisons cover exact decimal arithmetic, preferred scales, rounding and numeric conversions, and selected original pinned Hibernate decimal coercion methods with their lambda/exception path. MathContext, engineering formatting, streams and full framework execution remain open.

## Sequential reference streams

Revision `reference-streams-v63` adds [lazy sequential reference streams and selected collectors](bytecode-reference-streams.md), including Collection/Arrays/Optional sources, short-circuiting, close/error paths, and execution of four unchanged pinned Micrometer utility classes. Primitive streams, spliterators, parallel execution, broader collector families and the other core-JDK gaps remain open.
