# Project namespace

Project Java packages use `one.dastec.javaw`; the Spring example uses
`one.dastec.demo`. Source directories match package declarations. Examples,
ASM fixtures, generated source maps, CLI references and Maven plugin filtering
use the same domain order.

All project Maven artifacts now use group ID `one.dastec`, including the parent,
core, bytecode compiler, Maven plugin and optional logging compatibility module.
Artifact IDs and the `1.0-SNAPSHOT` version are unchanged. Consumers must update
both dependency and plugin coordinates and reinstall the reactor artifacts:

```sh
mvn -o -pl javaw-log4j-compat,javaw-maven-plugin -am -DskipTests clean install
```

The compiler CLI entry point is `one.dastec.javaw.JavacToWasm`; the plugin
implementation is `one.dastec.javaw.plugin.CompileMojo`. Third-party namespaces
are preserved, including JDK runtime classes and the optional logging facade's
API names. Existing local Maven cache entries under previous coordinates may
remain on disk; they are no longer referenced by this project's POMs.

The cleanup does not change compiler/runtime semantics or the audited third-party
dependency inventory. Namespace verification scans tracked file paths and raw
contents, including binary artifacts and source maps. Validation results are
recorded in Readme.md and Todo.md.

Validation: all 470 tests pass in the required offline bytecode suite (four forks), with zero failures, errors or skips. Eight focused logging, Maven plugin and pinned-runtime reference tests pass. The Spring example clean-build compiles Java and Wasm under the corrected package; its existing test class is empty. Corrected reactor artifacts install successfully. The audit baseline check passes unchanged. A raw-content and path scan of every tracked file, including binaries and source maps, finds zero legacy namespace references.
