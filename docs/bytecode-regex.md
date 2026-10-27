# Selected bytecode regular expressions

This staged sequential engine establishes selected linked-bytecode behavior, not full java.util.regex compatibility. Unsupported syntax/features are diagnosed explicitly; no host JavaScript regex semantics are substituted.

Completed subsets:

- v211: Literal Pattern/Matcher compilation, quoting, flags and PatternSyntaxException accessors.
- v212: Regex code-point atoms, escaped literals and quoted spans.
- v213: Positive/negative character classes, ranges and ASCII predefined classes.
- v214: Greedy/reluctant bounded and unbounded repetition with zero-width progress.
- v215: Capturing/noncapturing groups and ordered alternatives with backtracking.

Matching uses an explicit instruction/backtracking stack with saved capture and repetition state. This replaces the initial recursive continuation traversal, which exhausted Wasm call frames on a small nested-capture repetition fixture. Parser/program construction still uses recursive traversal; large-pattern compilation and bounded allocation under sustained workloads are not established.
- v216: Anchors and selected matching flags with explicit unsupported-feature diagnostics.
- v217: Matcher find/lookingAt/reset, capture access and String.matches.

Matcher search starts require reset after a failed `matches()` or `lookingAt()`; `find()` diagnoses that unsupported reuse state explicitly. `find(int)` resets by definition. Ordinary repeated find, successful anchored reuse, reset, capture access, and Java 17 zero-width exhaustion state are differential tested. After `reset(null)`, restore non-null input before further traversal; only failure type and invalidation of the prior match are covered.
- v218: Group-aware replacement, quoteReplacement and String replaceAll/replaceFirst.
