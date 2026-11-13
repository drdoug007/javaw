# Reviewing agent-kit work

The local agent commits each finished task to the `agent-kit/work` branch, as one commit per task with the
subject `feat(bytecode): ... (agent kit TNN, vNNN)`. Blocked tasks get a `chore(agent-kit): block TNN` commit.
Review before anything reaches `main`.

## Per task

1. **Check the guard rails with `main`'s copy of the kit.** The agent could have changed its own copy of
   `bin/kit` or its task's `allow:` list, so run the copy from `main`, from anywhere in the repository:

   ```
   git show main:javaw-agent-kit/bin/kit > /tmp/kit-main && python3 /tmp/kit-main review
   ```

   This checks every commit on `agent-kit/work` that is not on `main`. Pass commit IDs to check only those,
   or `--trusted-ref REF` to compare against another branch. For each task commit, it reads the task's
   `allow:` list from `main`, not from the branch. It checks the commit against its parent and requires:
   - only allowed files, registration lines, and the revision lines change;
   - the task test changes only by losing its `agent-pending` tag;
   - there are no forbidden constructs and no unregistered runtime classes;
   - the revision is `<slug>-v<parent + 1>`, the same in both files;
   - `QUEUE.md` changes only in the task's own row.

   Block commits may touch only the report and `QUEUE.md`. Retry commits may only set the task back to
   `todo` and delete its blocked report.
2. **Read the runtime code**, not only the test result. Look for:
   - special-casing of test values;
   - members the task said to leave out;
   - large verbatim JDK copies;
   - `jdk.internal` references;
   - quadratic algorithms where the JDK is linear.
3. **Check the revision when merging several tasks.** `review` checks each commit against its own parent,
   so consecutive tasks on the branch are already numbered one step apart. If `main` gained a revision bump
   since the branch was cut, renumber them while rebasing.
4. **Write the documentation the agent does not write.**
   - Add one line to `Todo.md` under the matching section 12 item (e.g. the collection or text families), in
     the style of the LinkedList (v252) entry: revision, behaviour, evidence (`<Test>` in
     `javaw-agent-kit`), and what remains open.
   - Add one line to `Readme.md` where the other bytecode runtime APIs are listed.
   - Review the agent's `docs/bytecode-<slug>.md`.
   - Amend these into the task commit (`git commit --amend`), so each task stays one complete commit. If you
     fix the agent's code, add a `Review:` paragraph to the message. `kit review` accepts `Todo.md` and
     `Readme.md` changes in a task commit; agent-time `kit guard` does not. Re-run `kit review` afterwards.
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

- **The session ran out of context or time.** The task is fine. Run `kit retry TNN` to queue it again, then
  `bin/run-local --task TNN --resume` to continue from the saved attempt. `kit check` keeps an attempt history
  across sessions.
- **The spec was unclear.** Improve the task's Hints on `main`, then `kit retry TNN` and let the agent try
  again. Use `--resume` only if the attempt was heading the right way.
- **The task needed a file it was not allowed to change.** Widen `allow:` if that is safe, or do that part
  yourself.
- **The task is too hard for the model.** Finish it yourself from the patch.

## Merging

Cherry-pick or merge the reviewed commits onto `main`. Each must be a complete, validated task, following
AGENTS.md's "commit each completed task". Keep `agent-kit/work` rebased on `main` before the next agent run,
so the agent starts from the current revision.
