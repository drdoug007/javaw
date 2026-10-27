# Bytecode compile cost and output size

## Measurements (before this work, `aa52ecf`)

The figures are for a trivial fixture (`return 41+1`) compiled with the bundled exception runtime. Every compile lowered and emitted the whole runtime: about 12 s, 153 MB of WAT, 12,036 functions and an 11.8 MB `.wasm`. Lowering methods (`compileMethod`) took only about 2.5% of that compile time. Most of the rest was whole-program analysis repeated for each of the ~300 runtime classes:

- `BytecodeDispatch` construction and array-type collection (~24%)
- object layouts (~12%)
- whole-program validation repeated in every class's `BytecodeCompatibility.validate` (~22%)

Only 1,255 functions (3.7% of the WAT text) are reachable from the fixture's exports.

## Per-compilation analysis reuse

`BytecodeAnalysisCache` memoizes whole-program analyses for one compilation. It is opened in `BytecodeCompiler` after normalization and keyed by the identity of the linked map. It caches:

- object layouts, including the `sizes`/`offsets` maps
- the caller-independent `BytecodeDispatch` core: the effective linked map, class and array IDs, and shared subtype answers
- trace-site IDs
- three whole-program checks in `BytecodeCompatibility.validate` (declarations, overrides, descriptor linkage), each memoized at its original position so the first diagnostic and its order are unchanged

This relies on one invariant: lowering and validation never mutate linked classes after normalization; all mutation happens in the normalizers. Callers outside a compilation (no active cache) compute every analysis as before.

Result: the trivial fixture compiles in 2.2 s instead of 11.8 s, and its WAT is byte-identical to `aa52ecf`. All 592 prescribed bytecode tests pass (four forks). Output is unchanged, so the compiler revision is unchanged.
