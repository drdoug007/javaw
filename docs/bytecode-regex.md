# Selected bytecode regular expressions

This staged sequential engine establishes selected linked-bytecode behavior, not full java.util.regex compatibility. Unsupported syntax/features are diagnosed explicitly; no host JavaScript regex semantics are substituted.

Completed subsets:

- v211: Literal Pattern/Matcher compilation, quoting, flags and PatternSyntaxException accessors.
