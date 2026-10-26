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
