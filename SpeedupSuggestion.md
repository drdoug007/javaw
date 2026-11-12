Leveraging GraalVM 25 for this kind of pipeline can drastically change how you handle test execution and speed things up. The primary bottlenecks in your current pipeline are the multi-step file round-trips (Java -> WAT -> `wat2wasi` -> `wasm-opt` -> Node.js external process calls), which force frequent context switches and disk I/O.

GraalVM features powerful tools that can streamline or completely eliminate parts of this workflow:

### 1. GraalWasm (Run WebAssembly natively inside Java)

Instead of dropping out of Java to execute code via an external Node.js process, you can use **GraalWasm** (GraalVM's built-in WebAssembly execution engine) directly inside your Java test framework (like JUnit).

* **How it helps:** GraalWasm allows you to load and execute compiled `.wasm` or `.wasi` binaries directly within the JVM using the GraalVM Polyglot API.
* **The Benefit:** No Node.js runtime overhead, no process-spawning latency, and direct memory/object passing between your test code and the Wasm module.

### 2. Eliminating Intermediate File Steps

If your Java app generates WAT text files as an intermediate step before running `wat2wasi`, you can bypass writing text files to disk entirely:

* Have your Java compiler/generator emit **binary WebAssembly byte arrays (`byte[]`)** directly in memory.
* Pass those bytes straight into GraalWasm using `Source.newBuilder("wasm", ByteSequence.create(bytes), "name").build()`.

### 3. What about `wasm-opt`?

GraalWasm interprets and JIT-compiles WebAssembly binaries to native machine code on-the-fly using the Graal compiler. However, if `wasm-opt` is being used to aggressively shrink file sizes or apply heavy binary-level optimizations required for production, you can still execute it conditionally. But for *testing*, running the unoptimized or lightly optimized binary straight through GraalWasm can cut out hours of waiting by keeping everything in-memory.

### Example: Replacing Node.js with GraalWasm in Tests

Instead of invoking `node script.js` via `ProcessBuilder`, your Java tests can look like this:

```java
import org.comp.YourJavaToWasmGenerator;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Source;
import org.graalvm.polyglot.Value;

// 1. Generate WASM bytes directly in Java
byte[] wasmBytes = YourJavaToWasmGenerator.compileToWasmBytes();

// 2. Execute and test instantly inside the JVM using GraalWasm
try (Context context = Context.newBuilder("wasm")
        .option("wasm.Builtins", "wasi_snapshot_preview1")
        .build()) {
    
    Source source = Source.newBuilder("wasm", org.graalvm.polyglot.io.ByteSequence.create(wasmBytes), "my-test-module").build();
    Value module = context.eval(source);
    Value instance = module.newInstance();
    
    // Call exported functions directly from your Java test assertions
    Value result = instance.getMember("exports").getMember("myTestFunction").execute(arg1, arg2);
    assert result.asInt() == expectedValue;
}

```

By shifting the execution environment inside the JVM with GraalWasm and keeping the compilation steps in-memory, you can parallelize your tests more easily with standard Java testing tools (like JUnit 5 concurrent execution) and remove the heavy process-launch penalty of Node.js and external CLI tools.

---

## Capturing test performance statistics offline

Before changing the execution pipeline, take a baseline. Then you can measure any GraalWasm or in-memory change against it. `javaw-bytecode/perf-capture.sh` runs the prescribed bytecode selection offline (`mvn -o`) and collects everything needed for later analysis in one archive.

```
javaw-bytecode/perf-capture.sh [--label NAME] [--java-home DIR] [--forks F] [--test PATTERN] [--repeat N]
                               [--cold] [--core] [--sample-seconds S] [--no-sample] [--allow-new-jdk]
                               [-- EXTRA_MAVEN_ARGS...]
```

**Use a JDK 21**, as CI does (Temurin 21 or GraalVM CE 21), through `--java-home` or `JAVA_HOME`. The tests compile fixtures with the running JDK's `javac`. The pinned ASM 9.7 and Byte Buddy cannot read class files newer than Java 23, so on JDK 24 or 25 about 66 tests fail almost at once, and the timings are not comparable. The script refuses such a JDK unless `--allow-new-jdk` is given. The script puts `$JAVA_HOME/bin` first on `PATH`, because the tests start reference JVMs as plain `java`. This matters with SDKMAN: `sdk use java 21.0.2-graalce` sets `JAVA_HOME`, but a JDK earlier on `PATH` (for example `/Library/Java/.../graalvm-25.../bin`) still wins for a bare `java`. JDK 17 runs, but 10 tests fail: two use `List.getFirst()`, and the JVM-comparison tests see JDK 17's older formatting of floating-point values, `BigDecimal` and charsets.

Suggested captures (run them on an idle machine, plugged in):

```
# 0. Point at a JDK 21 (adjust the path)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# 1. Baseline: full selection, pom default forks (0.6C), warm wat2wasm cache, 2 runs for variance
javaw-bytecode/perf-capture.sh --label baseline --repeat 2

# 2. Cold: empty wat2wasm cache, so every wat2wasm call really runs
javaw-bytecode/perf-capture.sh --label cold --cold

# 3. Fork scaling (optional)
javaw-bytecode/perf-capture.sh --label forks4 --forks 4
javaw-bytecode/perf-capture.sh --label forks1C --forks 1C

# 4. Node process mode instead of the persistent worker host (only once the WasmTestSupport change is in)
javaw-bytecode/perf-capture.sh --label node-process -- -Djavaw.test.node=process
```

Each capture is written to `javaw-bytecode/target/perf-runs/<timestamp>-<label>/run-<n>/` and packed as `javaw-bytecode/target/perf-runs/<timestamp>-<label>.tar.gz`. Bring back the `.tar.gz` files. Each run contains:

| File | Contents |
| --- | --- |
| `environment.txt` | Commit, hash of the uncommitted diff, `git status`, CPU/memory, Java/Maven/Node/wabt versions, pom test settings, wasm-cache size, options |
| `wall.txt` | Wall seconds, Maven exit status, surefire totals |
| `time.txt` | `/usr/bin/time -l` for the whole Maven process tree: user/sys CPU, max RSS, context switches |
| `samples.tsv` | Every 2 s (by default): load average, plus process count, CPU % and RSS per process kind (`java`, `node`, `wat2wasm`, `wasm-opt`, `other`) |
| `test-timings/`, `test-timings.json`, `test-timings.md` | Per-test wall and bytecode-compile time from `BytecodeTestTimings`, with the `test-timings.py` report |
| `surefire-reports/` | Surefire XML and text reports, with per-test times and failures |
| `core/` | The same log, time and surefire reports for `javaw-core` (with `--core`) |

Notes:

- `samples.tsv` shows how CPU is split between the JVM forks (compiler, javac, JVM reference runs) and the `node`/`wat2wasm` child processes. Those two are the costs GraalWasm would replace. The `node` row also counts unrelated Node processes, such as IDE helpers; their baseline shows up before the forks start.
- Before each run, the script clears `target/test-timings` and `target/surefire-reports`, so each report covers only that run. `--cold` also deletes `target/wasm-cache`.
- Failing tests do not stop the capture. The exit status is recorded in `wall.txt`.
- `--test` narrows the selection, for example `--test 'one.dastec.javaw.bytecode.BytecodeJackson*Test'` to profile only the slowest classes.

## First results (2026-11-12, superseded by the JDK 21 results below)

Machine: Apple M4 Pro (10 performance + 4 efficiency cores, 48 GB), Node 24.14, wabt 1.0.42, at `a69441e` plus the uncommitted `WasmTestSupport` work (wat2wasm cache, persistent Node worker host). There is no JDK 21 on this machine. The first four captures ran on Oracle GraalVM 25 and have 66 early failures (see above), so their test-class time is understated. The JDK 17 run has 10 failures, mostly assertion failures after the test had executed, so it is the most representative of these captures.

| Capture | Wall | Cores used | Test-class time | Compile time | Mean compile | CPU: java / node / wat2wasm |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| JDK 25, 8 forks, warm cache (run 1) | 106 s | 11.2 | 785 s | 612 s (78%) | 0.33 s | 82% / 13% / 0.2% |
| JDK 25, 8 forks, warm cache (run 2) | 107 s | 11.5 | 793 s | 637 s (80%) | 0.35 s | 83% / 14% / 0.0% |
| JDK 25, 8 forks, cold cache | 113 s | 11.2 | 843 s | 612 s (73%) | 0.33 s | 82% / 13% / 1.4% |
| JDK 25, 4 forks, warm cache | 136 s | 6.4 | 519 s | 418 s (80%) | 0.23 s | 80% / 16% / 0.0% |
| JDK 17, 8 forks, mostly warm | 115 s | 10.6 | 860 s | 610 s (71%) | 0.33 s | 78% / 12% / 1.1% |

"Cores used" is (user + sys CPU) / wall from `/usr/bin/time`. The CPU split comes from `samples.tsv`. The `java` share includes the compiler, in-process `javac`, the JVM reference runs and Maven.

What the data shows:

1. **The bytecode compiler is the bottleneck.** It takes 71–80% of test-class time (about 610 s over 1,831 compilations). Running Wasm through GraalWasm or keeping it in memory does not touch this cost. The slowest classes (FloatingFormat, Lowercase, TextFormat, Transcendental, Reflection, NumericText) each spend 25–31 s compiling. The slowest single tests are two-compile tests over the bundled runtime or pinned Jackson/ByteBuddy, at 9–11 s each.
2. **Node is about 13% of CPU and wat2wasm is negligible.** With the wasm cache, `wat2wasm` uses 0.0–0.2% of CPU; an empty cache costs about 7 s of wall time (6%). On the 8-fork runs, Node averaged 1.3–1.6 cores out of 11. The heaviest non-compile classes are FloatingFormat (20 s), Lowercase (18 s), Charset (13 s) and TextFormat (12 s) of class time outside compilation. That time is Node, `javac` and the JVM reference runs together. Removing Node and wat2wasm entirely could save **at most about 10–15%** of wall time, and only if their replacement ran in negligible time. GraalWasm would run the same Wasm inside the already saturated JVM forks.
3. **The machine is CPU-saturated.** 8 forks keep about 11 of 14 cores busy. 4 forks give 136 s instead of 106 s, but each compile is 30% faster (0.23 s against 0.33 s), so the forks compete for the cores, and the 4 efficiency cores probably run slower compiles. Adding forks will not help. Only the last ~10 s of a run use fewer than 8 forks, so there is little imbalance at the end to fix by ordering classes.
4. **The GraalWasm proposal needs a compatibility check first.** The generated modules use Wasm exception handling (`try`/`catch` with `tag`, from `BytecodeExceptions`). Before any port, check that the GraalWasm version you would use supports that proposal, and the other features wat2wasm is asked to enable. GraalWasm would also replace V8 as the reference Wasm engine, which changes what the JVM-versus-Wasm tests prove for browser hosts.

Suggested order of work, by expected payoff:

- Cut the per-compilation cost of lowering the bundled runtime, the same lever named in `docs/bytecode-test-performance.md`. Each test class compiles the runtime again for every program. A process-wide cache of the lowered runtime, or of whole-program WAT keyed by its inputs and the compiler revision, would remove much of the 610 s.
- Reduce compilations per test where tests compile the same program twice (most of the slowest tests have 2 compiles).
- Leave Node and wat2wasm as they are until compile time has dropped. They become worth replacing only once compilation is no longer about 75% of the time.


## JDK 21 results (2026-11-12)

GraalVM CE 21.0.2 through SDKMAN, on the same machine and working tree as above. **All 607 tests pass in every capture.**

| Capture | Wall | Cores used | Test-class time | Compile time | Mean compile | CPU: java / node / wat2wasm |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| 8 forks (`0.6C`), warm cache, run 1 | 96 s | 10.5 | 700 s | 555 s (79%) | 0.29 s | 81% / 15% / 0.2% |
| 8 forks, warm cache, run 2 | 97 s | 10.8 | 718 s | 580 s (81%) | 0.31 s | 81% / 15% / 0.0% |
| 8 forks, cold cache | 107 s | 10.5 | 796 s | 579 s (73%) | 0.31 s | 80% / 15% / 1.5% |
| 6 forks, warm cache | 111 s | 8.4 | 618 s | 499 s (81%) | 0.26 s | 77% / 16% / 0.0% |
| 10 forks, warm cache | 109 s | 11.5 | 975 s | 775 s (79%) | 0.41 s | 79% / 14% / 0.0% |

There are 1,894 compilations per run. Per compilation, the median is 0.46 s, the 90th percentile 0.81 s and the maximum 6.1 s.

Findings:

1. **The pom default of 8 forks is the best setting.** 6 forks are 15% slower, because cores sit idle. 10 forks are 13% slower: each compile takes 0.41 s instead of 0.30 s as forks contend for the 10 performance cores, which outweighs the extra parallelism.
2. **An empty wasm cache costs about 10 s (+11%).** The warm cache already removes nearly all `wat2wasm` time.
3. **The bytecode compiler is about 80% of test time, and Node is 15% of CPU.** This confirms the earlier conclusion: GraalWasm or in-memory Wasm could save at most about 15% of wall time. Lowering compile cost is the lever.
4. **Compiles of the same program in both JAR orders take over half of compile time.** A second capture used the compile-output fingerprints (`-- -Djavaw.compileOutputs.dir=.../target/compile-outputs`, still 607 passing tests, 97 s). It recorded 1,894 compilations: 958 produced WAT and 936 ended in an expected diagnostic. Only 702 of the WAT outputs are distinct. 249 tests compile the same program twice and get byte-identical WAT. They spend 308 s (55% of compile time), and 228 of the 384 tests that produce two WAT outputs are like this. The pattern is `for (boolean reverse : new boolean[]{false, true}) execute(compileJar(jar(reverse)), expected)`. It appears 119 times in 65 test classes and checks that the compiler's output does not depend on JAR entry order. The tests that only expect diagnostics are cheap: 73 tests and 598 compilations take 20 s in total.
5. **The generated modules are large.** WAT output has a median of 7.7 million characters, a 90th percentile of 23 million and a maximum of 124 million, about 11 GB of WAT text per run. This is mostly the bundled runtime. Compile time correlates with WAT size (r = 0.49, about 48 ms per million characters).

Options, by expected payoff:

- **Check JAR-order independence once instead of in every test.** Have the compiler put its input classes in a canonical order before linking (if it does not already), and test that ordering directly. Keep the full reverse-order compile in a few dedicated tests. The other tests then compile once. That removes roughly half of the 308 s, about 150 s of CPU, or about 20% of wall time (~96 s → ~77 s). It changes test coverage, so it needs a decision on which tests keep both orders.
- **Shrink the bundled runtime in each module** (prune unreachable runtime code, or lower the runtime once and reuse it). At about 7.7 million characters of WAT per typical module, this reduces compile, hashing, disk writes and Node instantiation together.
- **Leave Node and wat2wasm as they are for now.** They are about 15% of CPU together, and the cache already makes wat2wasm negligible.

## Result: one JAR order per test (v259, 2026-11-12)

The first option has been implemented. The compiler links JAR classes in class-name order, the executable tests compile each fixture once, and `BytecodeJarOrderTest` checks order independence. Two captures (`--label jdk21-single-order-final --repeat 2`): **610 tests pass, 71–72 s instead of 96–97 s (−25%)**, 1,519 compilations instead of 1,894, compile time 361–374 s instead of 555–580 s, and CPU 753–776 s instead of 1,006–1,043 s. The estimate was about 77 s. Details are in [docs/bytecode-test-performance.md](docs/bytecode-test-performance.md#single-jar-order-v259).

Compilation is still about 74% of test-class time, so the next lever is the second option, shrinking or reusing the bundled runtime in each module (`Todo.md`: "Reuse the admitted and normalized bundled runtime across compilations").

## Option 2, first stage: deferred runtime text (2026-11-12)

A JFR profile showed most compile time going to generating, scanning and pruning runtime text, not to lowering. Every compile generated about 24.7 MB of runtime WAT, even for `return 41+1`, whose final module is 1.1 MB. Two output-preserving changes now defer the text that cannot affect what is kept: the `$__type_assignable` table, and the per-character stores of string constants. Generated text falls to 13.9 MB, and a warm trivial compile from 347 to 234 ms. **The prescribed selection takes 61–62 s instead of 71–72 s, and 96–97 s before option 1.** All 1,519 compile-output fingerprints are unchanged. Details are in [docs/bytecode-compile-scaling.md](docs/bytecode-compile-scaling.md#deferred-runtime-text-no-output-change).

Next candidates, from the updated profile:
- pruning re-scans text the reachability analysis has already split into fields (about 9% of a compile);
- admission validates every linked runtime method on every compile (20%);
- runtime field splitting (9%).
