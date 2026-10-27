# Linked bytecode lowercase and text convenience APIs

Selected Java 17 behavior for the isolated bundled bytecode runtime.
Source facades and full framework/JDK compatibility are separate targets.
Tests compile Java 17 fixtures, compare with an external Java 17 CLDR/ROOT
oracle, and execute both JAR orders with fresh dirty-memory instances.

ROOT word-break tables are generated from OpenJDK 17.0.9; upstream GPLv2
with Classpath exception notices, source and provenance are preserved under
`exception-runtime/legal/word-break/` and listed in the root NOTICE.
The adapted boundary cursor retains the upstream GPLv2/Classpath license;
the extraction tool is project-owned Apache 2.0.
This is an internal lowercase-context helper, not public java.text support.
Dictionary segmentation, mutable defaults, locale providers and concurrency
remain excluded. The isolated default locale is ROOT.

Completed subsets:

- v170: Reproducible Java 17 ROOT word-boundary data and an internal boundary cursor for final-sigma contexts, retaining upstream notices and license.
- v171: ROOT Unicode lowercase with full dotted-I expansion, supplementary mappings, surrogate preservation, unchanged-result identity and null-locale failures; final sigma and Turkic/Lithuanian contexts are staged separately.
- v172: Final-sigma lowercase using Java 17 boundary-query and cased-character semantics; word punctuation, combining marks, supplementary contexts and surrogate cuts are compared with the JVM. Thai dictionary-dependent sigma contexts are explicitly excluded.
- v173: Turkish/Azeri dotted and dotless I, conditional dot removal, combining-class barriers, locale normalization and unchanged identity.
- v174: Lithuanian dot retention before above accents, I/J/ogonek contexts, precomposed grave/acute/tilde expansions and combining-class barriers.
- v175: String.toLowerCase() uses the isolated ROOT default, with direct calls, both overload method references, contextual sigma and Java 17 identity/Unicode behavior.
- v176: Unchanged pinned Spring boolean-conversion and Jackson lowercase-strategy instructions execute in selected carriers against original-class JVM oracles; verify Thai exclusions and malformed lowercase descriptors.
- v177: Generic String.transform callback invocation, method references, identity/null results, captured state and unchanged exception propagation.
- v178: String.stripIndent common incidental indentation, trailing whitespace removal, blank final-line influence, CR/LF/CRLF normalization and final-terminator opt-out.
- v179: Deprecated String.getBytes(begin,end,byte[],offset) truncates UTF-16 units to low bytes, validates before writing, and preserves Java source/null/destination failure precedence.

## Observable behavior and limits

`String.toLowerCase(Locale)` applies Java 17 simple mappings plus ROOT dotted-I
expansion, contextual final sigma, Turkish/Azeri dotted and dotless I and dot
removal, and Lithuanian dot retention and accent expansions. Combining-class
barriers and word-boundary queries follow the pinned Java 17 data. Null locales
throw NullPointerException. The default overload uses the isolated ROOT default.
Unchanged BMP strings retain identity; supplementary or unpaired-surrogate
input follows Java 17's copy behavior even where contents stay unchanged.

Sigma under a Thai locale raises UnsupportedOperationException because the
required dictionary segmentation is excluded. Ordinary Thai lowercase without
sigma can execute. Other locales use ROOT boundary data; no general locale
provider or public BreakIterator compatibility is claimed.

`transform(Function)` invokes the callback once with the original receiver,
returns its result including null, and propagates callback exceptions. A null
callback throws NullPointerException. `stripIndent()` removes common incidental
indentation and trailing Java whitespace, accounts for a blank final line,
normalizes CR/LF/CRLF to LF, and applies Java 17's final-terminator opt-out.

Deprecated `getBytes(int,int,byte[],int)` copies the low eight bits of each
UTF-16 unit. It checks source bounds, destination null, then destination bounds,
and validates before any write. Bounds failures are
StringIndexOutOfBoundsException; this overload performs no charset encoding.
Allocation and out-of-memory stability remain separate runtime milestones.

## Execution evidence

`BytecodeLowercaseTest` compares javac fixtures with an external Java 17
CLDR/ROOT oracle, both JAR orders and two fresh dirty-memory Wasm instances per
bounded batch. ROOT tests cover every singleton code point except sigma, whose
contexts are tested separately. Boundary and contextual tests sample the
Unicode domain and include punctuation, combining marks and surrogate cuts.
Convenience API tests cover identity, null and exception propagation, whitespace
and line endings, truncation, bounds, failure precedence and destination state.
Malformed lowercase descriptors fail admission.

Pinned Spring Core 6.1.5 StringToBooleanConverter runs unchanged instructions and
fields in a carrier with the Converter interface/signature and method annotation
metadata removed. Jackson Databind 2.15.4 PropertyNamingStrategies$LowerCaseStrategy
retains its original fields, translate and class initializer; its superclass
and constructor are replaced by an Object carrier constructor. Original classes
and complete pinned JARs supply the JVM oracles. These tests establish selected
instruction-path interoperability, not complete class or framework support.
Artifact SHA-256 values:

- Spring Core: `476dac28509b3f47d0b5e895a639fa75ac2218f074ce214e44832a836c469f7e`
- Jackson Databind: `0f99ff7719eef3ee85608b3dee6ecf3c698003fe9d6e34eabbfabccdc9b5ac2c`

## Reproduction

Generate/check the pinned data with:

```sh
python3 javaw-bytecode/tools/generate-string-lowercase.py --java-home <jdk-17-home> --check
mvn -o -pl javaw-bytecode -am -Dtest='one.dastec.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false -DforkCount=4 test
python3 runtime-target/audit.py --check-baseline
```

Upstream complete source, hashes and license provenance accompany the runtime
under `legal/word-break/`. The compiler revision is
`1.0-SNAPSHOT-legacy-string-byte-copy-v179`. Combined v170–v179 validation: all 470 tests pass in the required offline bytecode suite (four forks), with zero failures, errors or skips, including all 13 new lowercase/text regressions. The pinned Java 17 data generator check and reviewed 63-JAR audit pass. Inventory remains 26,625 classes and 734 resources; selected linkage totals are 1,005 declared signatures, 30 missing members and 302 missing owners. Only the compiler revision changes in the audit baseline.
