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
javaw-bytecode/perf-capture.sh [--label NAME] [--forks F] [--test PATTERN] [--repeat N] [--cold]
                               [--core] [--sample-seconds S] [--no-sample] [-- EXTRA_MAVEN_ARGS...]
```

Suggested captures (run them on an idle machine, plugged in):

```
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
