# Instructions for the local agent

You implement small pieces of the Java standard library for the javaw bytecode compiler. Each piece is a
**task** in `javaw-agent-kit/tasks/`. A task's tests are already written, and you are done when they pass.
These instructions override the general workflow in the repository's AGENTS.md and Todo.md: do only the task
you were given.

Run every command from the repository root (`/Volumes/SanDisk/javaw`).

## The loop

1. `javaw-agent-kit/bin/kit next`: shows the next task (for example `T01`). If you were given a task ID, use it.
2. `javaw-agent-kit/bin/kit start T01`: starts it. Then read the task file it prints, **all of it**.
3. Read `javaw-agent-kit/RULES.md` once per session. It lists what runtime code may and may not use.
4. Read the task's test in `javaw-agent-kit/src/test/java/one/dastec/javaw/bytecode/`. It shows exactly what
   is called.
5. Look at the example classes the task file names. Copy their style.
6. Write the runtime class(es). Register each new top-level class with
   `javaw-agent-kit/bin/kit register java/util/Name.java` (the path under `exception-runtime/`). Do not read
   or hand-edit `BytecodeExceptionRuntime.java`; it is long and the command does it for you.
7. `javaw-agent-kit/bin/kit check T01`: runs the task's tests and prints a short summary.
   - `PASS`: go to step 8.
   - `FAIL`: read the message, fix **the runtime source**, and run check again.
   - Every check ends with an **attempt history** that the kit keeps for you. Read it: it tells you what
     already failed, even after your context was compacted. If it says the same failure repeated, change
     your approach instead of making the same edit again.
8. Write `javaw-agent-kit/reports/T01.md` (see below).
9. `javaw-agent-kit/bin/kit finish T01`: checks the guard rails, runs all tests, bumps the compiler
   revision, runs the audit and commits. If it fails, fix the problem and run finish again.
10. Stop. Report the task ID and the commit it printed.

## Hard rules

- **Never edit tests**, the kit (`javaw-agent-kit/src`, `bin`, `tasks`, `*.md` except your report), or any
  file the task does not list. `kit guard` and `kit finish` reject such changes. If a test is wrong, say so
  in your report and run `kit block`.
- Do not run `git commit`, `git checkout`, `git reset`, `git stash` or `rm`. `kit finish` commits; `kit block`
  restores files.
- Do not edit `CacheMetadata.java` or `runtime-target/audit-baseline.json`. `kit finish` updates them.
- Do not add `@Disabled`, skip tests, or special-case test values. The expected values come from the real JVM.
- Do not write `.wat`, `.wasm` or `.class` files into the source tree.
- Use only the kit commands to build and test. Do not invent other Maven commands.

## Reading failures

| Message starts with | Meaning | What to do |
|---|---|---|
| `FIX THE RUNTIME: the bytecode compiler rejected` | Admission found a missing class/member or a forbidden construct. The text names the file, line and member. | Add the member, or replace the construct (RULES.md). |
| `FIX THE RUNTIME: Wasm execution differs` | The value differs from the JVM. The message shows `expected` (JVM) and `actual` (Wasm). | Compare your code with the JDK behaviour the task describes. |
| `... threw an uncaught exception in Wasm` | Your code threw where the JVM did not, often an NPE or index error. | Check null handling and bounds. |
| `Cannot build bytecode exception runtime` | javac could not compile a runtime source. The javac error follows. | Fix the Java syntax or type error. |
| `TEST PROBLEM` | The test itself is wrong. | Do not edit it. Explain in the report, then `kit block`. |
| `GUARD FAILED` | You changed a file you may not change, or used a forbidden construct. | Undo exactly what it lists. |

## When you are stuck

If the attempt history shows the same failure **6 times in a row**, or a fix needs a file the task does not allow,
stop guessing:

```
javaw-agent-kit/bin/kit block T01 "one sentence: what fails and what you tried"
```

That saves your attempt as a patch, restores the files and marks the task blocked, so a reviewer can pick
it up. Then stop, or move on to `kit next` if you were asked to do several tasks.

## The report (`javaw-agent-kit/reports/T01.md`)

Keep it short and factual:

```
# T01 StringJoiner
Implemented: <classes and members>
Left out: <JDK members not implemented, and why>
Notes: <anything surprising, e.g. a JDK behaviour that took several tries>
```
