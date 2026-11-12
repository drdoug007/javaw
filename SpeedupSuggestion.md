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

**Use a JDK 21**, as CI does (Temurin 21 or GraalVM CE 21), through `--java-home` or `JAVA_HOME`. The tests compile fixtures with the running JDK's `javac`. The pinned ASM 9.7 and Byte Buddy cannot read class files newer than Java 23, so on JDK 24 or 25 about 66 tests fail almost at once, and the timings are not comparable. The script refuses such a JDK unless `--allow-new-jdk` is given. JDK 17 runs, but 10 tests fail: two use `List.getFirst()`, and the JVM-comparison tests see JDK 17's older formatting of floating-point values, `BigDecimal` and charsets.

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

## First results (2026-11-12)

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

Next captures to make on a JDK 21: `--label jdk21-baseline --repeat 2`, `--label jdk21-cold --cold`, and `--forks 6` and `--forks 10` to find the best fork count between the 4 and 8 measured.
