# Selected bytecode regular expressions

This staged sequential engine establishes selected linked-bytecode behavior, not full `java.util.regex` compatibility. No host JavaScript regex semantics are substituted.

Completed subsets:

- v211: Literal Pattern/Matcher compilation, quoting, flags and PatternSyntaxException accessors.
- v212: Regex code-point atoms, escaped literals and quoted spans.
- v213: Positive/negative character classes, ranges and ASCII predefined classes.
- v214: Greedy/reluctant bounded and unbounded repetition with zero-width progress.
- v215: Capturing/noncapturing groups and ordered alternatives with backtracking.
- v216: Anchors and selected matching flags with explicit unsupported-feature diagnostics.
- v217: Matcher find/lookingAt/reset, capture access and String.matches.
- v218: Group-aware replacement, quoteReplacement and String replaceAll/replaceFirst.
- v219: Pattern and String split limits, zero-width boundaries and selected pinned H2 replacement execution.
- v220: Pattern predicates, selected pinned library execution and malformed regex call/handle admission guards.

## Supported contract

`Pattern` provides `compile` (with optional flags), `matches`, `quote`, `pattern`, `toString`, `flags`, `matcher`, both `split` overloads, `asPredicate` and `asMatchPredicate`. It implements the Serializable marker; object serialization is not established. Predicates allocate a fresh matcher for each input. `PatternSyntaxException` exposes description, pattern, index and formatted message.

Grammar includes code-point literals (including supplementary characters and unpaired surrogate inputs), dot, escaped punctuation/control characters, `\Q…\E`, fixed hex/Unicode escapes, selected brace hex code points, positive/negative classes, ranges and ASCII `dDwWsS` predefined classes. Adjacent escaped Unicode high/low surrogates combine into a code point before quantifier binding. Surrogate values in brace hex escapes are explicitly unsupported. Hex digits are ASCII. Capturing/noncapturing groups, ordered alternatives, greedy/reluctant `?`, `*`, `+`, `{n}`, `{n,}`, `{n,m}` and anchors `^`, `$`, `\A`, `\z`, `\Z` are supported in the tested subset.

Supported flags are `UNIX_LINES`, `CASE_INSENSITIVE`, `MULTILINE`, `LITERAL`, `DOTALL`, and `UNICODE_CASE`. Case matching uses the runtime's selected Character mappings. `COMMENTS`, `CANON_EQ` and `UNICODE_CHARACTER_CLASS` fail explicitly; unknown flag bits are illegal arguments.

`Matcher` provides pattern access, matches/lookingAt/find (including explicit start), reset (including new input), numbered captures/start/end/groupCount, quoteReplacement, replaceAll/replaceFirst, and appendReplacement/appendTail with StringBuilder or StringBuffer. Replacement supports numbered group references and escaped characters. Split preserves Java limit and tested zero-width boundary behavior. `String.matches`, `replaceAll`, `replaceFirst` and both `split` overloads delegate to this engine.

Matcher search starts require reset after a failed `matches()` or `lookingAt()`; `find()` diagnoses that unsupported reuse state explicitly because JVM engine optimization affects the retained search position. `find(int)` resets by definition. Ordinary repeated find, successful anchored reuse, reset, capture access, and Java 17 zero-width exhaustion state are differential tested. After `reset(null)`, restore non-null input before further traversal; only failure type and invalidation of the prior match are covered.

Lookaround, named groups/backreferences, inline flags, possessive quantifiers, nested/intersected classes, Unicode properties, word boundaries and other unimplemented syntax fail explicitly. Named replacement references fail explicitly. Unsupported APIs such as region bounds, immutable match snapshots, functional replacement and split streams fail declaration admission, including calls in unused methods. Malformed descriptors, invocation kinds and method handles likewise fail before WAT generation with source/method context. Exact diagnostic wording/index compatibility is tested only for selected malformed forms.

Matching uses an explicit instruction/backtracking stack with saved capture and repetition state. This replaced recursive continuation traversal, which exhausted Wasm call frames on a small nested-capture repetition fixture. Parser/program construction still uses recursion; large-pattern compilation, bounded allocation, concurrent matcher use and sustained backtracking workloads are not established. No linear-time or denial-of-service bound is claimed.

## Pinned library evidence

The H2 2.2.224 helper `org.h2.command.dml.Help.stripAnnotationsFromSyntax` executes unchanged replacement/trim instructions from SHA-256 `b9d8f19358ada82a4f6eb5b174c6cfe320a375b5a9cb5a4fe456d623e6e55497`. The Wasm carrier omits unrelated methods, fields, initialization and hierarchy; the JVM oracle uses the original artifact. This establishes the selected helper, not H2 SQL execution.

Logback classic 1.4.14 `CallerDataConverter.splitRange` and `getDefaultRangeDelimiter` execute unchanged instructions for `input.split(Pattern.quote(".."), 2)`. Its Wasm carrier substitutes Object as superclass and adds a constructor and public forwarding shim; it omits the remaining logging hierarchy/state. The JVM oracle invokes the original private method reflectively against the pinned artifacts. This establishes range splitting, not logging startup or the logging pipeline. Artifact SHA-256 values:

- logback-classic 1.4.14: `8e832f7263ca606ae36dabb2d8b24c2f43d82cf634e81dad9d1640fa6ee3c596`
- logback-core 1.4.14: `f8c2f05f42530b1852739507c1792f0080167850ed8f396444c6913d6617a293`
- slf4j-api 2.0.2: `f920e120b32ffddb2fdf5af96eed3cd5320199693f47a36b0c2afde3913d7071`

`BytecodeRegexTest` compares executable fingerprints with Java 17 in both JAR orders and fresh dirty-memory instances, and uses malformed ASM fixtures for unused-method admission. The broad audited text/numeric, full regex and framework milestones remain open.

Batch validation at `regex-admission-v220`: all 510 bytecode tests pass, including ten regex tests; the separate combined regex/object-identity selection passes all 15 tests. The prescribed offline full selection used four forks with JVMCI compilation disabled. The reviewed audit baseline passes with unchanged 63-JAR / 26,625-class / 734-resource inventory; selected JDK linkage is 1,030 declared / 18 missing-member / 289 missing-owner signatures.
