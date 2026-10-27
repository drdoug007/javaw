# Selected bytecode regular expressions

This staged sequential engine establishes selected linked-bytecode behavior, not full java.util.regex compatibility. Unsupported syntax/features are diagnosed explicitly; no host JavaScript regex semantics are substituted.

Completed subsets:

- v211: Literal Pattern/Matcher compilation, quoting, flags and PatternSyntaxException accessors.
- v212: Regex code-point atoms, escaped literals and quoted spans.
- v213: Positive/negative character classes, ranges and ASCII predefined classes.
- v214: Greedy/reluctant bounded and unbounded repetition with zero-width progress.
- v215: Capturing/noncapturing groups and ordered alternatives with backtracking.

Matching uses an explicit instruction/backtracking stack with saved capture and repetition state. This replaces the initial recursive continuation traversal, which exhausted Wasm call frames on a small nested-capture repetition fixture. Parser/program construction still uses recursive traversal; large-pattern compilation and bounded allocation under sustained workloads are not established.
