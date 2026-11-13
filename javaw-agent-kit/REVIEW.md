# Reviewing agent-kit work

The local agent commits each finished task to the `agent-kit/work` branch, as one commit per task with the
subject `feat(bytecode): ... (agent kit TNN, vNNN)`. Blocked tasks get a `chore(agent-kit): block TNN` commit.
Review before anything reaches `main`.

## Per task

1. **Re-run the guard from `main`'s copy of the kit.** The agent could have changed its own copy of `bin/kit`.

   ```
   git show main:javaw-agent-kit/bin/kit > /tmp/kit-main && python3 /tmp/kit-main guard TNN
   ```

   Alternatively, check that the commit touches only the files the task allows:
   `git show --stat <commit>`. In particular, nothing under `javaw-agent-kit/src` except the removed
   `@Tag("agent-pending")` line, and nothing under `javaw-agent-kit/bin` or `tasks/` except `QUEUE.md`.
2. **Read the runtime code**, not only the test result. Look for:
   - special-casing of test values;
   - members the task said to leave out;
   - large verbatim JDK copies;
   - `jdk.internal` references;
   - quadratic algorithms where the JDK is linear.
3. **Check the revision.** It must be `1.0-SNAPSHOT-<slug>-vN` with N one more than `main`'s, and it must match
   in both `CacheMetadata.java` and `runtime-target/audit-baseline.json`. If several tasks are merged in one
   go, renumber them so each commit is one step up.
4. **Write the documentation the agent does not write.**
   - Add one line to `Todo.md` under the matching section 12 item (e.g. the collection or text families), in
     the style of the LinkedList (v252) entry: revision, behaviour, evidence (`<Test>` in
     `javaw-agent-kit`), and what remains open.
   - Add one line to `Readme.md` where the other bytecode runtime APIs are listed.
   - Review the agent's `docs/bytecode-<slug>.md`.
5. **Validate** on the merged result:

   ```
   mvn -o -pl javaw-bytecode,javaw-agent-kit -am -Dtest='one.dastec.javaw.bytecode.*Test' -Dsurefire.failIfNoSpecifiedTests=false test
   python3 runtime-target/audit.py --check-baseline
   ```

   Then, if you want the declaration counts for the Todo entry, look at
   `runtime-target/target/audit/core-jdk-linkage-summary.tsv`.

## Blocked tasks

`javaw-agent-kit/reports/TNN.md` gives the reason. `javaw-agent-kit/target/kit/TNN/attempt.patch` and
`session.log` hold the attempt. Typical outcomes:

- **The spec was unclear.** Improve the task's Hints, set its status back to `todo`, and let the agent retry.
- **The task needed a file it was not allowed to change.** Widen `allow:` if that is safe, or do that part
  yourself.
- **The task is too hard for the model.** Finish it yourself from the patch.

## Merging

Cherry-pick or merge the reviewed commits onto `main`. Each must be a complete, validated task, following
AGENTS.md's "commit each completed task". Keep `agent-kit/work` rebased on `main` before the next agent run,
so the agent starts from the current revision.
