# Bytecode test performance

The prescribed validation run is unchanged:

```
mvn -o -pl javaw-bytecode -am -Dtest='one.dastec.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test
```

Its fork and JVM settings now live in `javaw-bytecode/pom.xml`. Earlier notes ("four forks, JVMCI compilation disabled") came from command-line flags.

## Build settings

| Property | Default | Meaning |
| --- | --- | --- |
| `javaw.test.forks` | `0.6C` | Surefire `forkCount`: 0.6 forks per core, so 8 on a 14-core machine. Forks are reused, and each runs whole test classes sequentially. |
| `javaw.test.argLine` | empty | Extra JVM options for the forks |

Override either with `-D`, for example `-Djavaw.test.forks=4`. A plain `-DforkCount` no longer overrides the pom's setting.

The surefire configuration also sets `javaw.compileTimes.dir`, which turns on timing collection (below).

## Measurements

These are warm offline runs on a 14-core Apple-silicon machine with 48 GB of memory, GraalVM CE 21.0.2, wabt 1.0.41 and Node 24.19, at `d145ea0` plus this change. All 601 tests pass in every full run.

Fork count, full selection:

| Forks | Wall | Summed class time | Mean compile |
| --- | --- | --- | --- |
| 4 (former setting) | 693 s (11:33) | 2,718 s (surefire elapsed; run before the timing extension) | — |
| 8 (`0.6C`) | 506 s | 3,489 s | 1.60 s |
| 12 | 484 s | 4,627 s | 2.13 s |

With 4 forks the machine used about 5.2 of its 14 cores. Past about 8 forks, forks mainly compete for CPU: summed class time and mean compile time rise, and wall time barely falls.

JVM options, on six mid-sized classes (FloatingFormat, Regex, Lowercase, NumericText, Streams, Charset), with one class per fork:

| Options | Wall | Mean compile |
| --- | --- | --- |
| Default (Graal JIT on GraalVM) | 100 s | 2.30 s |
| `-XX:+UnlockExperimentalVMOptions -XX:-UseJVMCICompiler` (C2) | 102 s | 2.39 s |
| C2 with `-XX:+UseParallelGC` | 123 s | 2.92 s |
| C2 with `-XX:TieredStopAtLevel=1` (C1 only) | 168 s | 4.08 s |

Disabling JVMCI no longer helps, and stopping at C1 is much slower, because compilation is long-running, optimizable work. The default `argLine` is therefore empty.

## Where the time goes

`BytecodeTestTimings`, a JUnit extension the test resources register, reads a process-wide compile clock (`BytecodeCompileClock`). The clock wraps `BytecodeCompiler.compileClasses` and `ClassBytecodeCompiler.compileToWat`. The `BytecodeCompiler` entry points also include class parsing and legacy-subroutine normalization. It counts only the outermost compilation on a thread, and failed compilations count too. Normalization in a directly constructed `ClassBytecodeCompiler` is not timed. Outcomes are `passed`, `failed` or `aborted` (failed assumptions). Parameterized or repeated invocations are named by method and invocation. A failure to write a timing file is printed and never fails the class. The extension writes one TSV per test class to `javaw-bytecode/target/test-timings`, with a row per test and one class row. The class row's remainder covers static setup and test-instance construction.

In the 8-fork run, 87% of test-class time is spent inside the bytecode compiler: 3,023 s across 1,894 compilations. The rest is javac, `wat2wasm`, Node and the JVM reference runs. The largest single compilations link the pinned Jackson JAR, at about 16 s each in `BytecodeJacksonBase64Test`. Tests that admit about 40 malformed library fixtures through the bundled runtime take 16–32 s of compile time each. Runtime-free admission tests with 128–173 fixtures take milliseconds. Reducing compiler cost, especially for the bundled runtime that every compilation lowers, therefore remains the main lever for a faster run.

## Reports and CI

```
python3 javaw-bytecode/test-timings.py [--top N] [--markdown FILE] [--json FILE] [--warn-compile-seconds S]
```

The script prints totals, the slowest classes, and the tests with the most compile time. With no timing files it prints a notice and exits 0, so a failed build is not reported twice. `--warn-compile-seconds` emits a GitHub `::warning` for each test that exceeds it. Timing files are replaced per class, like surefire reports, so a partial run leaves earlier classes' files in place. Use `--dir` or a clean `target` for an isolated report.

`.github/workflows/bytecode-tests.yml` runs the prescribed selection on `ubuntu-24.04`, with Temurin 21, Node 24 and Ubuntu's `wabt` package. Before the tests it runs `runtime-target/verify-lock.py --online`, which downloads and hash-checks the pinned JARs that library tests read from the local repository. Temurin 17 supplies the JDK 17 oracle through `-Djavaw.jdk17.home`, whose default path is macOS-only. The run uses `-Djavaw.test.forks=1C`, because hosted runners have few cores and forks idle while `wat2wasm` and Node run. It then writes the report to the job summary, warns on any test above 90 s of compile time, and uploads the timing files, the JSON report and the surefire XML as the `bytecode-test-timings` artifact.

The first run (41360133970) hit the 60-minute job limit after 45 of 75 classes. It showed that Ubuntu's wabt 1.0.34 handles the generated exception handling, and that CI compiles are about 3.1 times slower than local ones. A completed run, the hosted fork count and a CI timing baseline are still open.

## Compile-output fingerprints

`BytecodeCompileOutputs`, a second auto-registered extension, is inactive unless `-Djavaw.compileOutputs.dir=DIR` is passed to Maven. When active, it records the outcome of every outermost compilation, in order, through `BytecodeCompileClock`'s observer hook. Each outcome is a SHA-256 of the WAT text, or of the thrown exception's class and message, plus the text length. It writes one TSV per test class (test, index, kind, hash, length).

To show that a compiler change preserves every output and diagnostic, run the prescribed selection with the property set, once before and once after, into different directories, and compare them with `diff -r`. A run before the change can use a copy of the working tree, so that it does not share `target/`.

Known variation: `BytecodeCompatibilityTest.preservesExplicitlyLinkedSystemDefinitionsInsteadOfReplacingTheirMethods` links classes in `Map.of(...)` order, which varies between JVM runs, so its single WAT hash takes one of two values.

## After the per-compile analysis work

With the dispatch, hierarchy and text-scan changes in [the scaling notes](bytecode-compile-scaling.md#per-compile-analysis-cost-no-output-change), the unchanged prescribed command (8 forks) runs 603 tests in 3:08, against 8:12. Compile time falls from 3,010 s to 549 s, a mean of 0.29 s per compilation. Compilation is now 57% of test-class time. The slowest compilations take about 8 s, for two-compile tests over pinned Jackson, ByteBuddy and BigInteger fixtures. The CI workflow's fork count and a hosted-runner baseline still need a new run.
