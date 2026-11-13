# javaw local-agent kit

This module lets a local model running offline (for example Qwen3-Coder-30B-A3B in Claude Code through oMLX)
do the simple bulk of the bytecode runtime work. That work is writing JDK classes such as `StringJoiner`,
`ArrayDeque` or `MessageDigest` as plain Java in the bundled exception runtime. A stronger model or a human
reviews and merges the result.

The kit holds:

- **Directions.** [AGENT.md](AGENT.md) is the loop the agent follows. [RULES.md](RULES.md) covers what runtime
  sources may use.
- **Tasks.** [tasks/](tasks/) has one specification per task, ordered by [tasks/QUEUE.md](tasks/QUEUE.md). Each
  lists the exact members, the JDK behaviour to match, the files the agent may change, and hints recorded
  when the task was calibrated.
- **Tests, written before the work.** `src/test/java/one/dastec/javaw/bytecode/` holds them. They are
  differential: javac compiles a Java 17 fixture, it runs on the JVM, then the bytecode compiler runs it in
  Wasm and the values must match. [`KitDifferential`](src/test/java/one/dastec/javaw/bytecode/KitDifferential.java)
  is the shared harness, and `KitHarnessSelfTest` proves it works. A pending task's test is tagged
  `agent-pending` and excluded from normal builds.
- **Guard rails.** [bin/kit](bin/kit) runs the loop: `next`, `start`, `check`, `guard`, `finish`, `block`.
  `guard` rejects changes outside the task's allowed files, edits to tests or the kit, generated files,
  unregistered runtime sources, and forbidden constructs (`synchronized`, `volatile`, `native`, threads, time,
  entropy). It also checks that only registration lines and revision lines change in the shared files.
  `finish` runs the task tests, the full bytecode suite plus the kit tests, bumps the compiler revision, runs
  `runtime-target/audit.py --check-baseline`, removes the `agent-pending` tag (so the test becomes a regression
  test) and commits on the `agent-kit/work` branch.
- **An unattended runner.** [bin/run-local](bin/run-local) gives each task a fresh session with a per-task
  permission allowlist.

Every task was calibrated before it was queued: a reference implementation was written in a scratch worktree
to prove the tests pass with only the allowed files changed, and then thrown away.

## Requirements

- JDK 21 at `~/.sdkman/candidates/java/21.0.2-graalce`, or set `KIT_JAVA_HOME`. The bytecode suite fails on
  JDK 24+ (ASM 9.7 cannot read class file version 68). `bin/kit` sets `JAVA_HOME` itself.
- `mvn`, `wat2wasm` (wabt) and `node` on `PATH`, and a populated `~/.m2`. Everything runs with `-o`.
- oMLX serving the model with a context window of **at least 128K tokens** (Qwen3-Coder-30B supports 256K).
  Claude Code's own prompt and tool definitions take about 25K tokens, and Claude Code will not compact below
  100K. With a 64K window the first attempt at T02 compacted five times and lost track of its own edits.
  `bin/run-local` reads the served length from oMLX and uses `min(131072, served)` unless you pass
  `--context`. It refuses anything under 100K. At 131072 tokens the KV cache is about 12 GB, so the 4-bit
  model (about 17 GB) fits comfortably in 48 GB.

Check with `javaw-agent-kit/bin/kit doctor`.

## Running it

### Interactive (you approve each command)

```
cd /Volumes/SanDisk/javaw
/Applications/oMLX.app/Contents/MacOS/omlx-cli launch claude --model Qwen3-Coder-30B-A3B-Instruct-MLX-4bit
```

Then paste:

```
Read javaw-agent-kit/AGENT.md and do the next kit task. Run javaw-agent-kit/bin/kit next to find it.
```

Approve `javaw-agent-kit/bin/kit ...` and read-only commands, and refuse anything else. Start a new session
for each task: small models do much better with a fresh context.

### Unattended (one fresh session per task)

```
OMLX_API_KEY=<your oMLX key> javaw-agent-kit/bin/run-local --tasks 3
```

Useful options are `--task T05`, `--model NAME`, `--context TOKENS`, `--timeout MINUTES`, `--resume` (continue a
retried task from its saved attempt) and `--dry-run`, which prints the session command and permission settings
without running anything.

To give blocked tasks another go, usually with a stronger model, use `--retry`. It runs `kit retry` on each
task and then a fresh session that continues from the saved attempt (`--fresh` starts over instead):

```
OMLX_API_KEY=<your oMLX key> javaw-agent-kit/bin/run-local --retry --model Qwen3.8-27B-oQ4e-mtp --timeout 240
OMLX_API_KEY=<your oMLX key> javaw-agent-kit/bin/run-local --retry T06 --fresh --model gemma-4-31B-it-oQ4e-mtp
```

A bare `--retry` (or `--retry all`) means every blocked task, in queue order. `kit finish` and `kit block` record
the model in their commits. Dense ~30B models are smarter than the 3B-active Qwen3-Coder but generate a few
times more slowly, so allow a longer `--timeout`. Each session:

- runs in `dontAsk` mode;
- may edit only its task's files and run only `bin/kit` plus read-only shell commands;
- cannot commit, check out, reset or delete files (`kit finish` and `kit block` do that);
- has subagents, web access and LSP disabled.

When a session ends without finishing, the runner blocks the task and saves the attempt in
`javaw-agent-kit/target/kit/<ID>/attempt.patch`. Logs are in `javaw-agent-kit/target/kit/<ID>/session.log`.

`kit finish` runs the full bytecode suite with all cores. If the model is generating at the same time, set
`KIT_FORKS=4`.

## Reviewing and merging

The agent commits only to `agent-kit/work`. Review each task with [REVIEW.md](REVIEW.md), then merge to `main`.

## Adding tasks

Copy [tasks/_TEMPLATE.md](tasks/_TEMPLATE.md), write the test first (extend `KitDifferential`, tag it
`@Tag("agent-pending")`), and add a row to `QUEUE.md`. Then calibrate:

1. Implement a reference solution in a scratch worktree.
2. Confirm that `kit check` passes and `kit guard` is clean with only the allowed files changed.
3. Record the pitfalls you hit in the task's Hints. For pitfalls tied to a fixture, also add a line to its
   ```` ```kit-hints ```` block, which `kit check` prints under that failure.
4. Throw the reference away.

Good tasks are one JDK class or a small family of 50–300 lines, with behaviour the JVM can check
deterministically (no hash-order or identity-hash output) and no need for monitors, threads, time, entropy,
weak references or reflection.
