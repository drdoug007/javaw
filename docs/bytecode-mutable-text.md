# Bundled mutable text and UTF-16 operations

Revision `mutable-text-v65` extends the isolated bytecode runtime selected by `compileJarWithExceptionRuntime`, `compileClassWithExceptionRuntime`, or CLI `--exception-runtime`. Operations execute in Wasm using ordinary bundled class files with the JDK member descriptors; source facades and host-JDK text operations do not supply linked execution.

## Supported behavior

String adds StringBuilder/StringBuffer snapshot constructors, contentEquals(CharSequence/StringBuffer), literal replace(CharSequence,CharSequence), backward string/code-point searches with optional starting indices, codePointAt/Before/Count and offsetByCodePoints. Search indices and lengths count UTF-16 code units. Code-point traversal joins valid surrogate pairs and treats unpaired surrogates as individual code points. A substring boundary can split a pair, and counting respects those boundaries. Literal replacement uses nonoverlapping matches, interprets neither regular expressions nor replacement escapes, and inserts replacements at every code-unit boundary for an empty target.

StringBuilder adds Comparable<StringBuilder> and its erased bridge, StringBuffer append, character insertion, replacement, appendCodePoint, backward searches and code-point traversal/count/offset operations. Replacement clamps an oversized end index, checks invalid ranges and null input before changing storage, and preserves unaffected prefixes/suffixes across growth or shrinkage. appendCodePoint accepts all values from 0 through U+10FFFF, including individual surrogate code units, and rejects values outside that range. Invalid appends leave the buffer unchanged.

StringBuffer supplies its own binary return descriptors while delegating UTF-16 storage to StringBuilder. It implements CharSequence, Comparable<StringBuffer> and Serializable. Its supported API includes:

- Default/capacity/String/CharSequence constructors; length, capacity, isEmpty, ensureCapacity and trimToSize.
- Character access/mutation, setLength with zero-fill, substring/subSequence, getChars and immutable toString snapshots.
- Append overloads for String, StringBuffer, CharSequence (whole/range), Object, char arrays (whole/slice), char and all numeric/boolean primitives; appendCodePoint.
- String/character insertion, delete/deleteCharAt, replace and surrogate-preserving reverse.
- Forward/backward string searches, code-point traversal/count/offset operations, typed comparison and its Comparable bridge.

Null textual append arguments become `null`; null char arrays and constructor inputs fail. Buffer self-append snapshots its source length before growth. Mutable inputs and returned String snapshots do not share writable storage. Equality and hashing of builders/buffers retain Object identity semantics, while compareTo compares their UTF-16 contents. Typed bounds, null, negative-capacity and invalid-code-point failures are compared with the JVM. Failure-message text is not promised.

## Execution contract and limits

StringBuffer is supported **only for the isolated sequential execution contract**. Its bundled methods do not implement synchronized monitors, cross-thread exclusion or Java Memory Model visibility. Calls must not be used to claim concurrent StringBuffer compatibility; threads, monitor admission and host concurrency remain governed by the separate concurrency milestone. No concurrency implementation is introduced here.

Appendable, the AbstractStringBuilder hierarchy, unlisted insertion overloads, character streams, serialization and complete JDK text coverage remain open. String regex, charset, formatting, locale/case conversion and broader Unicode classification remain separate work. Builder/buffer code-point traversal and searches currently make immutable snapshots, so their allocation/performance can differ from the JDK. Large text and snapshots are bounded by available Wasm memory; this subset does not introduce managed reclamation or promise JDK complexity.

## Actual dependency bytecode evidence

`BytecodeCoreTextTest` compiles fixtures with javac `--release 17`, obtains a JVM oracle, links ordinary dependency class files, assembles WAT, and executes Node/Wasm in both JAR orders and two independent instances with default memory limits. Tests cover mutable operations, growth/shrinkage, null appends, self-append, snapshot independence, Comparable bridges, code points (paired/unpaired surrogates), backward search offset extremes, empty and overlapping literal targets, replacement metacharacters and typed failures.

The **entire unchanged** pinned JAXB Core 4.0.2 `org.glassfish.jaxb.core.WhiteSpaceProcessor` class executes, retaining all constructors and methods. Its replace path invokes the previously missing String(StringBuilder) constructor; tests exercise replace/collapse/trim, XML whitespace classification, CharSequence inputs, unchanged-input identity, mutation independence, null failures, ASCII controls and non-ASCII text. Artifact SHA-256: `d7ff2954ad78480bbab9391cccff3a22f42a82b6e09aeca1c7d502411c470ccd`.

A second fixture verifies the pinned Logback Core 1.4.14 artifact and retains the **original complete escapeTags(StringBuffer) method body** from `ch.qos.logback.core.helpers.Transform`. It executes mutable character access and length, repeated expanding replacements and StringBuffer.toString over all ASCII values, BMP/supplementary text, XML metacharacters, empty input and null failure. Other methods, fields and the regex-dependent initializer are excluded. This is selected-method evidence, not whole-Transform or regex support. Artifact SHA-256: `f8c2f05f42530b1852739507c1792f0080167850ed8f396444c6913d6617a293`. The existing unchanged whole-class Logback JsonEscapeUtil regression also continues to run.

These fixtures establish the documented utility behavior, not JAXB/Logback framework startup or the full core-JDK dependency closure. The whole-JAR signature inventory rises to 827 declared signatures, with 192 missing members and 318 missing owners. All inventoried StringBuilder/StringBuffer references have declared signatures. These counts measure declarations rather than proven runtime reachability or semantic coverage. The 63-JAR dependency inventory changes only its compiler revision.

Validation: all 299 tests pass in the required full bytecode suite, including seven text regressions and five audit regressions. The focused 12-test run and reviewed 63-JAR audit baseline pass; dependency inventory counts are unchanged at revision `mutable-text-v65`.
